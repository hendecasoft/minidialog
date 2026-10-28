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

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

@NullMarked
@ApiStatus.Internal
final class DialogBindableTypeImpl<T>
        implements DialogBindableType<T>
{

    private static final DialogBindableTypeImpl<?>[] REGISTRY = new DialogBindableTypeImpl[DialogBindableType.class.getDeclaredFields().length];
    private static int REGISTRY_HEAD = 0;

    @Contract("_, _ -> new")
    static synchronized <R> DialogBindableTypeImpl<R> register(
            String name,
            Class<R> typeClass
    ) {
        final int ordinal = REGISTRY_HEAD++;
        DialogBindableTypeImpl<R> ret = new DialogBindableTypeImpl<>(ordinal, name, typeClass);
        REGISTRY[ordinal] = ret;
        return ret;
    }

    @Contract("-> new")
    static synchronized DialogBindableTypeImpl<?>[] values() {
        int count = REGISTRY_HEAD;
        DialogBindableTypeImpl<?>[] ret = new DialogBindableTypeImpl<?>[count];
        System.arraycopy(REGISTRY, 0, ret, 0, count);
        return ret;
    }

    //

    private final int ordinal;
    private final String name;
    private final Class<T> typeClass;

    private DialogBindableTypeImpl(
            int ordinal,
            String name,
            Class<T> typeClass
    ) {
        this.ordinal = ordinal;
        this.name = name;
        this.typeClass = typeClass;
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
    public Class<T> typeClass() {
        return this.typeClass;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.ordinal);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DialogBindableType<?> other &&
                this.ordinal == other.ordinal();
    }

    @Override
    public String toString() {
        return this.name;
    }

}
