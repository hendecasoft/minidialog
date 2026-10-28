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

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public final class PaperDialogUser extends AbstractDialogUser<Player> {

    PaperDialogUser(Player player) {
        super(player);
    }

    //

    @Override
    public UUID id() {
        return this.handle.getUniqueId();
    }

    @Override
    public String name() {
        return this.handle.getName();
    }

    @Override
    public Component displayName() {
        return this.handle.displayName();
    }

    @Override
    public Audience audience() {
        return this.handle;
    }

}
