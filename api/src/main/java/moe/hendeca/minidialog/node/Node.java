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

package moe.hendeca.minidialog.node;

import moe.hendeca.minidialog.element.Element;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
@ApiStatus.NonExtendable
public interface Node {

    @Contract("_ -> new")
    static Node text(Component value) {
        return new TextNode(Objects.requireNonNull(value, "value must not be null"));
    }

    //

    @Contract(pure = true)
    NodeType nodeType();

    @Contract(pure = true)
    default boolean isText() {
        return NodeType.TEXT == this.nodeType();
    }

    @Contract(pure = true)
    Component asText() throws UnsupportedOperationException;

    @Contract(pure = true)
    default boolean isElement() {
        return NodeType.ELEMENT == this.nodeType();
    }

    @Contract(pure = true)
    Element asElement() throws UnsupportedOperationException;

}
