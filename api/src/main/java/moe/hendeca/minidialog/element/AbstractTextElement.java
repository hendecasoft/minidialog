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

import java.util.Iterator;
import java.util.concurrent.locks.Lock;

@NullMarked
@ApiStatus.Internal
abstract class AbstractTextElement extends AbstractElement<Node> {

    protected AbstractTextElement(@Kind int kind, String name, PropertySet allowedProperties) {
        super(kind, name, allowedProperties);
    }

    //

    @Override
    public Component text() {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            Iterator<Node> iter = this.directChildren.iterator();
            if (!iter.hasNext()) return Component.empty();
            Component n0 = iter.next().asText();
            if (!iter.hasNext()) return n0;
            Component ret = Component.empty();
            ret = ret.append(n0);
            while (iter.hasNext())
                ret = ret.append(iter.next().asText());
            return ret;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public AbstractTextElement text(Component content) {
        final Lock lock = this.lock.writeLock();
        lock.lock();
        try {
            this.clear();
            this.addUnchecked(Node.text(content));
            return this;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void add(Node child) throws IllegalArgumentException {
        if (!child.isText())
            throw new IllegalArgumentException("Only text nodes can be added to this element (got " + child + ")");
        this.addUnchecked(child);
    }

    @Override
    public Component asText() {
        return this.text();
    }

}
