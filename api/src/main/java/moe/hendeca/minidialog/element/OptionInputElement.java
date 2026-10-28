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
public final class OptionInputElement extends InputElement {

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID,
            Property.OPTION_INPUT_INITIAL,
            Property.OPTION_INPUT_WIDTH
    );

    private static final ElementTypeUnion CHILD_TYPES = new ElementTypeUnion(
            LabelElement.class,
            OptionElement.class
    );

    //

    public OptionInputElement() {
        super(Kind.OPTION_INPUT, PROPERTIES, CHILD_TYPES);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public OptionInputElement id(@Nullable String id) {
        return (OptionInputElement) super.id(id);
    }

    @Override
    @Contract("_ -> fail")
    public OptionInputElement text(Component text) throws UnsupportedOperationException {
        return (OptionInputElement) super.text(text);
    }

    @Contract(pure = true)
    public @Nullable String initial() {
        return this.props.get(Property.OPTION_INPUT_INITIAL);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public OptionInputElement initial(@Nullable String initial) {
        this.props.set(Property.OPTION_INPUT_INITIAL, initial);
        return this;
    }

    @Contract(pure = true)
    public int width() {
        return this.props.get(Property.OPTION_INPUT_WIDTH);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public OptionInputElement width(int width) {
        if (width < 1) throw new IllegalArgumentException("width may not be less than 1 (got " + width + ")");
        this.props.set(Property.OPTION_INPUT_WIDTH, width);
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public OptionInputElement append(Node child) {
        return (OptionInputElement) super.append(child);
    }

    @Override
    public OptionInputElement copy() {
        OptionInputElement ret = new OptionInputElement();
        this.copyTo(ret);
        return ret;
    }

}
