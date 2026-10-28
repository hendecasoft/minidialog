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

import java.io.Serial;

@NullMarked
public final class DialogParseException extends DialogException {

    @Serial
    private static final long serialVersionUID = -4137156354258028018L;

    //

    private final String rawMessage;
    private final int line;
    private final int column;

    public DialogParseException(String message, int line, int column) {
        super(message + " @ " + line + ":" + column);
        this.rawMessage = message;
        this.line = line;
        this.column = column;
    }

    //

    @Override
    public String getMessage() {
        return super.getMessage();
    }

    @Contract(pure = true)
    public String rawMessage() {
        return this.rawMessage;
    }

    @Contract(pure = true)
    public int line() {
        return this.line;
    }

    @Contract(pure = true)
    public int column() {
        return this.column;
    }

}
