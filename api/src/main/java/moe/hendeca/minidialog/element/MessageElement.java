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
public final class MessageElement extends AbstractTextElement {

    public static final String NAME = "message";

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID,
            Property.MESSAGE_WIDTH
    );

    //

    public MessageElement() {
        super(Kind.MESSAGE, NAME, PROPERTIES);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public MessageElement id(@Nullable String id) {
        return (MessageElement) super.id(id);
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public MessageElement text(Component text) {
        return (MessageElement) super.text(text);
    }

    @Contract(pure = true)
    public int width() {
        return this.props.get(Property.MESSAGE_WIDTH);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public MessageElement width(int width) throws IllegalArgumentException {
        if (width < 1) throw new IllegalArgumentException("width must be at least 1 (got " + width + ")");
        this.props.set(Property.MESSAGE_WIDTH, width);
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public MessageElement append(Node child) {
        return (MessageElement) super.append(child);
    }

    @Override
    public MessageElement copy() {
        MessageElement ret = new MessageElement();
        this.copyTo(ret);
        return ret;
    }

}
