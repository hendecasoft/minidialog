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
public final class ActionsElement extends AbstractStructuralElement {

    public static final String NAME = "actions";

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID,
            Property.ACTIONS_COLUMNS
    );

    private static final ElementTypeUnion CHILD_TYPES = new ElementTypeUnion(ActionElement.class);

    //

    public ActionsElement() {
        super(Kind.ACTIONS, NAME, PROPERTIES, CHILD_TYPES);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public ActionsElement id(@Nullable String id) {
        return (ActionsElement) super.id(id);
    }

    @Override
    @Contract("_ -> fail")
    public ActionsElement text(Component text) throws UnsupportedOperationException {
        return (ActionsElement) super.text(text);
    }

    @Contract(pure = true)
    public int columns() {
        return this.props.get(Property.ACTIONS_COLUMNS);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public ActionsElement columns(int columns) {
        if (columns < 1) throw new IllegalArgumentException("columns must be at least 1 (got " + columns + ")");
        this.props.set(Property.ACTIONS_COLUMNS, columns);
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public ActionsElement append(Node child) {
        return (ActionsElement) super.append(child);
    }

    @Override
    public ActionsElement copy() {
        ActionsElement ret = new ActionsElement();
        this.copyTo(ret);
        return ret;
    }

}
