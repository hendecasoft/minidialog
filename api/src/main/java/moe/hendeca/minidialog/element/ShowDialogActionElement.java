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
public final class ShowDialogActionElement extends ActionElement {

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID,
            Property.ACTION_WIDTH
    );

    private static final ElementTypeUnion CHILD_TYPES = new ElementTypeUnion(
            LabelElement.class,
            TooltipElement.class,
            DialogElement.class
    );

    //

    public ShowDialogActionElement() {
        super(Kind.SHOW_DIALOG_ACTION, PROPERTIES, CHILD_TYPES);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public ShowDialogActionElement id(@Nullable String id) {
        return (ShowDialogActionElement) super.id(id);
    }

    @Override
    @Contract("_ -> fail")
    public ShowDialogActionElement text(Component text) throws UnsupportedOperationException {
        return (ShowDialogActionElement) super.text(text);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public ShowDialogActionElement width(int width) throws IllegalArgumentException {
        return (ShowDialogActionElement) super.width(width);
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public ShowDialogActionElement append(Node child) {
        return (ShowDialogActionElement) super.append(child);
    }

    @Override
    public ShowDialogActionElement copy() {
        ShowDialogActionElement ret = new ShowDialogActionElement();
        this.copyTo(ret);
        return ret;
    }
}
