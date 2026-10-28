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

import moe.hendeca.minidialog.ModdedMiniDialog;
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.manager.DialogManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@NullMarked
public final class ModdedClientMiniDialog
        extends VanillaClientMiniDialog<ModdedClientDialog, ModdedClientDialogUser>
        implements ModdedMiniDialog
{

    @ApiStatus.Internal
    public ModdedClientMiniDialog(Minecraft client) {
        super(client);
    }

    //

    @Override
    public ModdedClientDialogUser user(LocalPlayer player) {
        Objects.requireNonNull(player, "player must not be null");
        return new ModdedClientDialogUser(this, player);
    }

    @Override
    protected ModdedClientDialog newDialog(DialogElement document, @Nullable DialogManager manager) {
        return new ModdedClientDialog(this, document, manager);
    }

}
