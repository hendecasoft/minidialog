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

import moe.hendeca.minidialog.Dialog;
import moe.hendeca.minidialog.DialogUser;
import moe.hendeca.minidialog.VanillaMiniDialog;
import moe.hendeca.minidialog.render.DialogRenderer;
import moe.hendeca.minidialog.render.VanillaDialogRenderer;
import moe.hendeca.minidialog.util.ParamCasts;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@ApiStatus.Internal
public abstract class VanillaClientMiniDialog<D extends VanillaClientDialog<?, ?>, U extends VanillaClientDialogUser>
        extends VanillaMiniDialog<D>
{

    protected final Minecraft client;
    protected final MinecraftClientAudiences audiences;

    VanillaClientMiniDialog(Minecraft client) {
        this.client = client;
        this.audiences = MinecraftClientAudiences.of();
    }

    //

    @Contract(pure = true)
    public Minecraft client() {
        return this.client;
    }

    @Override
    @Contract(pure = true)
    public MinecraftClientAudiences audiences() {
        return this.audiences;
    }

    public U user() {
        LocalPlayer player = this.client.player;
        if (player == null) throw new IllegalStateException("local player is not initialized");
        return this.user(player);
    }

    @Override
    public U user(@ParamCasts(LocalPlayer.class) Object handle) {
        if (!(handle instanceof LocalPlayer p)) throw new IllegalArgumentException("handle is not a LocalPlayer");
        return this.user(p);
    }

    public abstract U user(LocalPlayer player);

    @Override
    public DialogRenderer<Holder<net.minecraft.server.dialog.Dialog>> renderer() {
        return new VanillaDialogRenderer(this.audiences, this.registryAccess());
    }

    private @Nullable RegistryAccess registryAccess() {
        ClientPacketListener connection = this.client.getConnection();
        if (connection != null) return connection.registryAccess();
        IntegratedServer integrated = this.client.getSingleplayerServer();
        if (integrated != null) return integrated.registryAccess();
        return null;
    }

}
