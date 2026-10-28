package moe.hendeca.minidialog.server;

import moe.hendeca.minidialog.ModdedDialogUser;
import net.minecraft.server.level.ServerPlayer;

public final class ModdedServerDialogUser
        extends VanillaServerDialogUser
        implements ModdedDialogUser
{

    ModdedServerDialogUser(ModdedServerMiniDialog instance, ServerPlayer handle) {
        super(instance, handle);
    }

}
