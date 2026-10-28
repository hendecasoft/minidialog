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
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.Lock;

@NullMarked
public final class DialogElement extends AbstractStructuralElement {

    public static final String NAME = "dialog";

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID,
            Property.DIALOG_SRC,
            Property.DIALOG_VERSION,
            Property.DIALOG_CAN_CLOSE_WITH_ESCAPE,
            Property.DIALOG_PAUSE,
            Property.DIALOG_AFTER_ACTION
    );

    private static final ElementTypeUnion CHILD_TYPES = new ElementTypeUnion(
            TitleElement.class,
            ExternalTitleElement.class,
            BodyElement.class,
            InputsElement.class,
            ActionsElement.class,
            FooterElement.class
    );

    //

    private @Nullable Map<String, IdLookupEntry> idLookup = null;

    public DialogElement() {
        super(Kind.DIALOG, NAME, PROPERTIES, CHILD_TYPES);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public DialogElement id(@Nullable String id) {
        return (DialogElement) super.id(id);
    }

    @Override
    @Contract("_ -> fail")
    public DialogElement text(Component text) throws UnsupportedOperationException {
        return (DialogElement) super.text(text);
    }

    @Contract(pure = true)
    public int version() {
        return this.props.get(Property.DIALOG_VERSION);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public DialogElement version(int version) throws IllegalArgumentException {
        if (1 != version) throw new IllegalArgumentException("Invalid version: " + version);
        this.props.set(Property.DIALOG_VERSION, version);
        return this;
    }

    @Contract(pure = true)
    public @Nullable Key src() {
        return this.props.get(Property.DIALOG_SRC);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public DialogElement src(@Nullable Key key) {
        this.props.set(Property.DIALOG_SRC, key);
        return this;
    }

    @Contract(pure = true)
    public boolean canCloseWithEscape() {
        return this.props.get(Property.DIALOG_CAN_CLOSE_WITH_ESCAPE);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public DialogElement canCloseWithEscape(boolean canCloseWithEscape) {
        this.props.set(Property.DIALOG_CAN_CLOSE_WITH_ESCAPE, canCloseWithEscape);
        return this;
    }

    @Contract(pure = true)
    public boolean pause() {
        return this.props.get(Property.DIALOG_PAUSE);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public DialogElement pause(boolean pause) {
        this.props.set(Property.DIALOG_PAUSE, pause);
        return this;
    }

    @Contract(pure = true)
    public AfterAction afterAction() {
        return this.props.get(Property.DIALOG_AFTER_ACTION);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public DialogElement afterAction(AfterAction afterAction) {
        this.props.set(Property.DIALOG_AFTER_ACTION, Objects.requireNonNull(afterAction, "afterAction must not be null"));
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public DialogElement append(Node child) {
        return (DialogElement) super.append(child);
    }

    @Override
    protected void onModify(boolean structural) {
        final Lock lock = this.lock.writeLock();
        lock.lock();
        try {
            this.idLookup = null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public DialogElement copy() {
        DialogElement ret = new DialogElement();
        this.copyTo(ret);
        return ret;
    }

    public boolean isReference() {
        return this.src() != null &&
                this.props.count() == 1 &&
                this.directChildren.isEmpty();
    }

    public @Unmodifiable Set<String> identifiers() {
        return resolveIdLookup().keySet();
    }

    public @Nullable Element find(String id) {
        Objects.requireNonNull(id, "id must not be null");
        Map<String, IdLookupEntry> map = resolveIdLookup();
        IdLookupEntry entry = map.get(id);
        if (entry == null) return null;
        return entry.element;
    }

    public boolean hasShadowedIds() {
        for (IdLookupEntry entry : this.resolveIdLookup().values()) {
            if (entry.multi) return true;
        }
        return false;
    }

    private Map<String, IdLookupEntry> resolveIdLookup() {
        Map<String, IdLookupEntry> map;
        final Lock rd = this.lock.readLock();
        rd.lock();
        try {
            map = this.idLookup;
            if (map != null) return map;
        } finally {
            rd.unlock();
        }

        map = calcIdLookup();
        final Lock wr = this.lock.writeLock();
        wr.lock();
        try {
            this.idLookup = map;
        } finally {
            wr.unlock();
        }

        return map;
    }

    private Map<String, IdLookupEntry> calcIdLookup() {
        Map<String, IdLookupEntry> map = new HashMap<>();
        Queue<AbstractElement<?>> queue = new LinkedList<>();
        queue.add(this);

        AbstractElement<?> next;
        while ((next = queue.poll()) != null) {
            String id;
            final Lock lock = next.lock.readLock();
            lock.lock();
            try {
                id = next.props.get(Property.ID);
                for (Node node : next.directChildren) {
                    if (!node.isElement()) continue;
                    queue.add((AbstractElement<?>) node.asElement());
                }
            } finally {
                lock.unlock();
            }
            if (id == null) continue;
            final AbstractElement<?> finalNext = next;
            map.compute(id, (String ignored, @Nullable IdLookupEntry existing) -> {
                if (existing == null) return new IdLookupEntry(finalNext, false);
                return new IdLookupEntry(finalNext, true);
            });
        }

        return Collections.unmodifiableMap(map);
    }

    //

    public enum AfterAction {
        CLOSE("close"),
        NONE("none"),
        WAIT_FOR_RESPONSE("waitForResponse");

        //

        private static final Map<String, AfterAction> BY_XML_NAME;
        static {
            AfterAction[] values = values();
            Map<String, AfterAction> byXmlName = HashMap.newHashMap(values.length);
            for (AfterAction value : values) byXmlName.put(value.xmlName, value);
            BY_XML_NAME = Collections.unmodifiableMap(byXmlName);
        }

        @Contract("null -> null; !null -> _")
        public static @Nullable AfterAction fromXmlName(@Nullable String xmlName) {
            if (xmlName == null) return null;
            return BY_XML_NAME.get(xmlName);
        }

        //

        private final String xmlName;

        AfterAction(String xmlName) {
            this.xmlName = xmlName;
        }

        public String xmlName() {
            return this.xmlName;
        }

    }

    private record IdLookupEntry(
            Element element,
            boolean multi
    ) { }

}
