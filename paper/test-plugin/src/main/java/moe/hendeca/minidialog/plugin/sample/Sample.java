package moe.hendeca.minidialog.plugin.sample;

import moe.hendeca.minidialog.DialogUser;
import moe.hendeca.minidialog.MiniDialog;
import moe.hendeca.minidialog.element.DialogElement;
import moe.hendeca.minidialog.manager.DialogManager;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class Sample implements DialogManager {

    private final String id;
    private @Nullable DialogElement doc;

    protected Sample(String id) {
        this.id = id;
    }

    //

    @Contract(pure = true)
    public String id() {
        return this.id;
    }

    public void show(MiniDialog md, DialogUser user) {
        md.init(this.resolveDocument(md), this.getClass())
                .show(user);
    }

    protected synchronized DialogElement resolveDocument(MiniDialog md) {
        DialogElement ret = this.doc;
        if (ret != null) return ret;
        this.doc = ret = md.read("dialogs/" + this.id + ".mdlg");
        return ret;
    }

}
