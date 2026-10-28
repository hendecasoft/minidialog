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

package moe.hendeca.minidialog.render;

import moe.hendeca.minidialog.element.*;
import moe.hendeca.minidialog.except.DialogRenderException;
import moe.hendeca.minidialog.node.Node;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.function.IntConsumer;

@NullMarked
@ApiStatus.Internal
public abstract class AbstractDialogRenderer<T> implements DialogRenderer<T> {

    protected enum Schema {
        NOTICE,
        CONFIRMATION,
        MULTI_ACTION;

        public static Schema match(Flattener.DialogElements elements) throws DialogRenderException {
            int footerSize = elements.footer != null ? elements.footer.children().size() : 0;
            int actionsSize = elements.actions != null ? elements.actions.children().size() : 0;
            if (footerSize > 2) throw new DialogRenderException("Footer element contains more than 2 children");
            if (footerSize == 2) {
                if (actionsSize != 0)
                    throw new DialogRenderException("Cannot create confirmation dialog with non-zero amount of actions");
                return Schema.CONFIRMATION;
            }
            return actionsSize == 0 ?
                    Schema.NOTICE :
                    Schema.MULTI_ACTION;
        }

    }

    protected static final class Flattener<I extends Element, O extends Record> {

        public static final Flattener<DialogElement, DialogElements> DIALOG
                = new Flattener<>(DialogElements.class);

        public static final Flattener<ItemElement, ItemElements> ITEM
                = new Flattener<>(ItemElements.class);

        public static final Flattener<InputElement, ? extends InputElements> INPUT
                = new Flattener<>(BasicInputElements.class);

        public static final Flattener<TextInputElement, TextInputElements> TEXT_INPUT
                = new Flattener<>(TextInputElements.class);

        public static final Flattener<OptionInputElement, OptionInputElements> OPTION_INPUT
                = new Flattener<>(OptionInputElements.class);

        public static final Flattener<ActionElement, ? extends ActionElements> ACTION
                = new Flattener<>(BasicActionElements.class);

        public static final Flattener<ShowDialogActionElement, ShowDialogActionElements> SHOW_DIALOG_ACTION
                = new Flattener<>(ShowDialogActionElements.class);

        //

        private final Constructor<O> ctor;
        private final Class<?>[] nodeTypes;
        private final StampedLock lutLock;
        private final Map<Class<?>, Integer> lut;
        private final int count;
        private final long listMask;
        private final long requiredMask;

        private Flattener(Class<O> clazz) {
            RecordComponent[] components = clazz.getRecordComponents();
            if (components == null) throw new IllegalStateException("Class " + clazz.getName() + " is not a record");

            int count = components.length;
            if (count > 64) throw new IllegalStateException("Too many components (" + components.length + ")");

            Map<Class<?>, Integer> lut = HashMap.newHashMap(count);
            Class<?>[] nodeTypes = new Class<?>[count];
            Class<?>[] rawTypes = new Class<?>[count];
            long listMask = 0L;
            long requiredMask = 0L;
            for (int i = 0; i < count; i++) {
                RecordComponent component = components[i];
                Type t = component.getGenericType();
                if (t instanceof Class<?> cls && Node.class.isAssignableFrom(cls)) {
                    nodeTypes[i] = cls;
                    rawTypes[i] = cls;
                    lut.put(cls, i);
                    if (!component.getAccessor().getAnnotatedReturnType().isAnnotationPresent(Nullable.class)) {
                        requiredMask |= (1L << (long) i);
                    }
                } else if (t instanceof ParameterizedType pt && List.class.equals(pt.getRawType())) {
                    Type arg = pt.getActualTypeArguments()[0];
                    if (arg instanceof Class<?> cls && Node.class.isAssignableFrom(cls)) {
                        nodeTypes[i] = cls;
                        rawTypes[i] = List.class;
                        lut.put(cls, i);
                        listMask |= (1L << (long) i);
                        continue;
                    }
                    throw new IllegalStateException("Element type of component \"" + component.getName() +
                            "\" is not Node (is " + arg + ")");
                } else {
                    throw new IllegalStateException("Type of component \"" + component.getName() +
                            "\" is not Node or List<Node> (is " + t + ")");
                }
            }

            Constructor<O> ctor;
            try {
                ctor = clazz.getDeclaredConstructor(rawTypes);
                ctor.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Failed to get primary constructor of class " + clazz.getName(), e);
            } catch (InaccessibleObjectException e) {
                throw new IllegalStateException("Failed to make primary constructor of class " +
                        clazz.getName() + " accessible", e);
            }

            this.ctor = ctor;
            this.nodeTypes = nodeTypes;
            this.lutLock = new StampedLock();
            this.lut = lut;
            this.count = count;
            this.listMask = listMask;
            this.requiredMask = requiredMask;
        }

        //

        @SuppressWarnings("unchecked")
        public O flatten(I element) {
            final List<? extends Node> children = element.children();
            final int childCount = children.size();

            @Nullable Object[] values = new Object[this.count];
            for (Node child : children) {
                int i = this.indexOf(child.getClass());
                if (i == -1) continue;
                long f = 1L << (long) i;
                if ((this.listMask & f) != 0L) {
                    List<Object> list = (List<Object>) values[i];
                    if (list == null) values[i] = list = new ArrayList<>(childCount);
                    list.add(child);
                } else {
                    if (values[i] != null) {
                        throw new DialogRenderException(element.name() + " element has multiple " +
                                this.nodeTypes[i].getSimpleName() + " children");
                    }
                    values[i] = child;
                }
            }

            decompose(this.requiredMask, (int i) -> {
                if (values[i] != null) return;
                throw new DialogRenderException(element.name() + " element has no " +
                        this.nodeTypes[i].getSimpleName() + " children");
            });

            decompose(this.listMask, (int i) -> {
                List<Object> list = (List<Object>) values[i];
                values[i] = list == null ? Collections.emptyList() :
                        Collections.unmodifiableList(list);
            });

            try {
                return this.ctor.newInstance(values);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause == null) cause = e;
                if (cause instanceof RuntimeException re) throw re;
                if (cause instanceof Error re) throw re;
                throw new IllegalStateException("Record constructor raised a checked exception", e);
            } catch (Exception e) {
                throw new IllegalStateException("Forbidden reflection error", e);
            }
        }

        private int indexOf(Class<? extends Node> nodeType) {
            long stamp = this.lutLock.readLock();
            try {
                Integer stored = this.lut.get(nodeType);
                if (stored != null) return stored;

                long s2 = this.lutLock.tryConvertToWriteLock(stamp);
                if (s2 == 0L) {
                    this.lutLock.unlock(stamp);
                    stamp = this.lutLock.writeLock();
                } else {
                    stamp = s2;
                }

                for (int i = 0; i < this.count; i++) {
                    if (this.nodeTypes[i].isAssignableFrom(nodeType)) {
                        this.lut.put(nodeType, i);
                        return i;
                    }
                }

                return -1;
            } finally {
                this.lutLock.unlock(stamp);
            }
        }

        private static void decompose(long m, IntConsumer fn) {
            while (m != 0L) {
                int q = Long.numberOfTrailingZeros(m);
                m ^= (1L << (long) q);
                fn.accept(q);
            }
        }

        //

        public interface InputElements {

            @Contract(pure = true)
            @Nullable LabelElement label();

        }

        public interface ActionElements {

            @Contract(pure = true)
            @Nullable LabelElement label();

            @Contract(pure = true)
            @Nullable TooltipElement tooltip();

        }

        public record DialogElements(
                @Nullable TitleElement title,
                @Nullable ExternalTitleElement externalTitle,
                @Nullable BodyElement body,
                @Nullable InputsElement inputs,
                @Nullable ActionsElement actions,
                @Nullable FooterElement footer
        ) { }

        public record ItemElements(
                @Nullable DescriptionElement description
        ) { }

        private record BasicInputElements(
                @Nullable LabelElement label
        ) implements InputElements { }

        public record TextInputElements(
                @Nullable LabelElement label,
                @Nullable MultilineElement multiline
        ) implements InputElements { }

        public record OptionInputElements(
                @Nullable LabelElement label,
                @Unmodifiable List<OptionElement> options
        ) implements InputElements { }

        private record BasicActionElements(
                @Nullable LabelElement label,
                @Nullable TooltipElement tooltip
        ) implements ActionElements { }

        public record ShowDialogActionElements(
                @Nullable LabelElement label,
                @Nullable TooltipElement tooltip,
                DialogElement dialog
        ) implements ActionElements { }

    }

}
