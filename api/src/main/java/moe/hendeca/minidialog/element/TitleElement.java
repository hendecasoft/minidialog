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
public final class TitleElement extends AbstractTextElement {

    public static final String NAME = "title";

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID
    );

    //

    public TitleElement() {
        super(Kind.TITLE, NAME, PROPERTIES);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public TitleElement id(@Nullable String id) {
        return (TitleElement) super.id(id);
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public TitleElement text(Component text) {
        return (TitleElement) super.text(text);
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public TitleElement append(Node child) {
        return (TitleElement) super.append(child);
    }

    @Override
    public TitleElement copy() {
        TitleElement ret = new TitleElement();
        this.copyTo(ret);
        return ret;
    }

}
