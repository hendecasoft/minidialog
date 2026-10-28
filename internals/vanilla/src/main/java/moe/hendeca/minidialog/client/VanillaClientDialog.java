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

package moe.hendeca.minidialog.client;

import moe.hendeca.minidialog.TokenizedDialogKeyFactory;
import moe.hendeca.minidialog.VanillaDialog;
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.manager.DialogManager;
import moe.hendeca.minidialog.response.VanillaDialogResponse;
import net.kyori.adventure.key.Key;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.Dialog;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@NullMarked
@ApiStatus.Internal
public abstract class VanillaClientDialog<I extends VanillaClientMiniDialog, U extends VanillaClientDialogUser>
        extends VanillaDialog<I, U>
{

    private static Screen newScreen(
            Access access,
            Holder<Dialog> dialog,
            @Nullable Screen previousScreen
    ) {
        Dialog resolved = dialog.value();
        DialogScreen<?> screen;
        if (resolved instanceof MultiActionDialog multi) {
            screen = new MultiButtonDialogScreen(previousScreen, multi, access);
        } else if (resolved instanceof SimpleDialog simple) {
            screen = new SimpleDialogScreen<>(previousScreen, simple, access);
        } else if (resolved instanceof DialogListDialog list) {
            screen = new DialogListDialogScreen(previousScreen, list, access);
        } else if (resolved instanceof ServerLinksDialog links) {
            screen = new ServerLinksDialogScreen(previousScreen, links, access);
        } else {
            throw new IllegalStateException("Unsupported dialog type " + resolved.getClass().getName());
        }
        return screen;
    }

    //

    private final Lock tickLock;
    private boolean isTicking;

    protected VanillaClientDialog(
            I instance,
            Class<U> userType,
            DialogElement document,
            @Nullable DialogManager manager
    ) {
        super(instance, userType, document, manager);
        this.tickLock = new ReentrantLock();
        this.isTicking = false;
    }

    //

    @Override
    protected void clear(U user) {
        this.instance.client.setScreen(null);
    }

    @Override
    protected void send(U user, Holder<Dialog> dialog) {
        Minecraft client = this.instance.client();
        ActionListener listener = (i, p) -> this.handleAction(user, i, p);
        Access access = new Access(client, user.handle(), listener);
        client.setScreenAndShow(newScreen(access, dialog, null));
        this.startTicking(client, user);
    }

    @Override
    protected void wake() {
        // NO-OP
    }

    protected abstract VanillaDialogResponse<?> newResponse(U user, @Nullable Tag payload);

    @SuppressWarnings({ "PatternValidation", "OptionalUsedAsFieldOrParameterType" })
    private void handleAction(U user, Identifier id, Optional<Tag> payload) {
        TokenizedDialogKeyFactory.ParseResult result = this.keyFactory.parse(Key.key(id.getNamespace(), id.getPath()));
        if (result == null) return;
        String eid = result.id();
        if (eid == null) return;
        VanillaDialogResponse<?> response = this.newResponse(user, payload.orElse(null));
        this.respond(eid, response);
    }

    private void startTicking(Minecraft client, U user) {
        this.tickLock.lock();
        try {
            if (this.isTicking) return;
            this.isTicking = true;
            client.schedule(() -> this.tick(client, user));
        } finally {
            this.tickLock.unlock();
        }
    }

    private void tick(Minecraft client, U user) {
        if (client.screen == null) this.untrack(user);
        if (!this.update()) {
            this.tickLock.lock();
            try {
                this.isTicking = false;
                return;
            } finally {
                this.tickLock.unlock();
            }
        }
        client.schedule(() -> this.tick(client, user));
    }

    //

    @FunctionalInterface
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private interface ActionListener {
        void run(Identifier id, Optional<Tag> payload);
    }

    private static final class Access implements DialogConnectionAccess {

        private final Minecraft client;
        private final LocalPlayer player;
        private final ActionListener listener;

        Access(Minecraft client, LocalPlayer player, ActionListener listener) {
            this.client = client;
            this.player = player;
            this.listener = listener;
        }

        //

        @Override
        public void disconnect(Component message) {
            this.client.disconnectFromWorld(message);
        }

        @Override
        public void runCommand(String command, @Nullable Screen activeScreen) {
            this.player.connection.sendUnattendedCommand(command, activeScreen);
        }

        @Override
        public void openDialog(Holder<Dialog> dialog, @Nullable Screen activeScreen) {
            this.client.setScreenAndShow(newScreen(this, dialog, activeScreen));
        }

        @Override
        public void sendCustomAction(Identifier id, Optional<Tag> payload) {
            this.listener.run(id, payload);
            this.player.connection.send(new ServerboundCustomClickActionPacket(id, payload));
        }

        @Override
        public ServerLinks serverLinks() {
            return ServerLinks.EMPTY;
        }

    }

}
