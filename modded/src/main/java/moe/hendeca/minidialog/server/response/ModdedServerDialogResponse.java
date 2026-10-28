package moe.hendeca.minidialog.server.response;

import moe.hendeca.minidialog.TokenizedDialogKeyFactory;
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.response.ModdedDialogResponse;
import moe.hendeca.minidialog.response.VanillaDialogResponse;
import moe.hendeca.minidialog.server.ModdedServerDialogUser;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ModdedServerDialogResponse
    extends VanillaDialogResponse<ModdedServerDialogUser>
    implements ModdedDialogResponse
{

    @ApiStatus.Internal
    public ModdedServerDialogResponse(
            @Nullable Tag handle,
            ModdedServerDialogUser user,
            DialogElement document,
            TokenizedDialogKeyFactory keyFactory
    ) {
        super(handle, user, document, keyFactory);
    }

}
