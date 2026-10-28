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
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@NullMarked
@ApiStatus.Internal
abstract class AbstractElement<C extends Node> implements Element {

    protected final @Kind int kind;
    protected final String name;
    protected final Properties props;
    protected final ReadWriteLock lock;
    private final ParentMap parents;
    protected final List<C> directChildren;
    private long modCount;

    protected AbstractElement(@Kind int kind, String name, PropertySet allowedProperties) {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        this.kind = kind;
        this.name = name;
        this.lock = lock;
        this.props = new Properties(this, allowedProperties);
        this.parents = new ParentMap(lock);
        this.directChildren = new ArrayList<>(1);
        this.modCount = 0L;
    }

    //

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public @Nullable String id() {
        return this.props.get(Property.ID);
    }

    @Override
    public Element id(@Nullable String id) {
        this.props.set(Property.ID, id);
        return this;
    }

    @Override
    public ElementObserver newObserver() {
        return new Observer(this);
    }

    @Override
    public @Unmodifiable List<C> children() {
        return new ChildrenView<>(this);
    }

    @Override
    public @Range(from = -1, to = Integer.MAX_VALUE) int indexOf(Element child) {
        AbstractElement<?> qual = (AbstractElement<?>) child;
        final Lock lock = qual.lock.readLock();
        lock.lock();
        try {
            return qual.parents.getInt(this);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        final Lock lock = this.lock.writeLock();
        lock.lock();
        try {
            Iterator<C> iter = this.directChildren.iterator();
            if (!iter.hasNext()) return;
            do {
                C next = iter.next();
                iter.remove();
                if (!(next.isElement())) continue;
                ((AbstractElement<?>) next.asElement()).parents.remove(this);
            } while (iter.hasNext());
            this.reportModified(true);
        } finally {
            lock.unlock();
        }
    }

    protected void addUnchecked(C node) {
        final Lock lock = this.lock.writeLock();
        lock.lock();
        try {
            if (node.isElement()) {
                AbstractElement<?> element = (AbstractElement<?>) node.asElement();
                if (this.isInHierarchy(element)) throw new IllegalArgumentException("Cannot add element (cyclic relationship)");
                element.parents.putInt(this, this.directChildren.size());
            }
            this.directChildren.add(node);
            this.reportModified(true);
        } finally {
            lock.unlock();
        }
    }

    private boolean isInHierarchy(AbstractElement<?> other) {
        if (this == other) return true;
        for (AbstractElement<?> parent : this.parents.keySet()) {
            if (parent.isInHierarchy(other)) return true;
        }
        return false;
    }

    @Override
    public final boolean remove(Node child) {
        final Lock lock = this.lock.writeLock();
        lock.lock();
        try {
            //noinspection SuspiciousMethodCalls
            if (!this.directChildren.remove(child)) return false;
            if (child.isElement()) ((AbstractElement<?>) child.asElement()).parents.removeElement(this);
            this.reportModified(true);
            return true;
        } finally {
            lock.unlock();
        }
    }

    private void reportModified(boolean structural) {
        final Lock lock = this.lock.writeLock();
        lock.lock();
        try {
            this.modCount++;
            this.onModify(structural);
            for (AbstractElement<?> parent : this.parents.keySet()) parent.reportModified(structural);
        } finally {
            lock.unlock();
        }
    }

    /** @implNote Called while holding write lock */
    protected void onModify(boolean structural) { }

    @Override
    @Contract("-> new")
    public abstract AbstractElement<C> copy();

    @SuppressWarnings("unchecked")
    protected final void copyTo(AbstractElement<C> other) {
        if (this == other) return;
        final Lock lock = other.lock.writeLock();
        lock.lock();
        try {
            final Lock rd = this.lock.readLock();
            rd.lock();
            try {
                this.props.copyTo(other.props);
                other.directChildren.clear();
                for (C child : this.directChildren) {
                    if (child instanceof Element el) {
                        Element copied = el.copy();
                        ((AbstractElement<?>) copied).parents.putInt(other, other.directChildren.size());
                        other.directChildren.add((C) copied);
                    } else {
                        other.directChildren.add(child);
                    }
                }
                other.reportModified(true);
            } finally {
                rd.unlock();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int hashCode() {
        int h = 7;
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            h = 31 * h + this.kind;
            h = 31 * h + this.props.hashCode();
            for (Node child : this.directChildren) h = 31 * h + child.hashCode();
        } finally {
            lock.unlock();
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractElement<?> other)) return false;
        if (this == other) return true;
        if (this.kind != other.kind) return false;
        final Lock al = this.lock.readLock();
        final Lock bl = other.lock.readLock();
        al.lock();
        try {
            bl.lock();
            try {
                return this.props.equals(other.props) &&
                        this.directChildren.equals(other.directChildren);
            } finally {
                bl.unlock();
            }
        } finally {
            al.unlock();
        }
    }

    @Override
    public String toString() {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return this.getClass().getSimpleName() +
                    "{props=" + this.props +
                    ", children=" + this.directChildren +
                    "}";
        } finally {
            lock.unlock();
        }
    }

    //

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.METHOD })
    @MagicConstant(valuesFromClass = Kind.class)
    protected @interface Kind {
        int DIALOG                   = 0x62f4bb19;
        int TITLE                    = 0x47f42ff8;
        int EXTERNAL_TITLE           = 0x60f64a87;
        int BODY                     = 0x94dc6686;
        int MESSAGE                  = 0x7281c9e8;
        int ITEM                     = 0x161a7da3;
        int DESCRIPTION              = 0x0756e301;
        int INPUTS                   = 0x2d4fe1f2;
        int TEXT_INPUT               = 0xeaa0af07;
        int BOOLEAN_INPUT            = 0xbdf5d460;
        int OPTION_INPUT             = 0x58316c0b;
        int RANGE_INPUT              = 0x76fbdc12;
        int LABEL                    = 0x3f5bed43;
        int MULTILINE                = 0x0964b5e5;
        int OPTION                   = 0xfb9fe296;
        int ACTIONS                  = 0xd670a9aa;
        int OPEN_URL_ACTION          = 0x97d4cd64;
        int RUN_COMMAND_ACTION       = 0xf4dd0685;
        int SUGGEST_COMMAND_ACTION   = 0x18cf2020;
        int COPY_TO_CLIPBOARD_ACTION = 0xf708f28c;
        int SHOW_DIALOG_ACTION       = 0xa339db32;
        int CUSTOM_ACTION            = 0x04053102;
        int TOOLTIP                  = 0x8a78ab3e;
        int FOOTER                   = 0x5259d7f0;
    }

    protected static final class Properties {

        final AbstractElement<?> parent;
        private final PropertySet allowed;
        private final @Nullable Object[] values;

        private Properties(AbstractElement<?> parent, PropertySet allowed) {
            this.parent = parent;
            this.allowed = allowed;
            this.values = new Object[allowed.count];
        }

        //

        public int count() {
            final Lock lock = this.parent.lock.readLock();
            lock.lock();
            try {
                int n = 0;
                for (Object v : this.values) {
                    if (v != null) n++;
                }
                return n;
            } finally {
                lock.unlock();
            }
        }

        public boolean supports(Property<?> property) {
            return this.allowed.contains(property);
        }

        public <T> @UnknownNullability T get(Property<T> property) throws IllegalArgumentException {
            final Lock lock = this.parent.lock.readLock();
            final int idx = this.map(property);
            Object value;
            lock.lock();
            try {
                value = this.values[idx];
            } finally {
                lock.unlock();
            }
            if (value == null) return property.defaultValue();
            return property.valueClass().cast(value);
        }

        public <T> @Nullable T get(Property.Explicit<T> property) throws IllegalArgumentException {
            return this.get((Property<T>) property);
        }

        public <T> T getRequired(Property<T> property) throws IllegalArgumentException, IllegalStateException {
            T value = this.get(property);
            if (value == null)
                throw new IllegalStateException("Element is missing required property " + property.name());
            return value;
        }

        public <T> void set(Property<T> property, @Nullable T value) {
            final Lock lock = this.parent.lock.writeLock();
            final int idx = this.map(property);
            lock.lock();
            try {
                this.values[idx] = value;
                this.parent.reportModified(false);
            } finally {
                lock.unlock();
            }
        }

        /** MUST BE PROPERLY LOCKED */
        private void copyTo(Properties other) {
            if (this == other) return;
            int vc = this.values.length;
            if (vc != other.values.length) throw new IllegalStateException();
            System.arraycopy(this.values, 0, other.values, 0, vc);
        }

        private int map(Property<?> key) {
            long m = this.allowed.mask;
            long f = 1L << (long) key.ordinal();
            if ((m & f) == 0)
                throw new IllegalArgumentException("Property " + key.name() + " is not applicable for this element");
            return Long.bitCount(m & (f - 1L));
        }

        @Override
        public int hashCode() {
            int h = 7;
            final Lock lock = this.parent.lock.readLock();
            lock.lock();
            try {
                for (Property<?> prop : this.allowed) {
                    Object v = this.values[this.map(prop)];
                    if (v == null) v = prop.defaultValue();
                    h = 31 * h + Objects.hashCode(v);
                }
            } finally {
                lock.unlock();
            }
            return h;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Properties other)) return false;
            if (this == other) return true;
            final Lock al = this.parent.lock.readLock();
            final Lock bl = other.parent.lock.readLock();
            al.lock();
            try {
                bl.lock();
                try {
                    for (Property<?> prop : PropertySet.VALUES) {
                        boolean supports = this.supports(prop);
                        if (supports != other.supports(prop)) return false;
                        if (!supports) continue;
                        Object av = this.values[this.map(prop)];
                        Object bv = other.values[other.map(prop)];
                        if (av == null) av = prop.defaultValue();
                        if (bv == null) bv = prop.defaultValue();
                        if (!Objects.equals(av, bv)) return false;
                    }
                } finally {
                    bl.unlock();
                }
            } finally {
                al.unlock();
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean sep = false;
            final Lock lock = this.parent.lock.readLock();
            lock.lock();
            try {
                for (Property<?> prop : this.allowed) {
                    Object v = this.values[this.map(prop)];
                    if (v == null || v.equals(prop.defaultValue())) continue;
                    if (sep) sb.append(", ");
                    sep = true;
                    sb.append(prop.name());
                    sb.append('=');
                    sb.append(v);
                }
            } finally {
                lock.unlock();
            }
            sb.append('}');
            return sb.toString();
        }

    }

    protected static final class PropertySet extends AbstractSet<Property<?>> {

        private static final Property<?>[] VALUES;
        static {
            VALUES = Property.values();
            assert VALUES.length <= 32;
        }

        //

        private final long mask;
        private final int count;

        PropertySet(Property<?>... allowed) {
            long mask = 0L;
            int count = 0;
            for (Property<?> prop : allowed) {
                long f = 1L << (long) prop.ordinal();
                if ((mask & f) != 0L) continue;
                mask |= f;
                count++;
            }
            this.mask = mask;
            this.count = count;
        }

        //

        @Override
        public int size() {
            return this.count;
        }

        @Override
        public boolean contains(Object o) {
            return o instanceof Property<?> prop &&
                    this.contains(prop);
        }

        public boolean contains(Property<?> property) {
            return (this.mask & (1L << (long) property.ordinal())) != 0;
        }

        @Override
        public Iterator<Property<?>> iterator() {
            return new Iter(this.mask);
        }

        //

        private static final class Iter implements Iterator<Property<?>> {

            private long mask;

            private Iter(long mask) {
                this.mask = mask;
            }

            //

            @Override
            public boolean hasNext() {
                return this.mask != 0L;
            }

            @Override
            public Property<?> next() {
                long mask = this.mask;
                if (mask == 0) throw new NoSuchElementException();
                int n = Long.numberOfTrailingZeros(mask);
                this.mask = mask ^ (1L << (long) n);
                return VALUES[n];
            }

        }

    }

    protected interface Property<T> {

        Property.Explicit<String> ID = PropertyImpl.registerExplicit("ID", String.class);
        Property<Integer> DIALOG_VERSION = PropertyImpl.register("DIALOG_VERSION", Integer.class, 1);
        Property<Boolean> DIALOG_CAN_CLOSE_WITH_ESCAPE = PropertyImpl.register("DIALOG_CAN_CLOSE_WITH_ESCAPE", Boolean.class, true);
        Property<Boolean> DIALOG_PAUSE = PropertyImpl.register("DIALOG_PAUSE", Boolean.class, true);
        Property<DialogElement.AfterAction> DIALOG_AFTER_ACTION = PropertyImpl.register("DIALOG_AFTER_ACTION", DialogElement.AfterAction.class, DialogElement.AfterAction.CLOSE);
        Property.Explicit<Key> DIALOG_SRC = PropertyImpl.registerExplicit("DIALOG_SRC", Key.class);
        Property<Integer> MESSAGE_WIDTH = PropertyImpl.register("MESSAGE_WIDTH", Integer.class, 200);
        Property.Explicit<Key> ITEM_SRC = PropertyImpl.registerExplicit("ITEM_SRC", Key.class);
        Property<Integer> ITEM_COUNT = PropertyImpl.register("ITEM_COUNT", Integer.class, 1);
        Property<Integer> ITEM_WIDTH = PropertyImpl.register("ITEM_WIDTH", Integer.class, 16);
        Property<Integer> ITEM_HEIGHT = PropertyImpl.register("ITEM_HEIGHT", Integer.class, 16);
        Property<Boolean> ITEM_SHOW_DECORATION = PropertyImpl.register("ITEM_SHOW_DECORATION", Boolean.class, true);
        Property<Boolean> ITEM_SHOW_TOOLTIP = PropertyImpl.register("ITEM_SHOW_TOOLTIP", Boolean.class, true);
        Property<Integer> DESCRIPTION_WIDTH = PropertyImpl.register("DESCRIPTION_WIDTH", Integer.class, 200);
        Property<Integer> TEXT_INPUT_WIDTH = PropertyImpl.register("TEXT_INPUT_WIDTH", Integer.class, 200);
        Property<String> TEXT_INPUT_INITIAL = PropertyImpl.register("TEXT_INPUT_INITIAL", String.class, "");
        Property<Integer> TEXT_INPUT_MAX_LENGTH = PropertyImpl.register("TEXT_INPUT_MAX_LENGTH", Integer.class, 32);
        Property<Boolean> BOOLEAN_INPUT_INITIAL = PropertyImpl.register("BOOLEAN_INPUT_INITIAL", Boolean.class, false);
        Property<Integer> OPTION_INPUT_WIDTH = PropertyImpl.register("OPTION_INPUT_WIDTH", Integer.class, 200);
        Property.Explicit<String> OPTION_INPUT_INITIAL = PropertyImpl.registerExplicit("OPTION_INPUT_INITIAL", String.class);
        Property<Integer> RANGE_INPUT_WIDTH = PropertyImpl.register("RANGE_INPUT_WIDTH", Integer.class, 200);
        Property.Explicit<Double> RANGE_INPUT_START = PropertyImpl.registerExplicit("RANGE_INPUT_START", Double.class);
        Property.Explicit<Double> RANGE_INPUT_END = PropertyImpl.registerExplicit("RANGE_INPUT_END", Double.class);
        Property.Explicit<Double> RANGE_INPUT_STEP = PropertyImpl.registerExplicit("RANGE_INPUT_STEP", Double.class);
        Property.Explicit<Double> RANGE_INPUT_INITIAL = PropertyImpl.registerExplicit("RANGE_INPUT_INITIAL", Double.class);
        Property<Boolean> LABEL_VISIBLE = PropertyImpl.register("LABEL_VISIBLE", Boolean.class, true);
        Property<String> LABEL_FORMAT = PropertyImpl.register("LABEL_FORMAT", String.class, "options.generic_value");
        Property<Integer> MULTILINE_MAX_LINES = PropertyImpl.registerExplicit("MULTILINE_MAX_LINES", Integer.class);
        Property<Integer> MULTILINE_HEIGHT = PropertyImpl.registerExplicit("MULTILINE_HEIGHT", Integer.class);
        Property.Explicit<String> OPTION_VALUE = PropertyImpl.registerExplicit("OPTION_VALUE", String.class);
        Property<Integer> ACTIONS_COLUMNS = PropertyImpl.register("ACTIONS_COLUMNS", Integer.class, 2);
        Property.Explicit<String> ACTION_VALUE = PropertyImpl.registerExplicit("ACTION_VALUE", String.class);
        Property<Integer> ACTION_WIDTH = PropertyImpl.register("ACTION_WIDTH", Integer.class, 150);

        @Contract("-> new")
        static Property<?>[] values() {
            return PropertyImpl.values();
        }

        //

        @Contract(pure = true)
        int ordinal();

        @Contract(pure = true)
        String name();

        @Contract(pure = true)
        Class<T> valueClass();

        @Contract(pure = true)
        @UnknownNullability T defaultValue();

        //

        interface Explicit<T> extends Property<T> {

            @Override
            @Contract("-> null")
            @Nullable T defaultValue();

        }

    }

    private static class PropertyImpl<T> implements Property<T> {

        private static final PropertyImpl<?>[] REGISTRY = new PropertyImpl<?>[Property.class.getDeclaredFields().length];
        private static int ORDINAL_HEAD = 0;

        @Contract("-> new")
        private static synchronized PropertyImpl<?>[] values() {
            int count = ORDINAL_HEAD;
            PropertyImpl<?>[] ret = new PropertyImpl<?>[count];
            System.arraycopy(REGISTRY, 0, ret, 0, count);
            return ret;
        }

        private static synchronized <V> PropertyImpl<V> register(
                String name,
                Class<V> valueClass,
                V defaultValue
        ) {
            int ordinal = ORDINAL_HEAD++;
            PropertyImpl<V> ret = new PropertyImpl<>(ordinal, name, valueClass, defaultValue);
            REGISTRY[ordinal] = ret;
            return ret;
        }

        private static synchronized <V> PropertyImpl.Explicit<V> registerExplicit(
                String name,
                Class<V> valueClass
        ) {
            int ordinal = ORDINAL_HEAD++;
            PropertyImpl.Explicit<V> ret = new PropertyImpl.Explicit<>(ordinal, name, valueClass);
            REGISTRY[ordinal] = ret;
            return ret;
        }

        //

        private final int ordinal;
        private final String name;
        private final Class<T> valueClass;
        private final @UnknownNullability T defaultValue;

        PropertyImpl(
                int ordinal,
                String name,
                Class<T> valueClass,
                @UnknownNullability T defaultValue
        ) {
            this.ordinal = ordinal;
            this.name = name;
            this.valueClass = valueClass;
            this.defaultValue = defaultValue;
        }

        //

        @Override
        public int ordinal() {
            return this.ordinal;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public Class<T> valueClass() {
            return this.valueClass;
        }

        @Override
        public @UnknownNullability T defaultValue() {
            return this.defaultValue;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(this.ordinal);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PropertyImpl<?> other &&
                    this.ordinal == other.ordinal;
        }

        @Override
        public String toString() {
            return this.name;
        }

        //

        private static final class Explicit<T>
                extends PropertyImpl<T>
                implements Property.Explicit<T>
        {

            Explicit(
                    int ordinal,
                    String name,
                    Class<T> valueClass
            ) {
                super(ordinal, name, valueClass, null);
            }

            @Override
            @Contract("-> null")
            public @Nullable T defaultValue() {
                return null;
            }

        }

    }

    private static final class Observer implements ElementObserver {

        private static long access(AbstractElement<?> element) {
            final Lock lock = element.lock.readLock();
            lock.lock();
            try {
                return element.modCount;
            } finally {
                lock.unlock();
            }
        }

        //

        private final AbstractElement<?> parent;
        private long n;

        Observer(AbstractElement<?> parent) {
            this.parent = parent;
            this.n = access(parent);
        }

        //

        @Override
        public boolean query(boolean update) {
            final long q = access(this.parent);
            if (q == this.n) return false;
            if (update) this.n = q;
            return true;
        }

    }

    static final class ParentMap extends AbstractMap<AbstractElement<?>, Integer> {

        private final ReadWriteLock lock;
        private int capacity;
        private int size;
        private @Nullable Bucket[] buckets;

        private ParentMap(ReadWriteLock lock) {
            this.lock = lock;
            this.capacity = 4;
            this.size = 0;
            this.buckets = new Bucket[4];
        }

        //

        @Override
        public Set<Entry<AbstractElement<?>, Integer>> entrySet() {
            return new Entries(this);
        }

        @Override
        public int size() {
            final Lock lock = this.lock.readLock();
            lock.lock();
            try {
                return this.size;
            } finally {
                lock.unlock();
            }
        }

        public @Range(from = -1, to = Integer.MAX_VALUE) int getInt(AbstractElement<?> element) {
            final Lock lock = this.lock.readLock();
            lock.lock();
            try {
                int h = Integer.remainderUnsigned(System.identityHashCode(element), this.capacity);
                Bucket head = this.buckets[h];
                while (head != null) {
                    if (head.key.refersTo(element)) return head.value;
                    head = head.next;
                }
                return -1;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public @Nullable Integer get(Object key) {
            if (!(key instanceof AbstractElement<?> element)) return null;
            int value = this.getInt(element);
            return value == -1 ? null : value;
        }

        public @Range(from = -1, to = Integer.MAX_VALUE) int putInt(
                AbstractElement<?> element,
                @Range(from = 0, to = Integer.MAX_VALUE) int index
        ) {
            final Lock lock = this.lock.writeLock();
            lock.lock();
            try {
                int h = Integer.remainderUnsigned(System.identityHashCode(element), this.capacity);
                Bucket head = this.buckets[h];

                Bucket tail = head;
                while (tail != null) {
                    if (tail.key.refersTo(element)) return tail.value;
                    tail = tail.next;
                }

                Bucket newBucket = new Bucket(element, index, head);
                this.buckets[h] = newBucket;
                this.size++;
                this.tryGrow();
                return -1;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public @Nullable Integer put(AbstractElement<?> element, Integer value) {
            Objects.requireNonNull(value, "value must not be null");
            int ret = this.putInt(element, value);
            return ret == -1 ? null : ret;
        }

        @Override
        public @Nullable Integer remove(Object key) {
            if (!(key instanceof AbstractElement<?> element)) return null;
            int value = this.removeElement(element);
            return value == -1 ? null : value;
        }

        public @Range(from = -1, to = Integer.MAX_VALUE) int removeElement(AbstractElement<?> element) {
            final Lock lock = this.lock.writeLock();
            lock.lock();
            try {
                int h = Integer.remainderUnsigned(System.identityHashCode(element), this.capacity);
                Bucket head = this.buckets[h];
                if (head == null) return -1;
                if (head.key.refersTo(element)) {
                    this.buckets[h] = head.next;
                    this.size--;
                    this.tryShrink();
                    return head.value;
                }
                Bucket next;
                while ((next = head.next) != null) {
                    if (next.key.refersTo(element)) {
                        head.next = next.next;
                        this.size--;
                        this.tryShrink();
                        return head.value;
                    }
                    head = next;
                }
                return -1;
            } finally {
                lock.unlock();
            }
        }

        /** REQUIRES WRITE LOCK */
        private void tryGrow() {
            int size = this.size;
            int capacity = this.capacity;
            int threshold = (3 * capacity) / 4;
            if (size < threshold) return;
            if (capacity == 0x7FFFFFFF) return;
            capacity = capacity >= 0x40000000 ? 0x7FFFFFFF : capacity << 1;
            this.rekey(capacity);
        }

        /** REQUIRES WRITE LOCK */
        private void tryShrink() {
            int size = this.size;
            int capacity = this.capacity;
            int hc = capacity >> 1;
            int threshold = (3 * hc) / 4;
            if (size >= threshold) return;
            this.rekey(hc);
        }

        /** REQUIRES WRITE LOCK */
        private void rekey(int nc) {
            Bucket[] newBuckets = new Bucket[nc];
            int newSize = 0;

            for (int i = 0; i < this.capacity; i++) {
                Bucket head = this.buckets[i];
                while (head != null) {
                    AbstractElement<?> value = head.key.get();
                    if (value == null) {
                        head = head.next;
                        continue;
                    }
                    int h = Integer.remainderUnsigned(System.identityHashCode(value), nc);
                    final Bucket tmp = head.next;
                    head.next = newBuckets[h];
                    newBuckets[h] = head;
                    newSize++;
                    head = tmp;
                }
            }

            this.capacity = nc;
            this.size = newSize;
            this.buckets = newBuckets;
        }

        //

        private static final class Bucket {

            final WeakReference<AbstractElement<?>> key;
            final int value;
            @Nullable Bucket next;

            Bucket(AbstractElement<?> key, int value, @Nullable Bucket next) {
                this.key = new WeakReference<>(key, null);
                this.value = value;
                this.next = next;
            }

        }

        private static final class Entries extends AbstractSet<Map.Entry<AbstractElement<?>, Integer>> {

            private final ParentMap parent;

            Entries(ParentMap parent) {
                this.parent = parent;
            }

            //

            @Override
            public int size() {
                return this.parent.size;
            }

            @Override
            public Iterator<Entry<AbstractElement<?>, Integer>> iterator() {
                return new Iter(this.parent);
            }

            //

            private static final class Iter implements Iterator<Map.Entry<AbstractElement<?>, Integer>> {

                private final ParentMap parent;
                private int head;
                private @Nullable Bucket bucket;
                private Map.@Nullable Entry<AbstractElement<?>, Integer> value;
                private @Nullable AbstractElement<?> removable;

                Iter(ParentMap parent) {
                    this.parent = parent;
                    this.head = 0;
                    this.value = this.advance();
                }

                //

                @Override
                public boolean hasNext() {
                    return this.value != null;
                }

                @Override
                public Map.Entry<AbstractElement<?>, Integer> next() {
                    Map.Entry<AbstractElement<?>, Integer> ret = this.value;
                    if (ret == null) throw new NoSuchElementException();
                    this.value = this.advance();
                    this.removable = ret.getKey();
                    return ret;
                }

                @Override
                public void remove() {
                    AbstractElement<?> removable = this.removable;
                    if (removable == null) throw new IllegalStateException();
                    this.removable = null;
                    this.parent.remove(removable);
                }

                private Map.@Nullable Entry<AbstractElement<?>, Integer> advance() {
                    int n = this.head;
                    Bucket bucket = this.bucket;
                    Lock lock = this.parent.lock.readLock();
                    lock.lock();
                    try {
                        while (true) {
                            while (bucket != null) {
                                AbstractElement<?> k = bucket.key.get();
                                int v = bucket.value;
                                bucket = bucket.next;
                                if (k == null) continue;
                                this.head = n;
                                this.bucket = bucket;
                                return Map.entry(k, v);
                            }
                            if (n >= this.parent.capacity) break;
                            bucket = this.parent.buckets[n++];
                        }
                        return null;
                    } finally {
                        lock.unlock();
                    }
                }

            }

        }

    }

    private static final class ChildrenView<C extends Node>
            extends AbstractList<C>
            implements RandomAccess
    {

        private final AbstractElement<C> parent;

        ChildrenView(AbstractElement<C> parent) {
            this.parent = parent;
        }

        //

        @Override
        public int size() {
            final Lock lock = this.parent.lock.readLock();
            lock.lock();
            try {
                return this.parent.directChildren.size();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public C get(int index) {
            final Lock lock = this.parent.lock.readLock();
            lock.lock();
            try {
                return this.parent.directChildren.get(index);
            } finally {
                lock.unlock();
            }
        }

    }

}
