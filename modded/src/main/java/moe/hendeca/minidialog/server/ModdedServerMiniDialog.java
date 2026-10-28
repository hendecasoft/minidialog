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

import moe.hendeca.minidialog.ModdedMiniDialog;
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.manager.DialogManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@NullMarked
public final class ModdedServerMiniDialog
        extends VanillaServerMiniDialog<ModdedServerDialog, ModdedServerDialogUser>
        implements ModdedMiniDialog
{

    @ApiStatus.Internal
    public ModdedServerMiniDialog(MinecraftServer server) {
        super(server);
    }

    //

    @Override
    protected ModdedServerDialogUser user(ServerPlayer handle) {
        Objects.requireNonNull(handle, "handle must not be null");
        return new ModdedServerDialogUser(this, handle);
    }

    @Override
    protected ModdedServerDialog newDialog(DialogElement document, @Nullable DialogManager manager) {
        return new ModdedServerDialog(this, document, manager);
    }

}
