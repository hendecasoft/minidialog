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

import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.netty.buffer.ByteBufOperator;
import com.github.retrooper.packetevents.protocol.dialog.Dialog;
import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCustomClickAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerClearDialog;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerShowDialog;
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.manager.DialogManager;
import moe.hendeca.minidialog.render.DialogRenderContext;
import moe.hendeca.minidialog.render.PacketEventsDialogRenderer;
import moe.hendeca.minidialog.response.DialogResponse;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.PrintStream;


@NullMarked
@ApiStatus.Internal
public abstract class PacketEventsDialog<I extends MiniDialog, U extends PacketEventsDialogUser<?>>
        extends AbstractDialog<I, U, Dialog>
{

    private static <K extends PacketEventsDialogUser<?>> void respondAnonymous(
            PacketEventsDialog<?, K> dialog,
            User user,
            String id,
            @Nullable NBT payload
    ) {
        K qual = dialog.user(user);
        if (qual == null) return;
        DialogResponse response = dialog.response(qual, payload);
        dialog.respond(id, response);
    }

    private static <K extends PacketEventsDialogUser<?>> void untrackAnonymous(
            PacketEventsDialog<?, K> dialog,
            User user,
            boolean onlyTenured
    ) {
        K qual = dialog.user(user);
        if (qual == null) return;
        if (!dialog.isTracked(qual)) return;
        dialog.untrack(qual, onlyTenured);
    }

    //

    protected PacketEventsDialog(
            I instance,
            Class<U> userType,
            DialogElement document,
            @Nullable DialogManager manager
    ) {
        super(instance, userType, document, manager);
    }

    //

    protected abstract @Nullable U user(User user);

    protected abstract DialogResponse response(U user, @Nullable NBT payload);

    @Override
    protected void userSync(U user, Runnable task) {
        task.run();
    }

    @Override
    protected void send(U user, Dialog dialog) {
        user.packetEventsUser().sendPacket(new WrapperPlayServerShowDialog(dialog));
    }

    @Override
    protected void clear(U user) {
        user.packetEventsUser().sendPacket(new WrapperPlayServerClearDialog());
    }

    @Override
    protected Dialog render(DialogElement dialog) {
        DialogRenderContext ctx = new DialogRenderContext(this.keyFactory);
        return PacketEventsDialogRenderer.INSTANCE.render(dialog, ctx);
    }

    protected final EventPipe registerEventPipe(PacketEventsAPI<?> api) {
        EventPipe ret = new EventPipe(api, this);
        ret.registration = api.getEventManager().registerListener(ret, PacketListenerPriority.LOWEST);
        return ret;
    }

    protected final void unregisterEventPipe(EventPipe pipe) {
        pipe.api.getEventManager().unregisterListener(pipe.registration);
    }

    //

    protected static final class EventPipe implements PacketListener {

        private final PacketEventsAPI<?> api;
        private final PacketEventsDialog<?, ?> parent;
        private @UnknownNullability PacketListenerCommon registration;

        private EventPipe(
                PacketEventsAPI<?> api,
                PacketEventsDialog<?, ?> parent
        ) {
            this.api = api;
            this.parent = parent;
        }

        //

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            if (event.isCancelled()) return;
            if (!(event.getPacketType() instanceof PacketType.Play.Client t)) return;
            switch (t) {
                case CUSTOM_CLICK_ACTION -> {
                    WrapperPlayClientCustomClickAction wrapper = new WrapperPlayClientCustomClickAction(event);
                    Key key = wrapper.getId().key();
                    TokenizedDialogKeyFactory.ParseResult result = this.parent.keyFactory.parse(key);
                    if (result == null) return;
                    String id = result.id();
                    if (id == null) return;
                    respondAnonymous(this.parent, event.getUser(), id, wrapper.getPayload());
                    if (this.parent.isConfiguredToClose()) untrackAnonymous(this.parent, event.getUser(), false);
                }

                // other packets that indicate
                // a dialog cannot possibly be open
                case PUNCH,
                     USE_ITEM,
                     ATTACK,
                     CHAT_MESSAGE,
                     CHAT_COMMAND,
                     CHAT_COMMAND_UNSIGNED,
                     PLAYER_BLOCK_PLACEMENT,
                     PLAYER_POSITION_AND_ROTATION,
                     PLAYER_ROTATION,
                     INTERACT_ENTITY -> untrackAnonymous(this.parent, event.getUser(), true);
            }
        }

        @Override
        public void onUserDisconnect(UserDisconnectEvent event) {
            untrackAnonymous(this.parent, event.getUser(), false);
        }

    }

}
