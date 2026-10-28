/*
 * Copyright 2026 hendecasoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package moe.hendeca.minidialog.server;

import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

@NullMarked
@ApiStatus.Internal
public final class VanillaServerPacketListener {

    private static final Field F_FIELD_MODIFIERS;
    private static final Field F_PACKET_PROCESSOR_PACKETS_TO_BE_HANDLED;
    private static final Method M_LAP_LISTENER;
    private static final Method M_LAP_PACKET;
    static {
        Field fFieldModifiers;
        Field fPacketProcessorPacketsToBeHandled;
        Method mLapListener;
        Method mLapPacket;
        try {
            fFieldModifiers = Field.class.getDeclaredField("modifiers");
            fFieldModifiers.setAccessible(true);

            fPacketProcessorPacketsToBeHandled = PacketProcessor.class.getDeclaredField("packetsToBeHandled");
            fPacketProcessorPacketsToBeHandled.setAccessible(true);

            Class<?> cLap = Class.forName("net.minecraft.network.PacketProcessor$ListenerAndPacket");
            mLapListener = cLap.getDeclaredMethod("listener");
            mLapListener.setAccessible(true);
            mLapPacket = cLap.getDeclaredMethod("packet");
            mLapPacket.setAccessible(true);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create reflection handles", e);
        }
        F_FIELD_MODIFIERS = fFieldModifiers;
        F_PACKET_PROCESSOR_PACKETS_TO_BE_HANDLED = fPacketProcessorPacketsToBeHandled;
        M_LAP_LISTENER = mLapListener;
        M_LAP_PACKET = mLapPacket;
    }

    @SuppressWarnings("unchecked")
    @Contract(mutates = "param2")
    private static synchronized ProxiedQueue patchQueue(Controller controller, PacketProcessor pp) {
        Queue<Object> queue;
        try {
            queue = (Queue<Object>) F_PACKET_PROCESSOR_PACKETS_TO_BE_HANDLED.get(pp);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to get value of packetsToBeHandled", e);
        }

        ProxiedQueue proxied;
        if (queue instanceof ProxiedQueue) {
            proxied = (ProxiedQueue) queue;
            if (proxied.controller.equals(controller)) return proxied;
            proxied = new ProxiedQueue(controller, proxied.backing);
        } else {
            proxied = new ProxiedQueue(controller, queue);
        }

        final int mod = F_PACKET_PROCESSOR_PACKETS_TO_BE_HANDLED.getModifiers();
        boolean wasFinal = (mod & Modifier.FINAL) != 0;
        if (wasFinal) {
            try {
                F_FIELD_MODIFIERS.set(
                        F_PACKET_PROCESSOR_PACKETS_TO_BE_HANDLED,
                        mod & ~Modifier.FINAL
                );
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to make packetsToBeHandled non-final", e);
            }
        }
        try {
            F_PACKET_PROCESSOR_PACKETS_TO_BE_HANDLED.set(pp, proxied);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to set packetsToBeHandled", e);
        } finally {
            if (wasFinal) {
                try {
                    F_FIELD_MODIFIERS.set(
                            F_PACKET_PROCESSOR_PACKETS_TO_BE_HANDLED,
                            mod
                    );
                } catch (IllegalAccessException ignored) { }
            }
        }

        return proxied;
    }

    private static Object access(Object target, Method method) {
        try {
            return method.invoke(target);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause == null) cause = e;
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error re) throw re;
            throw new IllegalStateException("Accessor raised a checked exception", cause);
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected reflection error", e);
        }
    }

    //

    private final Controller controller;

    public VanillaServerPacketListener(MinecraftServer server) {
        this(Controller.INSTANCE, server);
    }

    private VanillaServerPacketListener(Controller controller, MinecraftServer server) {
        patchQueue(controller, server.packetProcessor());
        this.controller = controller;
    }

    //

    public boolean on(Callback cb) {
        return this.controller.on(cb);
    }

    public boolean off(Callback cb) {
        return this.controller.off(cb);
    }

    //

    @FunctionalInterface
    public interface Callback extends BiConsumer<PacketListener, Packet<?>> {

        @Override
        void accept(PacketListener packetListener, Packet<?> packet);

    }

    private static final class Controller {

        static final Controller INSTANCE = new Controller();

        //

        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private final Set<Callback> callbacks = Collections.newSetFromMap(new IdentityHashMap<>());

        private Controller() { }

        //

        public boolean on(Callback cb) {
            Lock lock = this.lock.writeLock();
            lock.lock();
            try {
                return this.callbacks.add(cb);
            } finally {
                lock.unlock();
            }
        }

        public boolean off(Callback cb) {
            Lock lock = this.lock.writeLock();
            lock.lock();
            try {
                return this.callbacks.remove(cb);
            } finally {
                lock.unlock();
            }
        }

        public void submit(Object listenerAndPacket) {
            Lock lock = this.lock.readLock();
            lock.lock();
            try {
                if (this.callbacks.isEmpty()) return;
                PacketListener listener = (PacketListener) access(listenerAndPacket, M_LAP_LISTENER);
                Packet<?> packet = (Packet<?>) access(listenerAndPacket, M_LAP_PACKET);
                for (Callback cb : this.callbacks) cb.accept(listener, packet);
            } finally {
                lock.unlock();
            }
        }

    }

    private static final class ProxiedQueue extends AbstractQueue<Object> {

        private final Controller controller;
        private final Queue<Object> backing;

        ProxiedQueue(Controller controller, Queue<Object> backing) {
            this.controller = controller;
            this.backing = backing;
        }

        //

        @Override
        public int size() {
            return this.backing.size();
        }

        @Override
        public Iterator<Object> iterator() {
            return new Iter(this.controller, this.backing.iterator());
        }

        @Override
        public boolean offer(Object o) {
            return this.backing.offer(o);
        }

        @Override
        public @Nullable Object poll() {
            Object removed = this.backing.poll();
            if (removed != null) this.controller.submit(removed);
            return removed;
        }

        @Override
        public @Nullable Object peek() {
            return this.backing.peek();
        }

        //

        private static final class Iter implements Iterator<Object> {

            private final Controller controller;
            private final Iterator<Object> iterator;
            private boolean removable;
            private @UnknownNullability Object removableValue;

            Iter(Controller controller, Iterator<Object> iterator) {
                this.controller = controller;
                this.iterator = iterator;
                this.removable = false;
            }

            //

            @Override
            public boolean hasNext() {
                return this.iterator.hasNext();
            }

            @Override
            public synchronized Object next() {
                Object next = this.iterator.next();
                this.removable = true;
                this.removableValue = next;
                return next;
            }

            @Override
            public synchronized void remove() {
                if (!this.removable) throw new IllegalStateException();
                Object value = this.removableValue;
                this.removable = false;
                this.removableValue = null;
                this.controller.submit(value);
            }

        }

    }

}
