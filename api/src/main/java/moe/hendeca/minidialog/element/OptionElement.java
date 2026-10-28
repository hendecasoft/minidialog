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
public final class OptionElement extends AbstractTextElement {

    public static final String NAME = "option";

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID,
            Property.OPTION_VALUE
    );

    //

    public OptionElement() {
        super(Kind.OPTION, NAME, PROPERTIES);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public OptionElement id(@Nullable String id) {
        return (OptionElement) super.id(id);
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public OptionElement text(Component text) {
        return (OptionElement) super.text(text);
    }

    @Contract(pure = true)
    public @Nullable String value() {
        return this.props.get(Property.OPTION_VALUE);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public OptionElement value(@Nullable String value) throws IllegalArgumentException {
        this.props.set(Property.OPTION_VALUE, value);
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public OptionElement append(Node child) {
        return (OptionElement) super.append(child);
    }

    @Override
    public OptionElement copy() {
        OptionElement ret = new OptionElement();
        this.copyTo(ret);
        return ret;
    }

}
