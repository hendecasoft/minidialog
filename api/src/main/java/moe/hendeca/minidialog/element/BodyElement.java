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
public final class BodyElement extends AbstractStructuralElement {

    public static final String NAME = "body";

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID
    );

    private static final ElementTypeUnion CHILD_TYPES = new ElementTypeUnion(
            MessageElement.class,
            ItemElement.class
    );

    //

    public BodyElement() {
        super(Kind.BODY, NAME, PROPERTIES, CHILD_TYPES);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public BodyElement id(@Nullable String id) {
        return (BodyElement) super.id(id);
    }

    @Override
    @Contract("_ -> fail")
    public BodyElement text(Component text) throws UnsupportedOperationException {
        return (BodyElement) super.text(text);
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public BodyElement append(Node child) {
        return (BodyElement) super.append(child);
    }

    @Override
    public BodyElement copy() {
        BodyElement ret = new BodyElement();
        this.copyTo(ret);
        return ret;
    }

}
