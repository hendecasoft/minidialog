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

package moe.hendeca.minidialog.except;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public final class DialogNotFoundException extends DialogException {

    private final ClassLoader classLoader;
    private final String path;

    public DialogNotFoundException(ClassLoader classLoader, String path) {
        super("Dialog not found at path " + path + " in class loader " + classLoader);
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader must not be null");
        this.path = Objects.requireNonNull(path, "path must not be null");
    }

    //

    @Override
    public String getMessage() {
        return super.getMessage();
    }

    @Contract(pure = true)
    public ClassLoader classLoader() {
        return this.classLoader;
    }

    @Contract(pure = true)
    public String path() {
        return this.path;
    }

}
