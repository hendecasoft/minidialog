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

import moe.hendeca.minidialog.DialogUser;
import moe.hendeca.minidialog.TokenizedDialogKeyFactory;
import moe.hendeca.minidialog.element.*;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@NullMarked
@ApiStatus.Internal
public abstract class AbstractDialogResponse<H, U extends DialogUser> implements DialogResponse {

    protected final @UnknownNullability H handle;
    protected final U user;
    protected final DialogElement document;
    protected final TokenizedDialogKeyFactory keyFactory;

    protected AbstractDialogResponse(
            @UnknownNullability H handle,
            U user,
            DialogElement document,
            TokenizedDialogKeyFactory keyFactory
    ) {
        this.handle = handle;
        this.user = user;
        this.document = document;
        this.keyFactory = keyFactory;
    }

    //

    @Contract(pure = true)
    public @UnknownNullability H handle() {
        return this.handle;
    }

    @Override
    @Contract(pure = true)
    public U user() {
        return this.user;
    }

    protected final @Nullable String keyOf(InputElement element) {
        InputsElement inputs = this.inputs();
        if (inputs == null) return null;
        int idx = inputs.indexOf(element);
        if (idx == -1) return null;
        return this.keyFactory.inputKey(idx, element.id());
    }

    protected final @Nullable OptionElement findOption(OptionInputElement element, String rawOptionKey) {
        TokenizedDialogKeyFactory.ParseResult result = this.keyFactory.parse(rawOptionKey);
        if (result == null || result.type() != TokenizedDialogKeyFactory.ParseResult.Type.OPTION) return null;
        String id = result.id();
        if (id != null) {
            Element ret = this.document.find(id);
            if (ret instanceof OptionElement option &&
                    element.indexOf(option) != -1
            ) return option;
        }
        List<Element> children = element.children();
        for (Element child : children) {
            if (result.index() != element.indexOf(child)) continue;
            if (child instanceof OptionElement option) return option;
            break;
        }
        return null;
    }

    private @Nullable InputsElement inputs() {
        for (Element element : this.document.children()) {
            if (element instanceof InputsElement inputs) return inputs;
        }
        return null;
    }

}
