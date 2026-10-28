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
public final class DescriptionElement extends AbstractTextElement {

    public static final String NAME = "description";

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID,
            Property.DESCRIPTION_WIDTH
    );

    //

    public DescriptionElement() {
        super(Kind.DESCRIPTION, NAME, PROPERTIES);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public DescriptionElement id(@Nullable String id) {
        return (DescriptionElement) super.id(id);
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public DescriptionElement text(Component text) {
        return (DescriptionElement) super.text(text);
    }

    @Contract(pure = true)
    public int width() {
        return this.props.get(Property.DESCRIPTION_WIDTH);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public DescriptionElement width(int width) throws IllegalArgumentException {
        if (width < 1) throw new IllegalArgumentException("width must be at least 1 (got " + width + ")");
        this.props.set(Property.DESCRIPTION_WIDTH, width);
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public DescriptionElement append(Node child) {
        return (DescriptionElement) super.append(child);
    }

    @Override
    public DescriptionElement copy() {
        DescriptionElement ret = new DescriptionElement();
        this.copyTo(ret);
        return ret;
    }

}
