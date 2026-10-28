package moe.hendeca.minidialog.plugin.sample.impl;

import moe.hendeca.minidialog.DialogUser;
import moe.hendeca.minidialog.manager.DialogBind;
import moe.hendeca.minidialog.plugin.sample.Sample;
import moe.hendeca.minidialog.response.DialogResponse;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@NullMarked
public final class FortuneSample extends Sample {

    private static final long TICKS_PER_NOTE = 2L;
    private static final int[] NOTES = new int[] { 11, 11, -1, 11, 11, -1, 9, -1, -1, 9, -1, -1, 7, -1, -1, 7, -1, -1, 9 };
    private static final List<String> FORTUNES = List.of(
            "You are the hero of the brine.",
            "Your diamonds will increase.",
            "Mining is a virtue.",
            "You cannot craft what you do not mine.",
            "A villager will trade in your favor.",
            "Your pick is the pick that will pierce the heavens!",
            "81 netherite can make 1 beacon.",
            "Sell me this iron ingot.",
            "New perspectives await for those who press F5.",
            "Don't mine at night.",
            "What!? I wasn't ready.",
            "A fool jumps for the beef."
    );

    //

    @DialogBind.Context
    @UnknownNullability Audience audience;

    private long counter;

    public FortuneSample() {
        super("fortune");
    }

    //

    @DialogBind.Open
    void open() {
        this.counter = 0L;
    }

    @DialogBind.CustomAction("yes")
    void confirm(DialogResponse response) {
        DialogUser user = response.user();
        Random random = ThreadLocalRandom.current();

        // Fortune
        String fortune = FORTUNES.get(random.nextInt(FORTUNES.size()));

        // Lucky numbers
        StringBuilder lucky = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i != 0) lucky.append(", ");
            lucky.append(random.nextInt(100));
        }

        user.audience().showTitle(Title.title(
                Component.text(fortune).color(NamedTextColor.GOLD),
                Component.text(lucky.toString()).color(NamedTextColor.YELLOW)
        ));

        user.audience().playSound(Sound.sound(
                Key.key("block.bell.resonate"),
                Sound.Source.MASTER,
                1f,
                2f
        ));
    }

    @DialogBind.CustomAction("no")
    void reject(DialogResponse response) {
        response.user().audience()
                .sendMessage(Component.text("* I don't care for you much either.").color(NamedTextColor.YELLOW));
    }

    @DialogBind.Think(TICKS_PER_NOTE)
    void think() {
        long tick = this.counter++;
        if (tick < (long) NOTES.length) this.playNote(NOTES[(int) tick]);
    }

    private void playNote(int note) {
        if (note == -1) return;
        float pitch = (float) Math.pow(2d, (double) (note - 12) / 12d);
        float pitch2 = (float) Math.pow(2d, Math.min((double) (note - 12) + 0.5, 12d) / 12d);
        this.audience.playSound(Sound.sound(
                Key.key(Key.MINECRAFT_NAMESPACE, "block.note_block.pling"),
                Sound.Source.MASTER,
                1f,
                pitch
        ));
        this.audience.playSound(Sound.sound(
                Key.key(Key.MINECRAFT_NAMESPACE, "block.note_block.banjo"),
                Sound.Source.MASTER,
                0.25f,
                pitch2
        ));

        @KeyPattern.Value String drum;
        if (note == 9) {
            drum = "snare";
        } else if (note == 11) {
            drum = "basedrum";
        } else {
            return;
        }
        this.audience.playSound(Sound.sound(
                Key.key(Key.MINECRAFT_NAMESPACE, "block.note_block." + drum),
                Sound.Source.MASTER,
                0.75f,
                pitch
        ));
    }

}
