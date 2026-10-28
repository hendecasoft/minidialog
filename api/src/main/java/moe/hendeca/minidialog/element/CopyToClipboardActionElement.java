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

import java.util.Objects;

@NullMarked
public final class CopyToClipboardActionElement extends ActionElement {

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID,
            Property.ACTION_WIDTH,
            Property.ACTION_VALUE
    );

    private static final ElementTypeUnion CHILD_TYPES = new ElementTypeUnion(
            LabelElement.class,
            TooltipElement.class
    );

    //

    private CopyToClipboardActionElement() {
        super(Kind.COPY_TO_CLIPBOARD_ACTION, PROPERTIES, CHILD_TYPES);
    }

    public CopyToClipboardActionElement(String value) {
        this();
        this.props.set(Property.OPTION_VALUE, Objects.requireNonNull(value, "value must not be null"));
    }

    //


    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public CopyToClipboardActionElement id(@Nullable String id) {
        return (CopyToClipboardActionElement) super.id(id);
    }

    @Override
    @Contract("_ -> fail")
    public CopyToClipboardActionElement text(Component text) throws UnsupportedOperationException {
        return (CopyToClipboardActionElement) super.text(text);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public CopyToClipboardActionElement width(int width) throws IllegalArgumentException {
        return (CopyToClipboardActionElement) super.width(width);
    }

    @Contract(pure = true)
    public String value() {
        return this.props.getRequired(Property.OPTION_VALUE);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public CopyToClipboardActionElement value(String value) {
        this.props.set(Property.OPTION_VALUE, Objects.requireNonNull(value, "value must not be null"));
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public CopyToClipboardActionElement append(Node child) {
        return (CopyToClipboardActionElement) super.append(child);
    }

    @Override
    public CopyToClipboardActionElement copy() {
        CopyToClipboardActionElement ret = new CopyToClipboardActionElement();
        this.copyTo(ret);
        return ret;
    }

}
