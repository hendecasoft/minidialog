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
public final class TextInputElement extends InputElement {

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID,
            Property.TEXT_INPUT_WIDTH,
            Property.TEXT_INPUT_INITIAL,
            Property.TEXT_INPUT_MAX_LENGTH
    );

    private static final ElementTypeUnion CHILD_TYPES = new ElementTypeUnion(
            LabelElement.class,
            MultilineElement.class
    );

    //

    public TextInputElement() {
        super(Kind.TEXT_INPUT, PROPERTIES, CHILD_TYPES);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public TextInputElement id(@Nullable String id) {
        return (TextInputElement) super.id(id);
    }

    @Override
    @Contract("_ -> fail")
    public TextInputElement text(Component text) throws UnsupportedOperationException {
        return (TextInputElement) super.text(text);
    }

    @Contract(pure = true)
    public int width() {
        return this.props.get(Property.TEXT_INPUT_WIDTH);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public TextInputElement width(int width) throws IllegalArgumentException {
        if (width < 1) throw new IllegalArgumentException("width may not be less than 1 (got" + width + ")");
        this.props.set(Property.TEXT_INPUT_WIDTH, width);
        return this;
    }

    @Contract(pure = true)
    public @Nullable String initial() {
        return this.props.get(Property.TEXT_INPUT_INITIAL);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public TextInputElement initial(@Nullable String initial) throws IllegalArgumentException {
        this.props.set(Property.TEXT_INPUT_INITIAL, initial);
        return this;
    }

    @Contract(pure = true)
    public int maxLength() {
        return this.props.get(Property.TEXT_INPUT_MAX_LENGTH);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public TextInputElement maxLength(int maxLength) throws IllegalArgumentException {
        if (maxLength < 0) throw new IllegalArgumentException("maxLength may not be negative (got" + maxLength + ")");
        this.props.set(Property.TEXT_INPUT_MAX_LENGTH, maxLength);
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public TextInputElement append(Node child) {
        return (TextInputElement) super.append(child);
    }

    @Override
    public TextInputElement copy() {
        TextInputElement ret = new TextInputElement();
        this.copyTo(ret);
        return ret;
    }

}
