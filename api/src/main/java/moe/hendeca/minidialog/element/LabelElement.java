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
public final class LabelElement extends AbstractTextElement {

    public static final String NAME = "label";

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID,
            Property.LABEL_VISIBLE,
            Property.LABEL_FORMAT
    );

    //

    public LabelElement() {
        super(Kind.LABEL, NAME, PROPERTIES);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public LabelElement id(@Nullable String id) {
        return (LabelElement) super.id(id);
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public LabelElement text(Component text) {
        return (LabelElement) super.text(text);
    }

    @Contract(pure = true)
    public boolean visible() {
        return this.props.get(Property.LABEL_VISIBLE);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public LabelElement visible(boolean visible) {
        this.props.set(Property.LABEL_VISIBLE, visible);
        return this;
    }

    @Contract(pure = true)
    public String format() {
        return this.props.get(Property.LABEL_FORMAT);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public LabelElement format(String format) {
        this.props.set(Property.LABEL_FORMAT, format);
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public LabelElement append(Node child) {
        return (LabelElement) super.append(child);
    }

    @Override
    public LabelElement copy() {
        LabelElement ret = new LabelElement();
        this.copyTo(ret);
        return ret;
    }
}
