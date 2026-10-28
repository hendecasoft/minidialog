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
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class InputsElement extends AbstractStructuralElement {

    public static final String NAME = "inputs";

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID
    );

    private static final ElementTypeUnion CHILD_TYPES = new ElementTypeUnion(InputElement.class);

    //

    public InputsElement() {
        super(Kind.INPUTS, NAME, PROPERTIES, CHILD_TYPES);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public InputsElement id(@Nullable String id) {
        return (InputsElement) super.id(id);
    }

    @Override
    @Contract("_ -> fail")
    public InputsElement text(Component text) throws UnsupportedOperationException {
        return (InputsElement) super.text(text);
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public InputsElement append(Node child) {
        return (InputsElement) super.append(child);
    }

    @Override
    public InputsElement copy() {
        InputsElement ret = new InputsElement();
        this.copyTo(ret);
        return ret;
    }

}
