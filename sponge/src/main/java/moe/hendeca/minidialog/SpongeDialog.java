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
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.manager.DialogManager;
import moe.hendeca.minidialog.response.SpongeDialogResponse;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;

import java.util.UUID;
import java.util.concurrent.locks.StampedLock;

@NullMarked
public final class SpongeDialog extends PacketEventsDialog<SpongeMiniDialog, SpongeDialogUser> {

    private final StampedLock wakeLock;
    private boolean awake;
    private @UnknownNullability EventPipe eventPipe;
    private @UnknownNullability ScheduledTask updateTask;

    //

    SpongeDialog(
            SpongeMiniDialog instance,
            DialogElement document,
            @Nullable DialogManager manager
    ) {
        super(instance, SpongeDialogUser.class, document, manager);
        this.wakeLock = new StampedLock();
        this.awake = false;
    }

    //

    @Override
    protected @Nullable SpongeDialogUser user(User user) {
        UUID id = user.getUUID();
        return this.instance.server()
                .player(id)
                .map(this.instance::user)
                .orElse(null);
    }

    @Override
    protected SpongeDialogResponse response(SpongeDialogUser user, @Nullable NBT payload) {
        return new SpongeDialogResponse(
                user,
                payload,
                this.document(),
                this.keyFactory
        );
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

            EventPipe eventPipe = this.registerEventPipe(this.instance.packetEventsAPI());
            ScheduledTask updateTask = this.instance.server()
                    .scheduler()
                    .submit(Task.builder()
                            .plugin(this.instance.plugin())
                            .interval(Ticks.of(1L))
                            .execute(() -> {
                                if (!this.update()) this.hibernate();
                            })
                            .build());

            this.eventPipe = eventPipe;
            this.updateTask = updateTask;
            this.awake = true;
        } finally {
            this.wakeLock.unlock(stamp);
        }
    }

    private void hibernate() {
        long stamp = this.wakeLock.writeLock();
        try {
            if (!this.awake) return;
            this.unregisterEventPipe(this.eventPipe);
            this.updateTask.cancel();
            this.awake = false;
            this.eventPipe = null;
            this.updateTask = null;
        } finally {
            this.wakeLock.unlock(stamp);
        }
    }

}
