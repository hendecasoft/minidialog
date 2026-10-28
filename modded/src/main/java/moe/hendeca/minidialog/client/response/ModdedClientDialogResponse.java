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

package moe.hendeca.minidialog.client.response;

import moe.hendeca.minidialog.TokenizedDialogKeyFactory;
import moe.hendeca.minidialog.client.ModdedClientDialogUser;
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.response.ModdedDialogResponse;
import moe.hendeca.minidialog.response.VanillaDialogResponse;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ModdedClientDialogResponse
        extends VanillaDialogResponse<ModdedClientDialogUser>
        implements ModdedDialogResponse
{

    @ApiStatus.Internal
    public ModdedClientDialogResponse(
            @Nullable Tag handle,
            ModdedClientDialogUser user,
            DialogElement document,
            TokenizedDialogKeyFactory keyFactory
    ) {
        super(handle, user, document, keyFactory);
    }

}
