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

package moe.hendeca.minidialog.render;

import com.github.retrooper.packetevents.protocol.chat.clickevent.*;
import com.github.retrooper.packetevents.protocol.dialog.*;
import com.github.retrooper.packetevents.protocol.dialog.Dialog;
import com.github.retrooper.packetevents.protocol.dialog.action.DynamicCustomAction;
import com.github.retrooper.packetevents.protocol.dialog.action.StaticAction;
import com.github.retrooper.packetevents.protocol.dialog.body.DialogBody;
import com.github.retrooper.packetevents.protocol.dialog.body.ItemDialogBody;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessage;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessageDialogBody;
import com.github.retrooper.packetevents.protocol.dialog.button.ActionButton;
import com.github.retrooper.packetevents.protocol.dialog.button.CommonButtonData;
import com.github.retrooper.packetevents.protocol.dialog.input.*;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import moe.hendeca.minidialog.element.*;
import moe.hendeca.minidialog.except.DialogRenderException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@NullMarked
@ApiStatus.Internal
public final class PacketEventsDialogRenderer extends AbstractDialogRenderer<Dialog> {

    public static final PacketEventsDialogRenderer INSTANCE = new PacketEventsDialogRenderer();

    //

    private PacketEventsDialogRenderer() { }

    //

    @Override
    public Dialog render(DialogElement element, DialogRenderContext ctx) throws DialogRenderException {
        // Reference
        Key src = element.src();
        if (src != null) {
            if (!element.isReference())
                throw new DialogRenderException("Element may not have other properties or children when src is set");
            Dialog d = Dialogs.getRegistry().getByName(new ResourceLocation(src));
            if (d == null)
                throw new DialogRenderException("Dialog with key \"" + src + "\" does not exist in registry");
            return d;
        }

        // Non-reference
        Flattener.DialogElements children = Flattener.DIALOG.flatten(element);
        Schema schema = Schema.match(children);
        CommonDialogData common = this.renderCommon(element, children, ctx);
        return switch (schema) {
            case NOTICE -> {
                ActionButton action = children.footer() != null && !children.footer().children().isEmpty() ?
                        this.renderAction((ActionElement) children.footer().children().getFirst(), ctx, 0, true) :
                        NoticeDialog.DEFAULT_ACTION;
                yield new NoticeDialog(common, action);
            }
            case MULTI_ACTION -> {
                ActionsElement actions = children.actions();
                assert actions != null;

                List<? extends Element> actionsChildren = actions.children();
                List<ActionButton> actionButtons = new ArrayList<>(actionsChildren.size());
                for (int i = 0; i < actionsChildren.size(); i++) {
                    actionButtons.add(this.renderAction(
                            (ActionElement) actionsChildren.get(i),
                            ctx,
                            i,
                            false
                    ));
                }

                ActionButton exitAction = null;
                if (children.footer() != null && !children.footer().children().isEmpty()) {
                    exitAction = this.renderAction((ActionElement) children.footer().children().getFirst(), ctx, 0, true);
                }

                yield new MultiActionDialog(
                        common,
                        actionButtons,
                        exitAction,
                        actions.columns()
                );
            }
            case CONFIRMATION -> {
                FooterElement footer = children.footer();
                assert footer != null;
                yield new ConfirmationDialog(
                        common,
                        this.renderAction(
                                (ActionElement) footer.children().getFirst(),
                                ctx,
                                0,
                                true
                        ),
                        this.renderAction(
                                (ActionElement) footer.children().get(1),
                                ctx,
                                1,
                                true
                        )
                );
            }
        };
    }

    private CommonDialogData renderCommon(
            DialogElement element,
            Flattener.DialogElements children,
            DialogRenderContext ctx
    ) throws DialogRenderException {
        List<? extends Element> bodyChildren;
        List<? extends Element> inputsChildren;

        List<DialogBody> body;
        if (children.body() == null || (bodyChildren = children.body().children()).isEmpty()) {
            body = Collections.emptyList();
        } else {
            body = new ArrayList<>(bodyChildren.size());
            for (Element child : bodyChildren) {
                switch (child) {
                    case MessageElement m -> body.add(this.renderMessage(m));
                    case ItemElement i -> body.add(this.renderItem(i));
                    default -> {}
                }
            }
        }

        List<Input> inputs;
        if (children.inputs() == null || (inputsChildren = children.inputs().children()).isEmpty()) {
            inputs = Collections.emptyList();
        } else {
            inputs = new ArrayList<>(inputsChildren.size());
            for (Element child : inputsChildren) {
                if (!(child instanceof InputElement input)) continue;
                int i = children.inputs().indexOf(child);
                inputs.add(this.renderInput(input, ctx, i));
            }
        }

        return new CommonDialogData(
                children.title() != null ? children.title().text() : Component.empty(),
                children.externalTitle() != null ? children.externalTitle().text() : null,
                element.canCloseWithEscape(),
                element.pause(),
                this.renderAfterAction(element.afterAction()),
                body,
                inputs
        );
    }

    private DialogAction renderAfterAction(DialogElement.AfterAction action) {
        return switch (action) {
            case CLOSE -> DialogAction.CLOSE;
            case WAIT_FOR_RESPONSE -> DialogAction.WAIT_FOR_RESPONSE;
            case NONE -> DialogAction.NONE;
        };
    }

    private PlainMessageDialogBody renderMessage(MessageElement element) {
        return new PlainMessageDialogBody(new PlainMessage(element.text(), element.width()));
    }

    private DialogBody renderItem(ItemElement element) {
        Key src = element.src();
        ItemType itemType = ItemTypes.getRegistry().getByName(new ResourceLocation(src));
        if (itemType == null) itemType = ItemTypes.AIR;

        ItemStack item = ItemStack.builder()
                .type(itemType)
                .amount(element.count())
                .build();

        DescriptionElement descriptionElement = Flattener.ITEM.flatten(element).description();
        PlainMessage description;
        if (descriptionElement == null) {
            description = null;
        } else {
            description = new PlainMessage(descriptionElement.text(), descriptionElement.width());
        }

        return new ItemDialogBody(
                item,
                description,
                element.showDecoration(),
                element.showTooltip(),
                element.width(),
                element.height()
        );
    }

    private Input renderInput(InputElement input, DialogRenderContext ctx, int index) {
        String key = ctx.keyFactory().inputKey(index, input.id());
        InputControl control = switch (input) {
            case BooleanInputElement b -> this.renderBooleanInputControl(b);
            case TextInputElement t -> this.renderTextInputControl(t);
            case RangeInputElement r -> this.renderRangeInputControl(r);
            case OptionInputElement o -> this.renderOptionInputControl(o, ctx);
        };
        return new Input(key, control);
    }

    private BooleanInputControl renderBooleanInputControl(BooleanInputElement element) {
        Flattener.InputElements children = Flattener.INPUT.flatten(element);
        return new BooleanInputControl(
                children.label() != null ? children.label().text() : Component.empty(),
                element.initial(),
                "true",
                "false"
        );
    }

    private TextInputControl renderTextInputControl(TextInputElement element) {
        Flattener.TextInputElements children = Flattener.TEXT_INPUT.flatten(element);

        Component label;
        boolean labelVisible;
        if (children.label() == null) {
            label = Component.empty();
            labelVisible = false;
        } else {
            label = children.label().text();
            labelVisible = children.label().visible();
        }

        TextInputControl.MultilineOptions multiline;
        if (children.multiline() == null) {
            multiline = null;
        } else {
            multiline = new TextInputControl.MultilineOptions(
                    children.multiline().maxLines(),
                    children.multiline().height()
            );
        }

        return new TextInputControl(
                element.width(),
                label,
                labelVisible,
                element.initial() == null ? "" : element.initial(),
                element.maxLength(),
                multiline
        );
    }

    private NumberRangeInputControl renderRangeInputControl(RangeInputElement element) {
        Flattener.InputElements children = Flattener.INPUT.flatten(element);

        Component label;
        String labelFormat;
        if (children.label() == null) {
            label = Component.empty();
            labelFormat = "options.generic_value";
        } else {
            label = children.label().text();
            labelFormat = children.label().format();
        }

        Double initial = element.initial();
        Double step = element.step();

        return new NumberRangeInputControl(
                element.width(),
                label,
                labelFormat,
                new NumberRangeInputControl.RangeInfo(
                        (float) element.start(),
                        (float) element.end(),
                        initial != null ? initial.floatValue() : null,
                        step != null ? step.floatValue() : null
                )
        );
    }

    private SingleOptionInputControl renderOptionInputControl(OptionInputElement element, DialogRenderContext ctx) {
        Flattener.OptionInputElements children = Flattener.OPTION_INPUT.flatten(element);

        Component label;
        boolean labelVisible;
        if (children.label() == null) {
            label = Component.empty();
            labelVisible = false;
        } else {
            label = children.label().text();
            labelVisible = children.label().visible();
        }

        String initialValue = element.initial();
        boolean permitInitial = true;
        List<OptionElement> options = children.options();
        int optionCount = options.size();
        if (optionCount == 0) throw new DialogRenderException("Option input has no options");
        List<SingleOptionInputControl.Entry> entries = new ArrayList<>(optionCount);
        for (OptionElement option : options) {
            int i = element.indexOf(option);
            String key = ctx.keyFactory().optionKey(i, option.id());

            boolean initial;
            if (permitInitial) {
                initial = Objects.equals(initialValue, option.value());
                permitInitial = false;
            } else {
                initial = false;
            }

            entries.add(new SingleOptionInputControl.Entry(
                    key,
                    option.text(),
                    initial
            ));
        }

        return new SingleOptionInputControl(
                element.width(),
                entries,
                label,
                labelVisible
        );
    }

    private ActionButton renderAction(
            ActionElement element,
            DialogRenderContext ctx,
            int index,
            boolean footer
    ) {
        return switch (element) {
            case OpenUrlActionElement a -> new ActionButton(
                    this.renderCommonButtonData(element),
                    new StaticAction(new OpenUrlClickEvent(a.url())));
            case RunCommandActionElement a -> new ActionButton(
                    this.renderCommonButtonData(element),
                    new StaticAction(new RunCommandClickEvent(a.command())));
            case SuggestCommandActionElement a -> new ActionButton(
                    this.renderCommonButtonData(element),
                    new StaticAction(new SuggestCommandClickEvent(a.command())));
            case CopyToClipboardActionElement a -> new ActionButton(
                    this.renderCommonButtonData(element),
                    new StaticAction(new CopyToClipboardClickEvent(a.value())));
            case ShowDialogActionElement a -> {
                Flattener.ShowDialogActionElements children = Flattener.SHOW_DIALOG_ACTION.flatten(a);
                CommonButtonData common = this.renderCommonButtonData(a, children);
                yield new ActionButton(common, new StaticAction(new ShowDialogClickEvent(this.render(children.dialog(), ctx))));
            }
            case CustomActionElement a -> {
                Key key = ctx.keyFactory().customActionKey(index, footer, a.id());
                yield new ActionButton(
                        this.renderCommonButtonData(element),
                        new DynamicCustomAction(new ResourceLocation(key), null)
                );
            }
        };
    }

    private CommonButtonData renderCommonButtonData(ActionElement element) {
        Flattener.ActionElements children = Flattener.ACTION.flatten(element);
        return this.renderCommonButtonData(element, children);
    }

    private CommonButtonData renderCommonButtonData(ActionElement element, Flattener.ActionElements children) {
        return new CommonButtonData(
                children.label() != null ? children.label().text() : Component.empty(),
                children.tooltip() != null ? children.tooltip().text() : null,
                element.width()
        );
    }

}
