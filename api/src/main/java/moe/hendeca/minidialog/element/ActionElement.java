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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@ApiStatus.NonExtendable
public abstract sealed class ActionElement
        extends AbstractStructuralElement
        permits OpenUrlActionElement,
                RunCommandActionElement,
                SuggestCommandActionElement,
                CopyToClipboardActionElement,
                ShowDialogActionElement,
                CustomActionElement
{

    public static final String NAME = "action";

    //

    protected ActionElement(
            @Kind int kind,
            PropertySet allowedProperties,
            ElementTypeUnion allowedTypes
    ) {
        super(kind, NAME, allowedProperties, allowedTypes);
    }

    //


    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public ActionElement id(@Nullable String id) {
        return (ActionElement) super.id(id);
    }

    @Override
    @Contract("_ -> fail")
    public ActionElement text(Component text) throws UnsupportedOperationException {
        return (ActionElement) super.text(text);
    }

    @Contract(pure = true)
    public int width() {
        return this.props.get(Property.ACTION_WIDTH);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public ActionElement width(int width) throws IllegalArgumentException {
        this.props.set(Property.ACTION_WIDTH, width);
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public ActionElement append(Node child) {
        return (ActionElement) super.append(child);
    }

    @Override
    public abstract ActionElement copy();

}
