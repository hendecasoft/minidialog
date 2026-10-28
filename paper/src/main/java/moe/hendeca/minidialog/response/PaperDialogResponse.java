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

import io.papermc.paper.dialog.DialogResponseView;
import moe.hendeca.minidialog.PaperDialogUser;
import moe.hendeca.minidialog.TokenizedDialogKeyFactory;
import moe.hendeca.minidialog.element.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
@NullMarked
public final class PaperDialogResponse
        extends AbstractDialogResponse<DialogResponseView, PaperDialogUser>
{

    @ApiStatus.Internal
    public PaperDialogResponse(
            @Nullable DialogResponseView handle,
            PaperDialogUser user,
            DialogElement document,
            TokenizedDialogKeyFactory keyFactory
    ) {
        super(handle, user, document, keyFactory);
    }

    //

    @Override
    public @Nullable DialogResponseView handle() {
        return super.handle();
    }

    @Override
    public @UnknownNullability String value(TextInputElement element) {
        DialogResponseView handle = this.handle;
        if (handle == null) return null;
        String key = this.keyOf(element);
        if (key == null) return null;
        return handle.getText(key);
    }

    @Override
    public @UnknownNullability OptionElement value(OptionInputElement element) {
        DialogResponseView handle = this.handle;
        if (handle == null) return null;
        String key = this.keyOf(element);
        if (key == null) return null;
        String rawOptionKey = handle.getText(key);
        if (rawOptionKey == null) return null;
        return this.findOption(element, rawOptionKey);
    }

    @Override
    public @UnknownNullability Float value(RangeInputElement element) {
        DialogResponseView handle = this.handle;
        if (handle == null) return null;
        String key = this.keyOf(element);
        if (key == null) return null;
        return handle.getFloat(key);
    }

    @Override
    public @UnknownNullability Boolean value(BooleanInputElement element) {
        DialogResponseView handle = this.handle;
        if (handle == null) return null;
        String key = this.keyOf(element);
        if (key == null) return null;
        return handle.getBoolean(key);
    }

}
