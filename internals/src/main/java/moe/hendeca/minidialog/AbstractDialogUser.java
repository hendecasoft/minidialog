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

package moe.hendeca.minidialog;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
@ApiStatus.Internal
public abstract class AbstractDialogUser<H> implements DialogUser {

    protected final H handle;

    protected AbstractDialogUser(H handle) {
        this.handle = handle;
    }

    //

    @Override
    public H handle() {
        return this.handle;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.handle);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DialogUser other &&
                Objects.equals(this.handle, other.handle());
    }

}
