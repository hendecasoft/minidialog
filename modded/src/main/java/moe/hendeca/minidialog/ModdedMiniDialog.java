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
import moe.hendeca.minidialog.manager.DialogManager;
import moe.hendeca.minidialog.render.DialogRenderer;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@ApiStatus.NonExtendable
public interface ModdedMiniDialog extends MiniDialog {

    static ModdedMiniDialog miniDialog() {
        return miniDialog(null);
    }

    static ModdedMiniDialog miniDialog(@Nullable MinecraftServer server) {
        return ModdedMiniDialogProvider.get(server);
    }

    //

    @Override
    DialogRenderer<Holder<net.minecraft.server.dialog.Dialog>> renderer();

    @Override
    ModdedDialogUser user(Object handle);

    @Override
    ModdedDialog init(DialogElement element, @Nullable Class<? extends DialogManager> managerClass, Object... managerArgs);

    @Override
    ModdedDialog init(DialogElement element);

}
