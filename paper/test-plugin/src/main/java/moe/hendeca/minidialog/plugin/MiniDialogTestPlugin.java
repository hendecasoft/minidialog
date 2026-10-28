package moe.hendeca.minidialog.plugin;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import moe.hendeca.minidialog.Dialog;
import moe.hendeca.minidialog.MiniDialog;
import moe.hendeca.minidialog.PaperMiniDialog;
import moe.hendeca.minidialog.plugin.sample.Sample;
import moe.hendeca.minidialog.plugin.sample.Samples;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.Collections;

@NullMarked
public final class MiniDialogTestPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Initialize MiniDialog
        MiniDialog md = PaperMiniDialog.miniDialog(this);

        // Register main command
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, (c) -> {
            c.registrar().register("mdtp", new SamplesCommand(md));
        });
    }

    //

    private record SamplesCommand(
            MiniDialog md
    ) implements BasicCommand {

        @Override
        public void execute(CommandSourceStack stack, String[] args) {
            if (args.length < 1) {
                stack.getSender().sendMessage(Component.text("* A dialog ID is required!").color(NamedTextColor.RED));
                return;
            }

            if (args.length > 1) {
                stack.getSender().sendMessage(Component.text("* Too many arguments!").color(NamedTextColor.RED));
                return;
            }

            if (!(stack.getSender() instanceof Player p)) {
                stack.getSender().sendMessage(Component.text("* Not a player!").color(NamedTextColor.RED));
                return;
            }

            String id = args[0];
            Sample sample = Samples.get(id);

            if (sample == null) {
                stack.getSender().sendMessage(Component.text("* No dialog found with that ID!").color(NamedTextColor.RED));
                return;
            }

            sample.show(this.md, this.md.user(p));
        }

        @Override
        public Collection<String> suggest(CommandSourceStack stack, String[] args) {
            if (args.length <= 1) return Samples.keys();
            return Collections.emptyList();
        }

        @Override
        public boolean canUse(CommandSender sender) {
            return sender instanceof Player;
        }

    }

}
