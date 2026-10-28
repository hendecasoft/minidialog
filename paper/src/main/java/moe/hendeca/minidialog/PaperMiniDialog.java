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

import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.manager.DialogManager;
import moe.hendeca.minidialog.render.PaperDialogRenderer;
import moe.hendeca.minidialog.util.ParamCasts;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@NullMarked
public final class PaperMiniDialog extends AbstractMiniDialog<PaperDialog> {

    @Contract("_ -> new")
    public static PaperMiniDialog miniDialog(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        if (!plugin.isEnabled()) throw new IllegalArgumentException("plugin must be enabled");
        return new PaperMiniDialog(plugin);
    }

    //

    private final Plugin plugin;

    private PaperMiniDialog(Plugin plugin) {
        this.plugin = plugin;
    }

    //

    @Contract(pure = true)
    public Plugin plugin() {
        return this.plugin;
    }

    @Override
    public PaperDialogRenderer renderer() {
        return PaperDialogRenderer.INSTANCE;
    }

    @Override
    public PaperDialogUser user(@ParamCasts(Player.class) Object handle) {
        if (!(handle instanceof Player p))
            throw new IllegalArgumentException("Handle is not a Player (got " + handle + ")");
        return new PaperDialogUser(p);
    }

    @Override
    protected PaperDialog newDialog(DialogElement document, @Nullable DialogManager manager) {
        return new PaperDialog(this, document, manager);
    }

}
