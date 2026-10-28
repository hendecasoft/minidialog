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

package moe.hendeca.minidialog.response;

import moe.hendeca.minidialog.TokenizedDialogKeyFactory;
import moe.hendeca.minidialog.VanillaDialogUser;
import moe.hendeca.minidialog.element.*;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@ApiStatus.Internal
public abstract class VanillaDialogResponse<U extends VanillaDialogUser<?, ?>> extends AbstractDialogResponse<Tag, U> {

    protected VanillaDialogResponse(
            @Nullable Tag handle,
            U user,
            DialogElement document,
            TokenizedDialogKeyFactory keyFactory
    ) {
        super(handle, user, document, keyFactory);
    }

    //

    @Override
    public @Nullable Tag handle() {
        return super.handle();
    }

    @Override
    public @UnknownNullability String value(TextInputElement element) {
        return "";
    }

    @Override
    public @UnknownNullability OptionElement value(OptionInputElement element) {
        return null;
    }

    @Override
    public @UnknownNullability Float value(RangeInputElement element) {
        return 0f;
    }

    @Override
    public @UnknownNullability Boolean value(BooleanInputElement element) {
        return null;
    }

}
