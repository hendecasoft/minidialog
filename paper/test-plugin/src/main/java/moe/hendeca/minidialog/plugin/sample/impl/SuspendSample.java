package moe.hendeca.minidialog.plugin.sample.impl;

import moe.hendeca.minidialog.element.MessageElement;
import moe.hendeca.minidialog.manager.DialogBind;
import moe.hendeca.minidialog.plugin.sample.Sample;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@NullMarked
public final class SuspendSample extends Sample {

    @DialogBind.Element("time")
    @UnknownNullability MessageElement time;

    //

    public SuspendSample() {
        super("suspend");
    }

    //

    @DialogBind.Init()
    void init() {
        this.time.text(Component.empty()
                .append(Component.text("This dialog was initialized at "))
                .append(Component.text(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                        OffsetDateTime.now())).decorate(TextDecoration.ITALIC))
        );
    }

}
