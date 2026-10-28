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
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.manager.DialogManager;
import moe.hendeca.minidialog.render.DialogRenderer;
import moe.hendeca.minidialog.render.PacketEventsDialogRenderer;
import moe.hendeca.minidialog.util.ParamCasts;
import moe.hendeca.minidialog.util.VelocityProtocolVersions;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@NullMarked
public final class VelocityMiniDialog extends AbstractMiniDialog<VelocityDialog> {

    @Contract("_, _ -> new")
    public static VelocityMiniDialog miniDialog(ProxyServer server, Object plugin) {
        return miniDialog(server, plugin, PacketEvents.getAPI());
    }

    @Contract("_, _, _ -> new")
    public static VelocityMiniDialog miniDialog(ProxyServer server, Object plugin, PacketEventsAPI<?> packetEventsAPI) {
        Objects.requireNonNull(server, "server must not be null");
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(packetEventsAPI, "packetEventsAPI must not be null");
        server.getPluginManager().ensurePluginContainer(plugin);

        VelocityProtocolVersions versions = new VelocityProtocolVersions(server.getClass().getClassLoader());
        if (!versions.supports(VelocityProtocolVersions.MINECRAFT_26_1)) {
            throw new IllegalStateException("Server does not support MC 26.1 or greater, please update Velocity");
        }

        return new VelocityMiniDialog(server, plugin, versions, packetEventsAPI);
    }

    //

    private final ProxyServer server;
    private final Object plugin;
    private final VelocityProtocolVersions versions;
    private final PacketEventsAPI<?> packetEventsAPI;

    private VelocityMiniDialog(
            ProxyServer server,
            Object plugin,
            VelocityProtocolVersions versions,
            PacketEventsAPI<?> packetEventsAPI
    ) {
        this.server = server;
        this.plugin = plugin;
        this.versions = versions;
        this.packetEventsAPI = packetEventsAPI;
    }

    //

    @Contract(pure = true)
    public ProxyServer server() {
        return this.server;
    }

    @Contract(pure = true)
    public Object plugin() {
        return this.plugin;
    }

    @Contract(pure = true)
    public PacketEventsAPI<?> packetEventsAPI() {
        return this.packetEventsAPI;
    }

    @Override
    protected VelocityDialog newDialog(DialogElement document, @Nullable DialogManager manager) {
        return new VelocityDialog(this, document.copy(), manager);
    }

    @Override
    public DialogRenderer<Dialog> renderer() {
        return PacketEventsDialogRenderer.INSTANCE;
    }

    @Override
    public VelocityDialogUser user(@ParamCasts(Player.class) Object handle) {
        if (!(handle instanceof Player p)) throw new IllegalArgumentException("handle is not a Player");
        User packetEventsUser = this.packetEventsAPI.getPlayerManager().getUser(p);
        if (packetEventsUser == null) throw new IllegalStateException("Cannot get PacketEvents user for player " + p);
        return new VelocityDialogUser(
                p,
                packetEventsUser,
                this.versions.playerSupports(VelocityProtocolVersions.MINECRAFT_26_1, p)
        );
    }

}
