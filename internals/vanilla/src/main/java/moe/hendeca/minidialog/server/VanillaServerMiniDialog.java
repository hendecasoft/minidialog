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

import moe.hendeca.minidialog.VanillaMiniDialog;
import moe.hendeca.minidialog.render.DialogRenderer;
import moe.hendeca.minidialog.render.VanillaDialogRenderer;
import moe.hendeca.minidialog.util.ParamCasts;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

@NullMarked
@ApiStatus.Internal
public abstract class VanillaServerMiniDialog<D extends VanillaServerDialog<?, ?>, U extends VanillaServerDialogUser>
        extends VanillaMiniDialog<D>
{

    protected final MinecraftServer server;
    final VanillaServerPacketListener packetListener;
    protected final MinecraftServerAudiences audiences;
    protected final VanillaDialogRenderer renderer;

    protected VanillaServerMiniDialog(MinecraftServer server) {
        this.server = server;
        this.packetListener = new VanillaServerPacketListener(server);
        this.audiences = MinecraftServerAudiences.of(server);
        this.renderer = new VanillaDialogRenderer(this.audiences, server.registryAccess());
    }

    //

    @Contract(pure = true)
    public MinecraftServer server() {
        return this.server;
    }

    @Override
    @Contract(pure = true)
    public MinecraftServerAudiences audiences() {
        return this.audiences;
    }

    @Override
    public DialogRenderer<Holder<net.minecraft.server.dialog.Dialog>> renderer() {
        return this.renderer;
    }

    @Override
    public U user(@ParamCasts(ServerPlayer.class) Object handle) {
        if (!(handle instanceof ServerPlayer p)) throw new IllegalArgumentException("handle is not a ServerPlayer");
        return this.user(p);
    }

    protected abstract U user(ServerPlayer handle);

}
