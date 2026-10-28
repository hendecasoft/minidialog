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

import moe.hendeca.minidialog.VanillaDialog;
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.manager.DialogManager;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.server.dialog.Dialog;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@ApiStatus.Internal
public abstract class VanillaServerDialog<
        I extends VanillaServerMiniDialog<?, ?>,
        U extends VanillaServerDialogUser
        > extends VanillaDialog<I, U>
{

    protected VanillaServerDialog(
            I instance,
            Class<U> userType,
            DialogElement document,
            @Nullable DialogManager manager
    ) {
        super(instance, userType, document, manager);
    }

    //

    @Override
    protected void clear(U user) {
        user.handle().connection.send(ClientboundClearDialogPacket.INSTANCE);
    }

    @Override
    protected void send(U user, Holder<Dialog> dialog) {
        user.handle().openDialog(dialog);
    }

    @Contract(pure = true)
    protected final VanillaServerPacketListener packetListener() {
        return this.instance.packetListener;
    }

}
