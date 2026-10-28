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

import java.io.Serial;

@NullMarked
public final class DialogRenderException extends DialogException {

    @Serial
    private static final long serialVersionUID = -5792167024901189294L;

    //

    public DialogRenderException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    public DialogRenderException(String message) {
        this(message, null);
    }

    //

    @Override
    public String getMessage() {
        return super.getMessage();
    }

}
