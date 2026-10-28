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

import java.util.*;
import java.util.concurrent.locks.StampedLock;

@NullMarked
@ApiStatus.Internal
abstract class AbstractStructuralElement extends AbstractElement<Element> {

    protected final ElementTypeUnion allowedTypes;

    protected AbstractStructuralElement(
            @Kind int kind,
            String name,
            PropertySet allowedProperties,
            ElementTypeUnion allowedTypes
    ) {
        super(kind, name, allowedProperties);
        this.allowedTypes = allowedTypes;
    }

    //

    @Override
    @Contract(" -> fail")
    public Component text() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Element does not have text");
    }

    @Override
    @Contract("_ -> fail")
    public Element text(Component text) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Cannot set text of this element");
    }

    @Override
    public void add(Node child) throws IllegalArgumentException {
        if (!child.isElement())
            throw new IllegalArgumentException("Only element nodes can be added to this element (got " + child + ")");

        Element element = child.asElement();
        Class<? extends Element> cls = element.getClass();
        if (this.allowedTypes.includes(cls)) {
            this.addUnchecked(element);
            return;
        }

        throw new IllegalArgumentException("Element of type " + cls.getName() + " cannot be added to this element");
    }

    //

    /** @implNote Intentionally not a Set as the self-growing behavior violates the contract of a Set */
    protected static final class ElementTypeUnion {

        private final StampedLock lock;
        private final Set<Class<? extends Element>> backing;

        @SafeVarargs
        ElementTypeUnion(Class<? extends Element>... values) {
            this.lock = new StampedLock();
            this.backing = new HashSet<>(List.of(values));
        }

        //

        public boolean includes(Class<? extends Element> type) {
            long stamp = this.lock.readLock();
            try {
                final Set<Class<? extends Element>> backing = this.backing;
                if (backing.contains(type)) return true;
                if (!checkHierarchy(backing, type)) return false;
                long s2 = this.lock.tryConvertToWriteLock(stamp);
                if (s2 == 0L) {
                    this.lock.unlock(stamp);
                    stamp = this.lock.writeLock();
                } else {
                    stamp = s2;
                }
                backing.add(type);
                return true;
            } finally {
                this.lock.unlock(stamp);
            }
        }

        private static boolean checkHierarchy(
                Set<Class<? extends Element>> haystack,
                Class<? extends Element> needle
        ) {
            Queue<Class<? extends Element>> queue = new LinkedList<>();
            Class<? extends Element> next = needle;
            while (true) {
                Class<?> superType = next.getSuperclass();
                if (superType != null && Element.class.isAssignableFrom(superType)) {
                    queue.add(superType.asSubclass(Element.class));
                }
                for (Class<?> superInterface : next.getInterfaces()) {
                    if (!Element.class.isAssignableFrom(superInterface)) continue;
                    queue.add(superInterface.asSubclass(Element.class));
                }
                next = queue.poll();
                if (next == null) break;
                if (haystack.contains(next)) return true;
            }
            return false;
        }

    }

}
