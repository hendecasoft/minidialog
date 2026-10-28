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

import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.manager.DialogManager;
import moe.hendeca.minidialog.render.DialogRenderContext;
import moe.hendeca.minidialog.response.PaperDialogResponse;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.locks.StampedLock;

@SuppressWarnings("UnstableApiUsage")
@NullMarked
public final class PaperDialog
        extends AbstractDialog<PaperMiniDialog, PaperDialogUser, Dialog>
{

    private static final boolean NOT_FOLIA =
            !ServerBuildInfo.buildInfo().isBrandCompatible(Key.key("papermc", "folia"));

    //

    private final StampedLock wakeLock;
    private boolean awake;
    private @UnknownNullability ScheduledTask updateTask;
    private @UnknownNullability EventPipe eventPipe;

    PaperDialog(
            PaperMiniDialog instance,
            DialogElement document,
            @Nullable DialogManager manager
    ) {
        super(instance, PaperDialogUser.class, document, manager);
        this.wakeLock = new StampedLock();
        this.awake = false;
        this.eventPipe = null;
    }

    //

    @Override
    protected void wake() {
        Plugin plugin = this.instance.plugin();
        long stamp = this.wakeLock.readLock();
        try {
            boolean awake = this.awake;
            if (awake) return;

            long s2 = this.wakeLock.tryConvertToWriteLock(stamp);
            if (s2 == 0L) {
                this.wakeLock.unlock(stamp);
                stamp = this.wakeLock.writeLock();
                awake = this.awake;
                if (awake) return;
            } else {
                stamp = s2;
            }

            EventPipe pipe = new EventPipe(this);
            ScheduledTask task = Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(
                            plugin,
                            (ScheduledTask t) -> {
                                if (!this.update()) {
                                    t.cancel();
                                    this.hibernate();
                                }
                            },
                            1L,
                            1L);

            this.eventPipe = pipe;
            this.updateTask = task;
            this.awake = true;
            Bukkit.getPluginManager().registerEvents(pipe, plugin);
        } finally {
            this.wakeLock.unlock(stamp);
        }
    }

    private void hibernate() {
        final long stamp = this.wakeLock.writeLock();
        try {
            if (!this.awake) return;
            HandlerList.unregisterAll(this.eventPipe);
            this.updateTask.cancel();
            this.awake = false;
            this.eventPipe = null;
            this.updateTask = null;
        } finally {
            this.wakeLock.unlock(stamp);
        }
    }

    @Override
    protected void userSync(PaperDialogUser user, Runnable task) {
        if (NOT_FOLIA) {
            task.run();
            return;
        }
        user.handle().getScheduler().run(
                this.instance.plugin(),
                (ScheduledTask _) -> task.run(),
                null
        );
    }

    @Override
    protected void send(PaperDialogUser user, Dialog dialog) {
        user.handle().showDialog(dialog);
    }

    @Override
    protected void clear(PaperDialogUser user) {
        user.handle().closeDialog();
    }

    @Override
    protected Dialog render(DialogElement dialog) {
        DialogRenderContext ctx = new DialogRenderContext(this.keyFactory);
        return this.instance.renderer().render(dialog, ctx);
    }

    public void show(Player player) {
        this.show(this.instance.user(player));
    }

    public void hide(Player player) {
        this.hide(this.instance.user(player));
    }

    //

    private record EventPipe(
            PaperDialog parent
    ) implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onCustomClick(PlayerCustomClickEvent event) {
            if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
            PaperDialogUser user = this.parent.instance.user(connection.getPlayer());
            Key id = event.getIdentifier();
            TokenizedDialogKeyFactory keyFactory = this.parent.keyFactory;
            TokenizedDialogKeyFactory.ParseResult result = keyFactory.parse(id);
            if (result == null) return;

            String eid = result.id();
            if (eid != null) {
                this.parent.respond(eid, new PaperDialogResponse(
                        event.getDialogResponseView(),
                        user,
                        this.parent.document(),
                        keyFactory
                ));
            }

            if (this.parent.isConfiguredToClose()) {
                this.parent.untrack(user);
            }
        }

        // The server doesn't get a packet when a dialog is closed,
        // so we listen for actions that are impossible when a dialog is open
        // to detect when a dialog has been definitely canceled. Listening
        // for the custom click event when the dialog afterAction is "close"
        // is more reliable and should cover most cases.

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onDropItem(PlayerDropItemEvent event) {
            this.untrack(event.getPlayer(), true);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onArmSwing(PlayerArmSwingEvent event) {
            this.untrack(event.getPlayer(), true);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onChat(AsyncChatEvent event) {
            this.untrack(event.getPlayer(), true);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onInventoryOpen(InventoryOpenEvent event) {
            if (!(event.getPlayer() instanceof Player player)) return;
            this.untrack(player, true);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onDeath(PlayerDeathEvent event) {
            this.untrack(event.getPlayer(), true);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onQuit(PlayerQuitEvent event) {
            this.untrack(event.getPlayer(), false);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onCommand(PlayerCommandPreprocessEvent event) {
            this.untrack(event.getPlayer(), true);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onLook(PlayerMoveEvent event) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getYaw() == to.getYaw() && from.getPitch() == to.getPitch()) return;
            this.untrack(event.getPlayer(), true);
        }

        private void untrack(Player player, boolean onlyTenured) {
            PaperDialogUser user = this.parent.instance.user(player);
            if (!this.parent.isTracked(user)) return;
            this.parent.untrack(user, onlyTenured);
        }

    }

}
