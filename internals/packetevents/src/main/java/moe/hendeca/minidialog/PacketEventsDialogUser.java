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

import com.github.retrooper.packetevents.protocol.player.User;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
@ApiStatus.Internal
public abstract class PacketEventsDialogUser<H>
        extends AbstractDialogUser<H>
{

    protected final User packetEventsUser;

    protected PacketEventsDialogUser(H handle, User packetEventsUser) {
        super(handle);
        this.packetEventsUser = packetEventsUser;
    }

    //

    @Contract(pure = true)
    public User packetEventsUser() {
        return this.packetEventsUser;
    }

    @Override
    public UUID id() {
        return this.packetEventsUser.getUUID();
    }

    @Override
    public String name() {
        return this.packetEventsUser.getName();
    }

}
