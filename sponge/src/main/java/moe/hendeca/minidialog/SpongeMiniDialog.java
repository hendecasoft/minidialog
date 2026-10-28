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

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.protocol.dialog.Dialog;
import com.github.retrooper.packetevents.protocol.player.User;
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.manager.DialogManager;
import moe.hendeca.minidialog.render.DialogRenderer;
import moe.hendeca.minidialog.render.PacketEventsDialogRenderer;
import moe.hendeca.minidialog.util.ParamCasts;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.plugin.PluginContainer;

import java.util.Objects;

@NullMarked
public final class SpongeMiniDialog extends AbstractMiniDialog<SpongeDialog> {

    @Contract("_, _ -> new")
    public static SpongeMiniDialog miniDialog(Server server, PluginContainer plugin) {
        return miniDialog(server, plugin, PacketEvents.getAPI());
    }

    @Contract("_, _, _ -> new")
    public static SpongeMiniDialog miniDialog(Server server, PluginContainer plugin, PacketEventsAPI<?> packetEventsAPI) {
        Objects.requireNonNull(server, "server must not be null");
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(packetEventsAPI, "packetEventsAPI must not be null");
        return new SpongeMiniDialog(server, plugin, packetEventsAPI);
    }

    //

    private final Server server;
    private final PluginContainer plugin;
    private final PacketEventsAPI<?> packetEventsAPI;

    private SpongeMiniDialog(
            Server server,
            PluginContainer plugin,
            PacketEventsAPI<?> packetEventsAPI
    ) {
        this.server = server;
        this.plugin = plugin;
        this.packetEventsAPI = packetEventsAPI;
    }

    //

    @Contract(pure = true)
    public Server server() {
        return this.server;
    }

    @Contract(pure = true)
    public PluginContainer plugin() {
        return this.plugin;
    }

    @Contract(pure = true)
    public PacketEventsAPI<?> packetEventsAPI() {
        return this.packetEventsAPI;
    }

    @Override
    protected SpongeDialog newDialog(DialogElement document, @Nullable DialogManager manager) {
        return new SpongeDialog(this, document, manager);
    }

    @Override
    public DialogRenderer<Dialog> renderer() {
        return PacketEventsDialogRenderer.INSTANCE;
    }

    @Override
    public SpongeDialogUser user(@ParamCasts(ServerPlayer.class) Object handle) {
        if (!(handle instanceof ServerPlayer p)) throw new IllegalArgumentException("handle is not a ServerPlayer");
        User packetEventsUser = this.packetEventsAPI.getPlayerManager().getUser(p);
        if (packetEventsUser == null) throw new IllegalStateException("Failed to get PacketEvents user for " + p);
        return new SpongeDialogUser(p, packetEventsUser);
    }

}
