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

package moe.hendeca.minidialog;

import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.element.ElementObserver;
import moe.hendeca.minidialog.except.DialogException;
import moe.hendeca.minidialog.manager.DialogBindableType;
import moe.hendeca.minidialog.manager.DialogBindings;
import moe.hendeca.minidialog.manager.DialogManager;
import moe.hendeca.minidialog.response.DialogResponse;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import javax.script.Bindings;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@NullMarked
public abstract class AbstractDialog<I extends MiniDialog, U extends DialogUser, R>
        implements Dialog
{

    protected final I instance;
    protected final Class<U> userType;
    protected final TokenizedDialogKeyFactory keyFactory;
    private final DialogElement document;
    private final ElementObserver observer;
    private final @Nullable DialogBindings bindings;
    private final ReentrantReadWriteLock stateLock;
    private final Map<UUID, UserEntry<U>> users;
    private boolean configuredToClose;
    private long tickCounter;
    private boolean initialized;

    protected AbstractDialog(
            I instance,
            Class<U> userType,
            DialogElement document,
            @Nullable DialogManager manager
    ) {
        DialogBindings bindings;
        if (manager == null) {
            bindings = null;
        } else {
            bindings = new DialogBindings(manager);
            // audience is intentionally not bound here
            bindings.injectContext(DialogBindableType.MANAGER, manager);
            bindings.injectContext(DialogBindableType.DOCUMENT, document);
            bindings.injectContext(DialogBindableType.INSTANCE, instance);
            bindings.injectContext(DialogBindableType.DIALOG, this);
        }
        this.instance = instance;
        this.userType = userType;
        this.keyFactory = new TokenizedDialogKeyFactory();
        this.document = document;
        this.observer = document.newObserver();
        this.bindings = bindings;
        this.stateLock = new ReentrantReadWriteLock();
        this.users = new HashMap<>();
        this.configuredToClose = document.afterAction() == DialogElement.AfterAction.CLOSE;
        this.tickCounter = 0L;
        this.initialized = false;
    }

    //

    /**
     * Start managing this dialog if not already managed.
     * Management should continue until {@link #update()} returns false
     * and should include processing platform events to
     * {@link #respond(String, DialogResponse)} and {@link #untrack(DialogUser)}.
     */
    protected abstract void wake();

    /**
     * Execute the task sync
     * to the user's scheduler.
     * May execute the task immediately
     * if the user's scheduler is also
     * the global scheduler
     */
    protected abstract void userSync(U user, Runnable task);

    /**
     * Send a rendered dialog
     * to a user
     */
    protected abstract void send(U user, R dialog);

    /**
     * Clear the dialog
     * for a user
     */
    protected abstract void clear(U user);

    /**
     * Renders the dialog as it
     * currently exists
     */
    protected abstract R render(DialogElement dialog);

    protected boolean canShow(U user) {
        return true;
    }

    @Override
    public I instance() {
        return this.instance;
    }

    @Override
    public DialogElement document() {
        return this.document;
    }

    @Override
    public @Nullable DialogManager manager() {
        DialogBindings bindings = this.bindings;
        return bindings == null ? null : bindings.target();
    }

    @Override
    public @Unmodifiable Set<U> users() {
        final Lock lock = this.stateLock.readLock();
        lock.lock();
        try {
            Set<U> ret = HashSet.newHashSet(this.users.size());
            for (UserEntry<U> entry : this.users.values()) ret.add(entry.user);
            return Collections.unmodifiableSet(ret);
        } finally {
            lock.unlock();
        }
    }

    private U qualify(DialogUser user) {
        if (this.userType.isInstance(user)) return this.userType.cast(user);
        throw new IllegalArgumentException("User " + user +
                " is not of type " + this.userType.getName());
    }

    @Override
    public void show(DialogUser user) throws DialogException {
        final U qual = this.qualify(user);
        if (!this.canShow(qual)) return;
        try {
            final DialogBindings bindings = this.bindings;
            final Lock lock = this.stateLock.writeLock();
            R rendered;
            lock.lock();
            try {
                this.users.put(user.id(), new UserEntry<>(qual));
                if (bindings != null) {
                    bindings.injectContext(
                            DialogBindableType.AUDIENCE,
                            this.resolveAudience()
                    );
                    bindings.injectElements(this.document);
                }

                if (!this.initialized) {
                    if (bindings != null) bindings.runInit();
                    this.observer.query();
                    this.initialized = true;
                }

                rendered = this.render(this.document);
                if (bindings != null) bindings.runOpen(user);
            } finally {
                lock.unlock();
            }
            this.send(qual, rendered);
        } finally {
            this.wake();
        }
    }

    @Override
    public void hide(DialogUser user) {
        U qual = this.qualify(user);
        if (!this.isTracked(qual)) return;
        this.clear(qual);
        final Lock wr = this.stateLock.writeLock();
        wr.lock();
        try {
            if (this.users.remove(qual.id()) == null) return;
            DialogBindings bindings = this.bindings;
            if (bindings == null) return;
            bindings.injectContext(
                    DialogBindableType.AUDIENCE,
                    this.resolveAudience()
            );
        } finally {
            wr.unlock();
        }
    }

    /** MUST BE READ LOCKED */
    private Audience resolveAudience() {
        Iterator<UserEntry<U>> iter = this.users.values().iterator();
        if (!iter.hasNext()) return Audience.empty();

        Audience first = iter.next().user.audience();
        if (!iter.hasNext()) return first;

        List<Audience> list = new LinkedList<>();
        list.add(first);
        while (iter.hasNext()) {
            list.add(iter.next().user.audience());
        }
        return Audience.audience(list);
    }

    protected final boolean update() throws DialogException {
        final Lock rd = this.stateLock.readLock();
        final Lock wr = this.stateLock.writeLock();
        final long tick = this.nextTick();

        boolean configuredToClose;
        boolean relock;
        R rendered;
        List<UserEntry<U>> movedUsers;
        rd.lock();
        try {
            Collection<UserEntry<U>> users = this.users.values();
            if (users.isEmpty()) return false;

            DialogBindings bindings = this.bindings;
            if (!this.observer.query()) {
                if (bindings != null && bindings.isThinkTick(tick)) {
                    bindings.runThink();
                }
                return true;
            }

            rendered = this.render(this.document);
            configuredToClose = this.document.afterAction() == DialogElement.AfterAction.CLOSE;
            relock = configuredToClose != this.configuredToClose || bindings != null;
            movedUsers = new ArrayList<>(users);
        } finally {
            rd.unlock();
        }

        if (relock) {
            wr.lock();
            try {
                this.configuredToClose = configuredToClose;
                DialogBindings bindings = this.bindings;
                if (bindings != null) {
                    bindings.injectElements(this.document);
                    if (bindings.isThinkTick(tick)) bindings.runThink();
                }
            } finally {
                wr.unlock();
            }
        }

        for (UserEntry<U> entry : movedUsers) {
            U user = entry.user;
            this.userSync(
                    user,
                    () -> this.send(user, rendered)
            );
        }
        return true;
    }

    private long nextTick() {
        final Lock lock = this.stateLock.writeLock();
        lock.lock();
        try {
            return this.tickCounter++;
        } finally {
            lock.unlock();
        }
    }

    protected final boolean isConfiguredToClose() {
        final Lock lock = this.stateLock.readLock();
        lock.lock();
        try {
            return this.configuredToClose;
        } finally {
            lock.unlock();
        }
    }

    protected final void respond(String id, DialogResponse response) {
        final DialogBindings bindings = this.bindings;
        if (bindings == null) return;
        final Lock lock = this.stateLock.readLock();
        lock.lock();
        try {
            bindings.runCustomAction(id, response);
        } finally {
            lock.unlock();
        }
    }

    protected final boolean isTracked(U user) {
        final Lock lock = this.stateLock.readLock();
        lock.lock();
        try {
            UserEntry<U> existing = this.users.get(user.id());
            return existing != null && existing.user.equals(user);
        } finally {
            lock.unlock();
        }
    }

    protected final void untrack(U user) {
        this.untrack(user, false);
    }

    protected final void untrack(U user, boolean onlyTenured) {
        final Lock lock = this.stateLock.writeLock();
        lock.lock();
        try {
            UserEntry<U> existing = this.users.get(user.id());
            if (existing == null) return;
            if (onlyTenured && !existing.isTenured()) return;
            if (!user.equals(existing.user)) return;
            this.users.remove(user.id());
            DialogBindings bindings = this.bindings;
            if (bindings == null) return;
            bindings.injectContext(DialogBindableType.AUDIENCE, this.resolveAudience());
        } finally {
            lock.unlock();
        }
    }

    //

    private static final class UserEntry<U extends DialogUser> {

        static final long TENURE_TIME = 5000L;

        //

        private final U user;
        private final long since;

        UserEntry(U user) {
            this.user = user;
            this.since = System.currentTimeMillis();
        }

        //

        boolean isTenured() {
            return (System.currentTimeMillis() - this.since) >= TENURE_TIME;
        }

    }

}
