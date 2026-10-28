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

import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.player.User;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.manager.DialogManager;
import moe.hendeca.minidialog.response.DialogResponse;
import moe.hendeca.minidialog.response.VelocityDialogResponse;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

@NullMarked
public final class VelocityDialog
        extends PacketEventsDialog<VelocityMiniDialog, VelocityDialogUser>
{

    private final StampedLock wakeLock;
    private boolean awake;
    private @UnknownNullability EventPipe eventPipe;
    private @UnknownNullability ScheduledTask thinkTask;

    VelocityDialog(
            VelocityMiniDialog instance,
            DialogElement document,
            @Nullable DialogManager manager
    ) {
        super(instance, VelocityDialogUser.class, document, manager);
        this.wakeLock = new StampedLock();
        this.awake = false;
    }

    //

    public void show(Player player) {
        this.show(this.instance.user(player));
    }

    public void hide(Player player) {
        this.hide(this.instance.user(player));
    }

    @Override
    protected @Nullable VelocityDialogUser user(User user) {
        return this.instance.server()
                .getPlayer(user.getUUID())
                .map(this.instance::user)
                .orElse(null);
    }

    @Override
    protected DialogResponse response(VelocityDialogUser user, @Nullable NBT payload) {
        return new VelocityDialogResponse(user, payload, this.document(), this.keyFactory);
    }

    @Override
    protected boolean canShow(VelocityDialogUser user) {
        return user.supportsDialog();
    }

    @Override
    protected void wake() {
        long stamp = this.wakeLock.readLock();
        try {
            if (this.awake) return;
            long s2 = this.wakeLock.tryConvertToWriteLock(stamp);
            if (s2 == 0L) {
                this.wakeLock.unlock(stamp);
                stamp = this.wakeLock.writeLock();
                if (this.awake) return;
            } else {
                stamp = s2;
            }

            ProxyServer server = this.instance.server();
            EventPipe eventPipe = this.registerEventPipe(this.instance.packetEventsAPI());
            ScheduledTask task = server.getScheduler().buildTask(this.instance.plugin(), () -> {
                if (!this.update()) this.hibernate();
            }).repeat(50L, TimeUnit.MILLISECONDS).schedule();

            this.awake = true;
            this.eventPipe = eventPipe;
            this.thinkTask = task;
        } finally {
            this.wakeLock.unlock(stamp);
        }
    }

    private void hibernate() {
        long stamp = this.wakeLock.readLock();
        try {
            if (!this.awake) return;
            long s2 = this.wakeLock.tryConvertToWriteLock(stamp);
            if (s2 == 0L) {
                this.wakeLock.unlock(stamp);
                stamp = this.wakeLock.writeLock();
                if (!this.awake) return;
            } else {
                stamp = s2;
            }

            EventPipe eventPipe = this.eventPipe;
            ScheduledTask task = this.thinkTask;
            this.unregisterEventPipe(eventPipe);
            task.cancel();
            this.awake = false;
            this.eventPipe = null;
            this.thinkTask = null;
        } finally {
            this.wakeLock.unlock(stamp);
        }
    }

}
