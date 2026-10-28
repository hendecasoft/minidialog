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
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class VelocityDialogUser extends PacketEventsDialogUser<Player> {

    private final boolean supportsDialog;

    VelocityDialogUser(
            Player handle,
            User packetEventsUser,
            boolean supportsDialog
    ) {
        super(handle, packetEventsUser);
        this.supportsDialog = supportsDialog;
    }

    //

    public boolean supportsDialog() {
        return this.supportsDialog;
    }

    @Override
    public Component displayName() {
        return Component.text(this.handle.getUsername());
    }

    @Override
    public Audience audience() {
        return this.handle;
    }

}
