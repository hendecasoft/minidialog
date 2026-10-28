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

package moe.hendeca.minidialog.response;

import moe.hendeca.minidialog.DialogUser;
import moe.hendeca.minidialog.element.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;

@NullMarked
@ApiStatus.NonExtendable
public interface DialogResponse {

    @Contract(pure = true)
    DialogUser user();

    @UnknownNullability String value(TextInputElement element);

    @UnknownNullability OptionElement value(OptionInputElement element);

    @UnknownNullability Float value(RangeInputElement element);

    @UnknownNullability Boolean value(BooleanInputElement element);

}
