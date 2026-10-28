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

import moe.hendeca.minidialog.FabricDialog;
import moe.hendeca.minidialog.client.response.FabricClientDialogResponse;
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.manager.DialogManager;
import net.minecraft.nbt.Tag;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class FabricClientDialog
        extends VanillaClientDialog<FabricClientMiniDialog, FabricClientDialogUser>
        implements FabricDialog
{

    FabricClientDialog(
            FabricClientMiniDialog instance,
            DialogElement document,
            @Nullable DialogManager manager
    ) {
        super(instance, FabricClientDialogUser.class, document, manager);
    }

    //

    @Override
    protected FabricClientDialogResponse newResponse(FabricClientDialogUser user, @Nullable Tag payload) {
        return new FabricClientDialogResponse(payload, user, this.document(), this.keyFactory);
    }

}
