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

package moe.hendeca.minidialog.util;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

@NullMarked
@ApiStatus.Internal
public final class LimitedTagResolverFactory {

    private static final Set<String> EXCLUSIONS = Set.of("defaults", "nbt", "score", "selector");
    private static @Nullable TagResolver INSTANCE = null;

    public static synchronized TagResolver limitedTagResolver() {
        TagResolver ret = INSTANCE;
        if (ret == null) INSTANCE = ret = create();
        return ret;
    }

    private static TagResolver create() {
        Method[] methods = StandardTags.class.getDeclaredMethods();
        TagResolver[] buf = new TagResolver[methods.length];
        int head = 0;

        for (Method method : methods) {
            if ((method.getModifiers() & 0x9) != 0x9) continue; // public static
            if (!TagResolver.class.isAssignableFrom(method.getReturnType())) continue;
            if (method.getParameterCount() != 0) continue;
            if (EXCLUSIONS.contains(method.getName())) continue;
            try {
                buf[head++] = (TagResolver) method.invoke(null);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause == null) cause = e;
                if (cause instanceof RuntimeException re) throw re;
                if (cause instanceof Error re) throw re;
                throw new IllegalStateException("TagResolver accessor raised a checked exception", cause);
            } catch (Exception e) {
                throw new IllegalStateException("Disallowed reflection error", e);
            }
        }

        return TagResolver.resolver(Arrays.asList(buf)
                .subList(0, head));
    }

    //

    private LimitedTagResolverFactory() { }

}
