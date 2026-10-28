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

import moe.hendeca.minidialog.Dialog;
import moe.hendeca.minidialog.MiniDialog;
import moe.hendeca.minidialog.element.DialogElement;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

import static moe.hendeca.minidialog.manager.DialogBindableTypeImpl.register;

@NullMarked
@ApiStatus.Internal
public interface DialogBindableType<T> {

    DialogBindableType<Audience> AUDIENCE = register("AUDIENCE", Audience.class);
    DialogBindableType<DialogManager> MANAGER = register("MANAGER", DialogManager.class);
    DialogBindableType<DialogElement> DOCUMENT = register("DOCUMENT", DialogElement.class);
    DialogBindableType<MiniDialog> INSTANCE = register("INSTANCE", MiniDialog.class);
    DialogBindableType<Dialog> DIALOG = register("DIALOG", Dialog.class);

    @Contract("-> new")
    static DialogBindableType<?>[] values() {
        return DialogBindableTypeImpl.values();
    }

    //

    @Contract(pure = true)
    int ordinal();

    @Contract(pure = true)
    String name();

    @Contract(pure = true)
    Class<T> typeClass();

}
