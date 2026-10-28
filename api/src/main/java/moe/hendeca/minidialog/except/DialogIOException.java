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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Serial;
import java.util.Objects;

@NullMarked
public final class DialogIOException extends DialogException {

    @Serial
    private static final long serialVersionUID = -2992693501013449231L;

    public static final String DEFAULT_MESSAGE = "Underlying data source raised an exception";

    //

    public DialogIOException(@Nullable String message, IOException cause) {
        super(
                message == null ? DEFAULT_MESSAGE : message,
                Objects.requireNonNull(cause, "cause must not be null")
        );
    }

    public DialogIOException(IOException cause) {
        this(null, cause);
    }

    //


    @Override
    public String getMessage() {
        return super.getMessage();
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }

}
