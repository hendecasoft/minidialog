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

package moe.hendeca.minidialog.element;

import moe.hendeca.minidialog.node.Node;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@ApiStatus.NonExtendable
public abstract sealed class InputElement
        extends AbstractStructuralElement
        permits TextInputElement,
            BooleanInputElement,
            OptionInputElement,
            RangeInputElement
{

    public static final String NAME = "input";

    //

    protected InputElement(
            @Kind int kind,
            PropertySet allowedProperties,
            ElementTypeUnion allowedTypes
    ) {
        super(kind, NAME, allowedProperties, allowedTypes);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public InputElement id(@Nullable String id) {
        return (InputElement) super.id(id);
    }

    @Override
    @Contract("_ -> fail")
    public InputElement text(Component text) throws UnsupportedOperationException {
        return (InputElement) super.text(text);
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public InputElement append(Node child) {
        return (InputElement) super.append(child);
    }

    @Override
    public abstract InputElement copy();

}
