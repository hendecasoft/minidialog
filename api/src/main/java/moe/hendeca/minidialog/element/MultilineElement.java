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
public final class MultilineElement extends AbstractStructuralElement {

    public static final String NAME = "multiline";

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID,
            Property.MULTILINE_MAX_LINES,
            Property.MULTILINE_HEIGHT
    );

    private static final ElementTypeUnion CHILD_TYPES = new ElementTypeUnion();

    //

    public MultilineElement() {
        super(Kind.MULTILINE, NAME, PROPERTIES, CHILD_TYPES);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public MultilineElement id(@Nullable String id) {
        return (MultilineElement) super.id(id);
    }

    @Override
    @Contract("_ -> fail")
    public MultilineElement text(Component text) throws UnsupportedOperationException {
        return (MultilineElement) super.text(text);
    }

    @Contract(pure = true)
    public @Nullable Integer maxLines() {
        return this.props.get(Property.MULTILINE_MAX_LINES);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public MultilineElement maxLines(@Nullable Integer maxLines) throws IllegalArgumentException {
        if (maxLines != null && maxLines < 0)
            throw new IllegalArgumentException("maxLines may not be negative (got " + maxLines + ")");
        this.props.set(Property.MULTILINE_MAX_LINES, maxLines);
        return this;
    }

    @Contract(pure = true)
    public @Nullable Integer height() {
        return this.props.get(Property.MULTILINE_HEIGHT);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public MultilineElement height(@Nullable Integer height) throws IllegalArgumentException {
        if (height != null && height < 1)
            throw new IllegalArgumentException("height may not be less than 1 (got " + height + ")");
        this.props.set(Property.MULTILINE_HEIGHT, height);
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public MultilineElement append(Node child) {
        return (MultilineElement) super.append(child);
    }

    @Override
    public MultilineElement copy() {
        MultilineElement ret = new MultilineElement();
        this.copyTo(ret);
        return ret;
    }

}
