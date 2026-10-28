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

import java.util.concurrent.locks.Lock;

@NullMarked
public final class FooterElement extends AbstractStructuralElement {

    public static final String NAME = "footer";

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID
    );

    private static final ElementTypeUnion CHILD_TYPES = new ElementTypeUnion(ActionElement.class);

    //

    public FooterElement() {
        super(Kind.FOOTER, NAME, PROPERTIES, CHILD_TYPES);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public FooterElement id(@Nullable String id) {
        return (FooterElement) super.id(id);
    }

    @Override
    @Contract("_ -> fail")
    public FooterElement text(Component text) throws UnsupportedOperationException {
        return (FooterElement) super.text(text);
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public FooterElement append(Node child) {
        return (FooterElement) super.append(child);
    }

    @Override
    protected void addUnchecked(Element node) {
        int count;
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            count = this.directChildren.size();
        } finally {
            lock.unlock();
        }
        if (count >= 2) throw new IllegalStateException("cannot add more than 2 elements to footer");
        super.addUnchecked(node);
    }

    @Override
    public FooterElement copy() {
        FooterElement ret = new FooterElement();
        this.copyTo(ret);
        return ret;
    }

}
