package moe.hendeca.minidialog.plugin.sample.impl;

import moe.hendeca.minidialog.DialogUser;
import moe.hendeca.minidialog.element.ItemElement;
import moe.hendeca.minidialog.manager.DialogBind;
import moe.hendeca.minidialog.plugin.sample.Sample;
import moe.hendeca.minidialog.response.DialogResponse;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;

import java.util.Random;

@NullMarked
public final class ColorQuizSample extends Sample {

    @DialogBind.Element("item")
    @UnknownNullability ItemElement item;

    private @UnknownNullability ItemColor color;

    //

    public ColorQuizSample() {
        super("color_quiz");
    }

    //

    @DialogBind.Init
    void init() {
        ItemKind kind = Sampler.ITEM_KIND.next();
        ItemColor color = Sampler.ITEM_COLOR.next();
        this.item.src(this.createItemKey(kind, color));
        this.color = color;
    }

    @DialogBind.CustomAction("chooseBlack")
    void chooseBlack(DialogResponse response) {
        this.submit(response.user(), ItemColor.BLACK);
    }

    @DialogBind.CustomAction("chooseWhite")
    void chooseWhite(DialogResponse response) {
        this.submit(response.user(), ItemColor.WHITE);
    }

    @DialogBind.CustomAction("chooseGray")
    void chooseGray(DialogResponse response) {
        this.submit(response.user(), ItemColor.GRAY);
    }

    @DialogBind.CustomAction("chooseLightGray")
    void chooseLightGray(DialogResponse response) {
        this.submit(response.user(), ItemColor.LIGHT_GRAY);
    }

    @DialogBind.CustomAction("choosePink")
    void choosePink(DialogResponse response) {
        this.submit(response.user(), ItemColor.PINK);
    }

    @DialogBind.CustomAction("chooseRed")
    void chooseRed(DialogResponse response) {
        this.submit(response.user(), ItemColor.RED);
    }

    @DialogBind.CustomAction("chooseOrange")
    void chooseOrange(DialogResponse response) {
        this.submit(response.user(), ItemColor.ORANGE);
    }

    @DialogBind.CustomAction("chooseYellow")
    void chooseYellow(DialogResponse response) {
        this.submit(response.user(), ItemColor.YELLOW);
    }

    @DialogBind.CustomAction("chooseBrown")
    void chooseBrown(DialogResponse response) {
        this.submit(response.user(), ItemColor.BROWN);
    }

    @DialogBind.CustomAction("chooseGreen")
    void chooseGreen(DialogResponse response) {
        this.submit(response.user(), ItemColor.GREEN);
    }

    @DialogBind.CustomAction("chooseLime")
    void chooseLime(DialogResponse response) {
        this.submit(response.user(), ItemColor.LIME);
    }

    @DialogBind.CustomAction("chooseCyan")
    void chooseCyan(DialogResponse response) {
        this.submit(response.user(), ItemColor.CYAN);
    }

    @DialogBind.CustomAction("chooseLightBlue")
    void chooseLightBlue(DialogResponse response) {
        this.submit(response.user(), ItemColor.LIGHT_BLUE);
    }

    @DialogBind.CustomAction("chooseBlue")
    void chooseBlue(DialogResponse response) {
        this.submit(response.user(), ItemColor.BLUE);
    }

    @DialogBind.CustomAction("choosePurple")
    void choosePurple(DialogResponse response) {
        this.submit(response.user(), ItemColor.PURPLE);
    }

    @DialogBind.CustomAction("chooseMagenta")
    void chooseMagenta(DialogResponse response) {
        this.submit(response.user(), ItemColor.MAGENTA);
    }

    private void submit(DialogUser user, ItemColor color) {
        if (color.equals(this.color)) {
            user.audience().sendMessage(Component.text("* Correct!")
                    .color(NamedTextColor.GREEN));
        } else {
            user.audience().sendMessage(Component.text("* Incorrect!")
                    .color(NamedTextColor.RED));
        }
    }

    @SuppressWarnings("PatternValidation")
    private Key createItemKey(ItemKind kind, ItemColor color) {
        return Key.key(Key.MINECRAFT_NAMESPACE, color.name + "_" + kind.name);
    }

    //

    private static final class Sampler<E extends Enum<E>> {

        static final Sampler<ItemKind> ITEM_KIND = new Sampler<>(ItemKind.class);
        static final Sampler<ItemColor> ITEM_COLOR = new Sampler<>(ItemColor.class);

        //

        private final Random random;
        private final E[] constants;

        private Sampler(Class<E> clazz) {
            E[] constants = clazz.getEnumConstants();
            if (constants == null) throw new IllegalArgumentException("Class " + clazz.getName() + " is not an enum");
            this.random = new Random();
            this.constants = constants;
        }

        //

        synchronized E next() {
            return this.constants[this.random.nextInt(this.constants.length)];
        }

    }

    private enum ItemKind {
        DYE("dye"),
        WOOL("wool"),
        CONCRETE("concrete"),
        CONCRETE_POWDER("concrete_powder"),
        TERRACOTTA("terracotta");

        final @KeyPattern.Value String name;
        ItemKind(@KeyPattern.Value String name) {
            this.name = name;
        }
    }

    private enum ItemColor {
        BLACK("black"),
        WHITE("white"),
        GRAY("gray"),
        LIGHT_GRAY("light_gray"),
        PINK("pink"),
        RED("red"),
        ORANGE("orange"),
        YELLOW("yellow"),
        BROWN("brown"),
        GREEN("green"),
        LIME("lime"),
        CYAN("cyan"),
        LIGHT_BLUE("light_blue"),
        BLUE("blue"),
        PURPLE("purple"),
        MAGENTA("magenta");

        final @KeyPattern.Value String name;

        ItemColor(@KeyPattern.Value String name) {
            this.name = name;
        }
    }

}
