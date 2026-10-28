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

package moe.hendeca.minidialog.manager;

import moe.hendeca.minidialog.DialogUser;
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.element.Element;
import moe.hendeca.minidialog.except.DialogBindException;
import moe.hendeca.minidialog.response.DialogResponse;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@NullMarked
@ApiStatus.Internal
public final class DialogBindings {

    private static final DialogBindableType<?>[] BINDABLE_TYPES;
    private static final Map<Class<?>, DialogBindableType<?>> BINDABLE_TYPE_LUT;
    private static final StampedLock BINDABLE_TYPE_LUT_LOCK;
    static {
        BINDABLE_TYPES = DialogBindableType.values();
        BINDABLE_TYPE_LUT = new HashMap<>();
        BINDABLE_TYPE_LUT_LOCK = new StampedLock();
        for (DialogBindableType<?> type : BINDABLE_TYPES)
            BINDABLE_TYPE_LUT.put(type.typeClass(), type);
    }

    private static DialogBindableType<?> resolveBindableType(Class<?> clazz) throws IllegalArgumentException {
        long stamp = BINDABLE_TYPE_LUT_LOCK.readLock();
        try {
            DialogBindableType<?> stored = BINDABLE_TYPE_LUT.get(clazz);
            if (stored != null) return stored;

            long s2 = BINDABLE_TYPE_LUT_LOCK.tryConvertToWriteLock(stamp);
            if (s2 == 0L) {
                BINDABLE_TYPE_LUT_LOCK.unlock(stamp);
                stamp = BINDABLE_TYPE_LUT_LOCK.writeLock();
                stored = BINDABLE_TYPE_LUT.get(clazz);
                if (stored != null) return stored;
            } else {
                stamp = s2;
            }

            List<DialogBindableType<?>> matching = new LinkedList<>();
            for (DialogBindableType<?> candidate : BINDABLE_TYPES) {
                if (candidate.typeClass().isAssignableFrom(clazz)) matching.add(candidate);
            }

            Iterator<DialogBindableType<?>> iter = matching.iterator();
            if (!iter.hasNext()) {
                throw new IllegalArgumentException("Class " + clazz.getName() + " matches no bindable types (" +
                        StreamSupport.stream(Arrays.spliterator(BINDABLE_TYPES), false)
                                .map(DialogBindableType::name)
                                .collect(Collectors.joining(", ")) + ")"
                );
            }

            DialogBindableType<?> ret = iter.next();
            if (iter.hasNext()) {
                throw new IllegalArgumentException("Class " + clazz.getName() + " matches multiple bindable types (" +
                        matching.stream()
                                .map(DialogBindableType::name)
                                .collect(Collectors.joining(", ")) + ")"
                );
            }

            BINDABLE_TYPE_LUT.put(clazz, ret);
            return ret;
        } finally {
            BINDABLE_TYPE_LUT_LOCK.unlock(stamp);
        }
    }

    //

    private final DialogManager target;
    private final @Nullable Field[] contextFields;
    private final Map<String, ElementFieldData> elementFields;
    private final Map<SingleCallbackType, Method> singleCallbackMethods;
    private final Map<String, Method> customActionMethods;
    private final long thinkPeriod;

    public DialogBindings(DialogManager target) throws DialogBindException {
        // This is a monster but whatever
        @Nullable Field[] contextFields = new Field[BINDABLE_TYPES.length];
        Map<String, ElementFieldData> elementFields = new HashMap<>();
        Map<String, Method> customActionMethods = new HashMap<>();
        Map<SingleCallbackType, Method> singleCallbackMethods = new EnumMap<>(SingleCallbackType.class);
        long thinkPeriod = 0L;

        Queue<Class<?>> classQueue = new LinkedList<>();
        classQueue.add(target.getClass());
        Class<?> next;
        while ((next = classQueue.poll()) != null) {
            for (Field field : next.getDeclaredFields()) {
                if ((field.getModifiers() & 0x88) != 0) continue; // static or transient
                ParsedAnnotation.ForField annotation = ParsedAnnotation.fromField(field);
                if (annotation == null) continue;
                try {
                    field.setAccessible(true);
                } catch (InaccessibleObjectException e) {
                    throw new DialogBindException("Cannot make field \"" + field.getName() + "\" in class " +
                            next.getName() + " accessible", e);
                }
                switch (annotation) {
                    case ParsedAnnotation.Context _ -> {
                        DialogBindableType<?> bindableType;
                        try {
                            bindableType = resolveBindableType(field.getType());
                        } catch (IllegalArgumentException e) {
                            throw new DialogBindException("Type of field \"" + field.getName() + "\" in class " +
                                    next.getName() + " is not bindable", e);
                        }
                        Field existing = contextFields[bindableType.ordinal()];
                        if (existing != null) {
                            throw new DialogBindException("Multiple fields bound to " + bindableType.name() +
                                    " target (" + existing.getDeclaringClass().getName() + "#" + existing.getName() +
                                    " and " + field.getDeclaringClass().getName() + "#" + field.getName() + ")");
                        }
                        contextFields[bindableType.ordinal()] = field;
                    }
                    case ParsedAnnotation.Element el -> {
                        ElementFieldData existing = elementFields.put(el.id, new ElementFieldData(field, el.optional));
                        if (existing != null) {
                            throw new DialogBindException("Multiple fields bound to \"" + el.id +
                                    "\" element (" + existing.field.getDeclaringClass().getName() +
                                    "#" + existing.field.getName() +
                                    " and " + field.getDeclaringClass().getName() + "#" + field.getName() + ")");
                        }
                    }
                }
            }

            for (Method method : next.getDeclaredMethods()) {
                if ((method.getModifiers() & 0x80) != 0) continue; // static
                ParsedAnnotation.ForMethod annotation = ParsedAnnotation.fromMethod(method);
                if (annotation == null) continue;
                try {
                    method.setAccessible(true);
                } catch (InaccessibleObjectException e) {
                    throw new DialogBindException("Cannot make method \"" + method.getName() + "\" in class " +
                            next.getName() + " accessible", e);
                }
                if (!Void.TYPE.equals(method.getReturnType())) {
                    throw new DialogBindException("Bound method \"" + method.getName() + "\" in class " +
                            next.getName() + " must return void (returns " + method.getReturnType().getName() + ")");
                }
                Class<?> paramType = annotation.parameterType();
                if (paramType == null) {
                    if (method.getParameterCount() != 0) {
                        throw new DialogBindException("Bound method \"" + method.getName() + "\" in class " +
                                next.getName() + " may not have any parameters (has " +
                                method.getParameterCount() + ")");
                    }
                } else {
                    Class<?>[] types = method.getParameterTypes();
                    if (types.length > 1) {
                        throw new DialogBindException("Bound method \"" + method.getName() + "\" in class " +
                                next.getName() + " may have at most 1 parameter (has " +
                                types.length + ")");
                    }
                    if (types.length != 0 && !paramType.isAssignableFrom(types[0])) {
                        throw new DialogBindException("Bound method \"" + method.getName() + "\" in class " +
                                next.getName() + " has single parameter that is not of type " + paramType.getName() +
                                " (is " + types[0].getName() + ")");
                    }
                }
                if (method.getParameterCount() > 1) {
                    throw new DialogBindException("Bound method \"" + method.getName() + "\" in class " +
                            next.getName() + " should have at most 1 parameter (has " +
                            method.getParameterCount() + ")");
                }
                SingleCallbackType singleType = switch (annotation) {
                    case ParsedAnnotation.Init _ -> SingleCallbackType.INIT;
                    case ParsedAnnotation.Open _ -> SingleCallbackType.OPEN;
                    case ParsedAnnotation.Think t -> {
                        if (t.period < 1L) {
                            throw new DialogBindException("Think period on method " +
                                    next.getName() + "#" + method.getName() +
                                    " must have a period of at least 1 (is " +
                                    t.period + ")");
                        }
                        thinkPeriod = t.period;
                        yield SingleCallbackType.THINK;
                    }
                    case ParsedAnnotation.CustomAction t -> {
                        Method existing = customActionMethods.put(t.id, method);
                        if (existing != null) {
                            throw new DialogBindException("Multiple methods bound to \"" + t.id +
                                    "\" action (" + existing.getDeclaringClass().getName() + "#" + existing.getName() +
                                    " and " + method.getDeclaringClass().getName() + "#" + method.getName() + ")");
                        }
                        yield null;
                    }
                };
                if (singleType == null) continue;
                Method existing = singleCallbackMethods.put(singleType, method);
                if (existing != null) {
                    throw new DialogBindException("Multiple methods bound to " + singleType.name() +
                            " callback (" + existing.getDeclaringClass().getName() + "#" + existing.getName() +
                            " and " + method.getDeclaringClass().getName() + "#" + method.getName() + ")");
                }
            }

            Class<?> superClass = next.getSuperclass();
            if (superClass != null && DialogManager.class.isAssignableFrom(superClass))
                classQueue.add(superClass);

            for (Class<?> iface : next.getInterfaces()) {
                if (DialogManager.class.isAssignableFrom(iface))
                    classQueue.add(iface);
            }
        }

        this.target = target;
        this.contextFields = contextFields;
        this.elementFields = elementFields;
        this.singleCallbackMethods = singleCallbackMethods;
        this.customActionMethods = customActionMethods;
        this.thinkPeriod = thinkPeriod;
    }

    //

    @Contract(pure = true)
    public DialogManager target() {
        return this.target;
    }

    /** @return {@code 0} if no think callback is registered */
    @Contract(pure = true)
    public @Range(from = 0L, to = Long.MAX_VALUE) long thinkPeriod() {
        return this.thinkPeriod;
    }

    public boolean isThinkTick(long tick) {
        return this.thinkPeriod != 0L &&
                (tick % this.thinkPeriod) == 0L;
    }

    public <T> void injectContext(DialogBindableType<T> type, T value) throws DialogBindException {
        Field field = this.contextFields[type.ordinal()];
        if (field == null) return;

        Class<?> fieldType = field.getType();
        if (!fieldType.isInstance(value)) {
            throw new DialogBindException("Context value " + type.name() +
                    " is of type " + type.typeClass().getSimpleName() + " but not of type " +
                    fieldType.getName() + " which is the type of receiving field " +
                    field.getDeclaringClass().getName() + "#" + field.getName());
        }

        try {
            field.set(this.target, value);
        } catch (IllegalAccessException e) {
            throw new DialogBindException("Cannot set value of field " +
                    field.getDeclaringClass().getName() + "#" + field.getName() +
                    " to target " + type.name(), e);
        }
    }

    public void injectElements(DialogElement document) throws DialogBindException {
        for (Map.Entry<String, ElementFieldData> entry : this.elementFields.entrySet()) {
            String id = entry.getKey();
            Field field = entry.getValue().field;
            Element element = document.find(id);
            if (element == null) {
                if (entry.getValue().optional) continue;
                throw new DialogBindException("No element with ID \"" +
                        id + "\" exists in the document (injecting " +
                        field.getDeclaringClass().getName() + "#" + field.getName() +
                        ")");
            }

            Class<?> fieldType = field.getType();
            if (!fieldType.isInstance(element)) {
                throw new DialogBindException("Element with ID \"" + id +
                        "\" is not of type " + fieldType.getName() +
                        " which is the type of receiving field " +
                        field.getDeclaringClass().getName() + "#" + field.getName());
            }

            try {
                field.set(this.target, element);
            } catch (IllegalAccessException e) {
                throw new DialogBindException("Cannot set value of field " +
                        field.getDeclaringClass().getName() + "#" + field.getName() +
                        " to element with ID \"" + id + "\"", e);
            }
        }
    }

    public void runInit() throws DialogBindException {
        Method method = this.singleCallbackMethods.get(SingleCallbackType.INIT);
        if (method == null) return;
        this.dispatch(method, null);
    }

    public void runOpen(DialogUser user) throws DialogBindException {
        Method method = this.singleCallbackMethods.get(SingleCallbackType.OPEN);
        if (method == null) return;
        this.dispatch(method, user);
    }

    public void runThink() throws DialogBindException {
        Method method = this.singleCallbackMethods.get(SingleCallbackType.THINK);
        if (method == null) return;
        this.dispatch(method, null);
    }

    public void runCustomAction(String id, DialogResponse response) throws DialogBindException {
        Method method = this.customActionMethods.get(id);
        if (method == null) return;
        this.dispatch(method, response);
    }

    private void dispatch(Method method, @Nullable Object arg0) throws DialogBindException {
        Object[] args;
        if (method.getParameterCount() == 0) {
            args = new Object[0];
        } else {
            assert arg0 != null;
            Class<?> constraint = method.getParameters()[0].getType();
            if (!constraint.isInstance(arg0)) {
                throw new DialogBindException("Cannot dispatch " + method.getDeclaringClass().getName() + "#" + method.getName() +
                        " with argument of type " + arg0.getClass().getName() + " (requires " + constraint.getName() + ")");
            }
            args = new Object[] { arg0 };
        }
        try {
            method.invoke(this.target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause == null) cause = e;
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error re) throw re;
            throw new DialogBindException("Bound method " + method.getDeclaringClass().getName() +
                    "#" + method.getName() + " raised a checked exception", cause);
        } catch (IllegalAccessException e) {
            throw new DialogBindException("Bound method " + method.getDeclaringClass().getName() +
                    "#" + method.getName() + " is not accessible", e);
        }
    }

    //

    private enum SingleCallbackType {
        INIT,
        OPEN,
        THINK
    }

    private record ElementFieldData(
            Field field,
            boolean optional
    ) { }

    private sealed interface ParsedAnnotation {

        static @Nullable ForField fromField(Field field) throws DialogBindException {
            ParsedAnnotation ret = from(field);
            if (ret == null) return null;
            if (ret instanceof ForField qual) return qual;
            throw new DialogBindException(
                    "Annotation " + ret.getClass().getSimpleName() +
                            " is not applicable for fields (present on " + field.getDeclaringClass().getName() +
                            "#" + field.getName() + ")"
            );
        }

        static @Nullable ForMethod fromMethod(Method method) throws DialogBindException {
            ParsedAnnotation ret = from(method);
            if (ret == null) return null;
            if (ret instanceof ForMethod qual) return qual;
            throw new DialogBindException(
                    "Annotation " + ret.getClass().getSimpleName() +
                            " is not applicable for fields (present on " + method.getDeclaringClass().getName() +
                            "#" + method.getName() + ")"
            );
        }

        static <M extends Member & AnnotatedElement> @Nullable ParsedAnnotation from(M member) throws DialogBindException {
            List<ParsedAnnotation> applicable = new LinkedList<>();
            for (Annotation annotation : member.getAnnotations()) {
                ParsedAnnotation parsed = of(annotation);
                if (parsed == null) continue;
                applicable.add(parsed);
            }

            Iterator<ParsedAnnotation> iter = applicable.iterator();
            if (!iter.hasNext()) return null;
            ParsedAnnotation ret = iter.next();
            if (iter.hasNext()) {
                throw new DialogBindException("Member " + member.getName() + " in class " +
                        member.getDeclaringClass().getSimpleName() + " has multiple DialogBind annotations (" +
                        applicable.stream().map((ParsedAnnotation a) -> a.getClass().getSimpleName())
                                .collect(Collectors.joining(", "))
                );
            }
            return ret;
        }

        static @Nullable ParsedAnnotation of(Annotation annotation) {
            Class<?> parentType = annotation.annotationType().getDeclaringClass();
            if (!DialogBind.class.equals(parentType)) return null;
            return switch (annotation) {
                case DialogBind.Context _ -> Context.INSTANCE;
                case DialogBind.Element q -> new Element(q.value(), q.optional());
                case DialogBind.Init _ -> Init.INSTANCE;
                case DialogBind.Open _ -> Open.INSTANCE;
                case DialogBind.Think q -> new Think(q.value());
                case DialogBind.CustomAction q -> new CustomAction(q.value());
                default -> throw new IllegalStateException("Unhandled DialogBind annotation: " +
                        annotation.annotationType().getSimpleName());
            };
        }

        //

        sealed interface ForField
                extends ParsedAnnotation
                permits Context, Element
        { }

        final class Context implements ForField {
            public static final Context INSTANCE = new Context();
            private Context() { }
        }

        record Element(
                String id,
                boolean optional
        ) implements ForField { }

        //

        sealed interface ForMethod
                extends ParsedAnnotation
                permits Init, Open, Think, CustomAction
        {
            @Nullable Class<?> parameterType();
        }

        final class Init implements ForMethod {
            public static final Init INSTANCE = new Init();
            private Init() { }

            @Override
            @Contract("-> null")
            public @Nullable Class<?> parameterType() {
                return null;
            }
        }

        final class Open implements ForMethod {
            public static final Open INSTANCE = new Open();
            private Open() { }

            @Override
            public Class<?> parameterType() {
                return DialogUser.class;
            }
        }

        record Think(
                long period
        ) implements ForMethod {
            @Override
            @Contract("-> null")
            public @Nullable Class<?> parameterType() {
                return null;
            }
        }

        record CustomAction(
                String id
        ) implements ForMethod {
            @Override
            public Class<?> parameterType() {
                return DialogResponse.class;
            }
        }

    }

}
