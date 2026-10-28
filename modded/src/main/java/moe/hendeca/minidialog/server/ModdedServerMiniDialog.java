package moe.hendeca.minidialog.server;

import moe.hendeca.minidialog.ModdedMiniDialog;
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.manager.DialogManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@NullMarked
public final class ModdedServerMiniDialog
        extends VanillaServerMiniDialog<ModdedServerDialog, ModdedServerDialogUser>
        implements ModdedMiniDialog
{

    @ApiStatus.Internal
    public ModdedServerMiniDialog(MinecraftServer server) {
        super(server);
    }

    //

    @Override
    protected ModdedServerDialogUser user(ServerPlayer handle) {
        Objects.requireNonNull(handle, "handle must not be null");
        return new ModdedServerDialogUser(this, handle);
    }

    @Override
    protected ModdedServerDialog newDialog(DialogElement document, @Nullable DialogManager manager) {
        return new ModdedServerDialog(this, document, manager);
    }

}
