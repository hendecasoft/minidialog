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

import com.github.retrooper.packetevents.protocol.nbt.*;
import moe.hendeca.minidialog.PacketEventsDialogUser;
import moe.hendeca.minidialog.TokenizedDialogKeyFactory;
import moe.hendeca.minidialog.element.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@ApiStatus.Internal
public abstract class PacketEventsDialogResponse<U extends PacketEventsDialogUser<?>>
    extends AbstractDialogResponse<NBT, U>
{

    protected PacketEventsDialogResponse(
            U user,
            @Nullable NBT payload,
            DialogElement document,
            TokenizedDialogKeyFactory keyFactory
    ) {
        super(payload, user, document, keyFactory);
    }

    //

    @Override
    public @Nullable NBT handle() {
        return super.handle();
    }

    @Override
    public @UnknownNullability String value(TextInputElement element) {
        NBT raw = this.rawValue(element);
        if (raw == null || !NBTType.STRING.equals(raw.getType())) return null;
        return ((NBTString) raw).getValue();
    }

    @Override
    public @UnknownNullability OptionElement value(OptionInputElement element) {
        NBT raw = this.rawValue(element);
        if (raw == null || !NBTType.STRING.equals(raw.getType())) return null;
        return this.findOption(element, ((NBTString) raw).getValue());
    }

    @Override
    public @UnknownNullability Float value(RangeInputElement element) {
        return switch (this.rawValue(element)) {
            case NBTNumber n -> n.getAsFloat();
            case null, default -> null;
        };
    }

    @Override
    public @UnknownNullability Boolean value(BooleanInputElement element) {
        return switch (this.rawValue(element)) {
            case NBTNumber n -> 0 != n.getAsByte();
            case NBTString s -> !"false".equals(s.getValue());
            case null, default -> null;
        };
    }

    private @Nullable NBT rawValue(InputElement element) {
        if (!(this.handle instanceof NBTCompound compound)) return null;
        String key = this.keyOf(element);
        if (key == null) return null;
        return compound.getTagOrNull(key);
    }

}
