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
public final class BooleanInputElement extends InputElement {

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID,
            Property.BOOLEAN_INPUT_INITIAL
    );

    private static final ElementTypeUnion CHILD_TYPES = new ElementTypeUnion(
            LabelElement.class
    );

    //

    public BooleanInputElement() {
        super(Kind.BOOLEAN_INPUT, PROPERTIES, CHILD_TYPES);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public BooleanInputElement id(@Nullable String id) {
        return (BooleanInputElement) super.id(id);
    }

    @Override
    @Contract("_ -> fail")
    public BooleanInputElement text(Component text) throws UnsupportedOperationException {
        return (BooleanInputElement) super.text(text);
    }

    @Contract(pure = true)
    public boolean initial() {
        return this.props.get(Property.BOOLEAN_INPUT_INITIAL);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public BooleanInputElement initial(boolean initial) {
        this.props.set(Property.BOOLEAN_INPUT_INITIAL, initial);
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public BooleanInputElement append(Node child) {
        return (BooleanInputElement) super.append(child);
    }

    @Override
    public BooleanInputElement copy() {
        BooleanInputElement ret = new BooleanInputElement();
        this.copyTo(ret);
        return ret;
    }

}
