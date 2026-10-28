package moe.hendeca.minidialog.plugin.sample.impl;

import moe.hendeca.minidialog.element.Element;
import moe.hendeca.minidialog.element.OptionElement;
import moe.hendeca.minidialog.element.OptionInputElement;
import moe.hendeca.minidialog.manager.DialogBind;
import moe.hendeca.minidialog.plugin.sample.Sample;
import moe.hendeca.minidialog.response.DialogResponse;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@NullMarked
public final class PianoSample extends Sample {

    @DialogBind.Context
    @UnknownNullability Audience audience;

    @DialogBind.Element("instrument")
    @UnknownNullability OptionInputElement instrument;

    //

    public PianoSample() {
        super("piano");
    }

    //

    @DialogBind.Init
    void init() {
        // Select a random instrument
        List<String> instruments = this.instrument
                .children()
                .stream()
                .filter(OptionElement.class::isInstance)
                .map((Element e) -> Objects.requireNonNull(((OptionElement) e).value()))
                .toList();
        this.instrument.initial(instruments.get(ThreadLocalRandom.current().nextInt(instruments.size())));
    }

    @DialogBind.CustomAction("play00")
    void play00(DialogResponse response) {
        this.play(response, 0);
    }

    @DialogBind.CustomAction("play01")
    void play01(DialogResponse response) {
        this.play(response, 1);
    }

    @DialogBind.CustomAction("play02")
    void play02(DialogResponse response) {
        this.play(response, 2);
    }

    @DialogBind.CustomAction("play03")
    void play03(DialogResponse response) {
        this.play(response, 3);
    }

    @DialogBind.CustomAction("play04")
    void play04(DialogResponse response) {
        this.play(response, 4);
    }

    @DialogBind.CustomAction("play05")
    void play05(DialogResponse response) {
        this.play(response, 5);
    }

    @DialogBind.CustomAction("play06")
    void play06(DialogResponse response) {
        this.play(response, 6);
    }

    @DialogBind.CustomAction("play07")
    void play07(DialogResponse response) {
        this.play(response, 7);
    }

    @DialogBind.CustomAction("play08")
    void play08(DialogResponse response) {
        this.play(response, 8);
    }

    @DialogBind.CustomAction("play09")
    void play09(DialogResponse response) {
        this.play(response, 9);
    }

    @DialogBind.CustomAction("play10")
    void play10(DialogResponse response) {
        this.play(response, 10);
    }

    @DialogBind.CustomAction("play11")
    void play11(DialogResponse response) {
        this.play(response, 11);
    }

    @DialogBind.CustomAction("play12")
    void play12(DialogResponse response) {
        this.play(response, 12);
    }

    @DialogBind.CustomAction("play13")
    void play13(DialogResponse response) {
        this.play(response, 13);
    }

    @DialogBind.CustomAction("play14")
    void play14(DialogResponse response) {
        this.play(response, 14);
    }

    @DialogBind.CustomAction("play15")
    void play15(DialogResponse response) {
        this.play(response, 15);
    }

    @DialogBind.CustomAction("play16")
    void play16(DialogResponse response) {
        this.play(response, 16);
    }

    @DialogBind.CustomAction("play17")
    void play17(DialogResponse response) {
        this.play(response, 17);
    }

    @DialogBind.CustomAction("play18")
    void play18(DialogResponse response) {
        this.play(response, 18);
    }

    @DialogBind.CustomAction("play19")
    void play19(DialogResponse response) {
        this.play(response, 19);
    }

    @DialogBind.CustomAction("play20")
    void play20(DialogResponse response) {
        this.play(response, 20);
    }

    @DialogBind.CustomAction("play21")
    void play21(DialogResponse response) {
        this.play(response, 21);
    }

    @DialogBind.CustomAction("play22")
    void play22(DialogResponse response) {
        this.play(response, 22);
    }

    @DialogBind.CustomAction("play23")
    void play23(DialogResponse response) {
        this.play(response, 23);
    }

    @DialogBind.CustomAction("play24")
    void play24(DialogResponse response) {
        this.play(response, 24);
    }

    private void play(DialogResponse response, @Range(from = 0, to = 24) int pitch) {
        Sound sound = this.resolveSound(response, pitch);
        this.audience.playSound(sound);
    }

    private Sound resolveSound(DialogResponse response, @Range(from = 0, to = 24) int pitch) {
        @Subst("banjo") String instrument = response.value(this.instrument).value();
        assert instrument != null;
        return Sound.sound(
                Key.key(Key.MINECRAFT_NAMESPACE, "block.note_block." + instrument),
                Sound.Source.MASTER,
                1f,
                (float) Math.pow(2d, (double) (pitch - 12) / 12d)
        );
    }

}
