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

import moe.hendeca.minidialog.ModdedDialog;
import moe.hendeca.minidialog.TokenizedDialogKeyFactory;
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.manager.DialogManager;
import moe.hendeca.minidialog.server.response.ModdedServerDialogResponse;
import net.kyori.adventure.key.Key;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.TickTask;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@NullMarked
public final class ModdedServerDialog
        extends VanillaServerDialog<ModdedServerMiniDialog, ModdedServerDialogUser>
        implements ModdedDialog
{

    private final Lock wakeLock;
    private final VanillaServerPacketListener.Callback packetCallback;
    private boolean isAwake;

    //

    ModdedServerDialog(
            ModdedServerMiniDialog instance,
            DialogElement document,
            @Nullable DialogManager manager
    ) {
        super(instance, ModdedServerDialogUser.class, document, manager);
        this.wakeLock = new ReentrantLock();
        this.packetCallback = this::handlePacket;
        this.isAwake = false;
    }

    //

    @Override
    protected void wake() {
        this.wakeLock.lock();
        try {
            if (this.isAwake) return;
            this.isAwake = true;
            this.packetListener().on(this.packetCallback);
            this.scheduleTick();
        } finally {
            this.wakeLock.unlock();
        }
    }

    private void scheduleTick() {
        this.instance.server.schedule(new TickTask(Integer.MIN_VALUE, this::tick));
    }

    private void tick() {
        if (this.update()) {
            this.scheduleTick();
            return;
        }
        this.wakeLock.lock();
        try {
            this.packetListener().off(this.packetCallback);
            this.isAwake = false;
        } finally {
            this.wakeLock.unlock();
        }
    }

    @SuppressWarnings("PatternValidation")
    private void handlePacket(PacketListener listener, Packet<?> packet) {
        if (!(listener instanceof ServerGamePacketListenerImpl game)) return;
        if (!(packet instanceof ServerboundCustomClickActionPacket click)) return;
        TokenizedDialogKeyFactory.ParseResult result = this.keyFactory.parse(Key.key(
                click.id().getNamespace(),
                click.id().getPath()
        ));
        if (result == null) return;
        String eid = result.id();
        if (eid == null) return;
        this.respond(eid, new ModdedServerDialogResponse(
                click.payload().orElse(null),
                this.instance.user(game.player),
                this.document(),
                this.keyFactory
        ));
    }

}
