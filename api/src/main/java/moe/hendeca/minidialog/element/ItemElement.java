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
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@NullMarked
public final class ItemElement extends AbstractStructuralElement {

    public static final String NAME = "item";

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID,
            Property.ITEM_SRC,
            Property.ITEM_COUNT,
            Property.ITEM_WIDTH,
            Property.ITEM_HEIGHT,
            Property.ITEM_SHOW_DECORATION,
            Property.ITEM_SHOW_TOOLTIP
    );

    private static final ElementTypeUnion CHILD_TYPES = new ElementTypeUnion(
            DescriptionElement.class
    );

    //

    private ItemElement() {
        super(Kind.ITEM, NAME, PROPERTIES, CHILD_TYPES);
    }

    public ItemElement(Key src) {
        this();
        this.props.set(Property.ITEM_SRC, Objects.requireNonNull(src, "src must not be null"));
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public ItemElement id(@Nullable String id) {
        return (ItemElement) super.id(id);
    }

    @Override
    @Contract("_ -> fail")
    public ItemElement text(Component text) throws UnsupportedOperationException {
        return (ItemElement) super.text(text);
    }

    @Contract(pure = true)
    public Key src() {
        return this.props.getRequired(Property.ITEM_SRC);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public ItemElement src(Key src) {
        this.props.set(Property.ITEM_SRC, Objects.requireNonNull(src, "src must not be null"));
        return this;
    }

    @Contract(pure = true)
    public int count() {
        return this.props.get(Property.ITEM_COUNT);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public ItemElement count(int count) throws IllegalArgumentException {
        if (count < 0) throw new IllegalArgumentException("count must not be negative (got " + count + ")");
        this.props.set(Property.ITEM_COUNT, count);
        return this;
    }

    @Contract(pure = true)
    public int width() {
        return this.props.get(Property.ITEM_WIDTH);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public ItemElement width(int width) throws IllegalArgumentException {
        if (width < 1) throw new IllegalArgumentException("width must be at least 1 (got " + width + ")");
        this.props.set(Property.ITEM_WIDTH, width);
        return this;
    }

    @Contract(pure = true)
    public int height() {
        return this.props.get(Property.ITEM_HEIGHT);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public ItemElement height(int height) throws IllegalArgumentException {
        if (height < 1) throw new IllegalArgumentException("height must be at least 1 (got " + height + ")");
        this.props.set(Property.ITEM_HEIGHT, height);
        return this;
    }

    @Contract(pure = true)
    public boolean showDecoration() {
        return this.props.getRequired(Property.ITEM_SHOW_DECORATION);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public ItemElement showDecoration(boolean showDecoration) {
        this.props.set(Property.ITEM_SHOW_DECORATION, showDecoration);
        return this;
    }

    @Contract(pure = true)
    public boolean showTooltip() {
        return this.props.getRequired(Property.ITEM_SHOW_TOOLTIP);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public ItemElement showTooltip(boolean showTooltip) {
        this.props.set(Property.ITEM_SHOW_TOOLTIP, showTooltip);
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public ItemElement append(Node child) {
        return (ItemElement) super.append(child);
    }

    @Override
    public ItemElement copy() {
        ItemElement ret = new ItemElement();
        this.copyTo(ret);
        return ret;
    }

}
