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

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.NumberRangeDialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import moe.hendeca.minidialog.element.*;
import moe.hendeca.minidialog.except.DialogRenderException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemType;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NullMarked
@SuppressWarnings("UnstableApiUsage")
public final class PaperDialogRenderer extends AbstractDialogRenderer<Dialog> {

    public static final PaperDialogRenderer INSTANCE = new PaperDialogRenderer();

    //

    private PaperDialogRenderer() { }

    //

    @Override
    public Dialog render(DialogElement element, DialogRenderContext ctx) throws DialogRenderException {
        // Reference
        Key src = element.src();
        if (src != null) {
            if (!element.isReference()) {
                throw new DialogRenderException("Dialog may not have other properties or children when src is set");
            }
            return Dialog.create(rbf -> rbf
                    .copyFrom(TypedKey.create(RegistryKey.DIALOG, src)));
        }

        // Non-reference
        Flattener.DialogElements children = Flattener.DIALOG.flatten(element);
        return Dialog.create(rbf -> rbf.empty()
                .base(this.renderBase(element, children, ctx))
                .type(this.renderType(children, ctx)));
    }

    private DialogBase renderBase(
            DialogElement element,
            Flattener.DialogElements children,
            DialogRenderContext ctx
    ) {
        Component title = children.title() != null ? children.title().text() : Component.empty();
        DialogBase.Builder ret = DialogBase.builder(title);

        if (children.externalTitle() != null) ret.externalTitle(children.externalTitle().text());
        ret.canCloseWithEscape(element.canCloseWithEscape());
        ret.pause(element.pause());
        ret.afterAction(renderAfterAction(element.afterAction()));

        if (children.body() != null) {
            List<Element> content = children.body().children();
            List<DialogBody> body = new ArrayList<>(content.size());
            for (Element inner : content) {
                if (inner instanceof MessageElement msg) {
                    body.add(this.renderMessage(msg));
                } else if (inner instanceof ItemElement itm) {
                    body.add(this.renderItem(itm));
                }
            }
            ret.body(body);
        }

        if (children.inputs() != null) {
            List<Element> content = children.inputs().children();
            List<DialogInput> inputs = new ArrayList<>(content.size());
            for (Element inner : content) {
                int i = children.inputs().indexOf(inner);
                switch (inner) {
                    case TextInputElement text -> inputs.add(this.renderTextInput(text, i, ctx));
                    case OptionInputElement option -> inputs.add(this.renderOptionInput(option, i, ctx));
                    case RangeInputElement range -> inputs.add(this.renderRangeInput(range, i, ctx));
                    case BooleanInputElement bool -> inputs.add(this.renderBooleanInput(bool, i, ctx));
                    default -> {}
                }
            }
            ret.inputs(inputs);
        }

        return ret.build();
    }

    private DialogBase.DialogAfterAction renderAfterAction(DialogElement.AfterAction action) {
        return switch (action) {
            case NONE -> DialogBase.DialogAfterAction.NONE;
            case CLOSE -> DialogBase.DialogAfterAction.CLOSE;
            case WAIT_FOR_RESPONSE -> DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE;
        };
    }

    private DialogBody renderMessage(MessageElement e) {
        return DialogBody.plainMessage(e.text(), e.width());
    }

    private DialogBody renderItem(ItemElement e) {
        DescriptionElement description = Flattener.ITEM.flatten(e).description();
        Key src = e.src();
        ItemType type = Registry.ITEM.get(src);
        if (type == null) throw new DialogRenderException("Invalid item type \"" + src + "\"");
        return DialogBody.item(
                type.createItemStack(e.count()),
                description == null ? null : DialogBody.plainMessage(description.text(), description.width()),
                e.showDecoration(),
                e.showTooltip(),
                e.width(),
                e.height()
        );
    }

    private DialogInput renderTextInput(TextInputElement e, int index, DialogRenderContext ctx) {
        Flattener.TextInputElements elements = Flattener.TEXT_INPUT.flatten(e);
        Component label;
        boolean labelVisible;
        if (elements.label() != null) {
            label = elements.label().text();
            labelVisible = elements.label().visible();
        } else {
            label = Component.empty();
            labelVisible = false;
        }

        String key = ctx.keyFactory().inputKey(index, e.id());
        TextDialogInput.Builder ret = DialogInput.text(key, label);
        ret.labelVisible(labelVisible);
        if (e.initial() != null) ret.initial(e.initial());
        ret.maxLength(e.maxLength());
        ret.width(e.width());

        MultilineElement ml = elements.multiline();
        if (ml != null) {
            ret.multiline(TextDialogInput.MultilineOptions.create(
                    ml.maxLines(),
                    ml.height()
            ));
        }
        return ret.build();
    }

    private DialogInput renderOptionInput(OptionInputElement e, int index, DialogRenderContext ctx) {
        Flattener.OptionInputElements elements = Flattener.OPTION_INPUT.flatten(e);
        Component label;
        boolean labelVisible;
        if (elements.label() != null) {
            label = elements.label().text();
            labelVisible = elements.label().visible();
        } else {
            label = Component.empty();
            labelVisible = false;
        }

        List<OptionElement> optionElements = elements.options();
        int optionElementCount = optionElements.size();
        if (optionElementCount == 0) {
            throw new DialogRenderException("Option input has no options");
        }

        List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>(optionElementCount);
        boolean permitInitial = true;
        for (OptionElement optionElement : optionElements) {
            int i = e.indexOf(optionElement);
            String optionKey = ctx.keyFactory().optionKey(i, optionElement.id());
            boolean initial;
            if (permitInitial) {
                initial = Objects.equals(e.initial(), optionElement.value());
                if (initial) permitInitial = false;
            } else {
                initial = false;
            }
            entries.add(SingleOptionDialogInput.OptionEntry.create(
                    optionKey,
                    optionElement.text(),
                    initial
            ));
        }

        String key = ctx.keyFactory().inputKey(index, e.id());
        return DialogInput.singleOption(
                key,
                e.width(),
                entries,
                label,
                labelVisible
        );
    }

    private DialogInput renderRangeInput(RangeInputElement e, int index, DialogRenderContext ctx) {
        LabelElement labelElement = Flattener.INPUT.flatten(e).label();
        Component label = labelElement != null ? labelElement.text() : Component.empty();

        String key = ctx.keyFactory().inputKey(index, e.id());
        NumberRangeDialogInput.Builder ret = DialogInput.numberRange(
                key,
                label,
                (float) e.start(),
                (float) e.end()
        );

        if (labelElement != null) {
            ret.labelFormat(labelElement.format());
        }

        Double initial = e.initial();
        if (initial != null) {
            ret.initial(initial.floatValue());
        }

        Double step = e.step();
        if (step != null) {
            ret.step(step.floatValue());
        }

        ret.width(e.width());
        return ret.build();
    }

    private DialogInput renderBooleanInput(BooleanInputElement e, int index, DialogRenderContext ctx) {
        LabelElement labelElement = Flattener.INPUT.flatten(e).label();
        Component label = labelElement != null ? labelElement.text() : Component.empty();
        String key = ctx.keyFactory().inputKey(index, e.id());
        return DialogInput.bool(key, label)
                .initial(e.initial())
                .build();
    }

    private DialogType renderType(
            Flattener.DialogElements children,
            DialogRenderContext ctx
    ) {
        Schema schema = Schema.match(children);
        return switch (schema) {
            case NOTICE -> {
                if (children.footer() != null && !children.footer().children().isEmpty()) {
                    yield DialogType.notice(this.renderActionButton(
                            (ActionElement) children.footer().children().getFirst(),
                            0,
                            true,
                            ctx));
                } else {
                    yield DialogType.notice();
                }
            }
            case CONFIRMATION -> {
                FooterElement footer = children.footer();
                assert footer != null;
                yield DialogType.confirmation(
                        this.renderActionButton(
                                (ActionElement) footer.children().getFirst(),
                                0,
                                true,
                                ctx
                        ),
                        this.renderActionButton(
                                (ActionElement) footer.children().getLast(),
                                1,
                                true,
                                ctx
                        )
                );
            }
            case MULTI_ACTION -> {
                ActionsElement actions = children.actions();
                assert actions != null;
                yield DialogType.multiAction(
                        this.renderActionButtons(actions, ctx),
                        children.footer() != null && !children.footer().children().isEmpty() ?
                                this.renderActionButton(
                                        (ActionElement) children.footer().children().getFirst(),
                                        0,
                                        true,
                                        ctx) :
                                null,
                        actions.columns()
                );
            }
        };
    }

    private List<ActionButton> renderActionButtons(ActionsElement actions, DialogRenderContext ctx) {
        List<Element> children = actions.children();
        int count = children.size();
        List<ActionButton> ret = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ret.add(this.renderActionButton(
                    (ActionElement) children.get(i),
                    i,
                    false,
                    ctx
            ));
        }
        return ret;
    }

    private ActionButton renderActionButton(ActionElement element, int index, boolean footer, DialogRenderContext ctx) {
        DialogAction action;
        switch (element) {
            case ShowDialogActionElement showDialog:
                return this.renderShowDialogActionButton(showDialog, ctx);
            case CustomActionElement custom:
                Key key = ctx.keyFactory().customActionKey(index, footer, custom.id());
                action = DialogAction.customClick(key, null);
                break;
            case OpenUrlActionElement openUrl:
                action = DialogAction.staticAction(ClickEvent.openUrl(openUrl.url()));
                break;
            case RunCommandActionElement runCommand:
                action = DialogAction.staticAction(ClickEvent.runCommand(runCommand.command()));
                break;
            case SuggestCommandActionElement suggestCommand:
                action = DialogAction.staticAction(ClickEvent.suggestCommand(suggestCommand.command()));
                break;
            case CopyToClipboardActionElement copyToClipboard:
                action = DialogAction.staticAction(ClickEvent.copyToClipboard(copyToClipboard.value()));
                break;
        }

        Flattener.ActionElements elements = Flattener.ACTION.flatten(element);
        Component label = elements.label() != null ? elements.label().text() : Component.empty();
        Component tooltip = elements.tooltip() != null ? elements.tooltip().text() : null;

        return ActionButton.create(
                label,
                tooltip,
                element.width(),
                action
        );
    }

    private ActionButton renderShowDialogActionButton(ShowDialogActionElement element, DialogRenderContext ctx) {
        Flattener.ShowDialogActionElements elements = Flattener.SHOW_DIALOG_ACTION.flatten(element);
        Component label = elements.label() != null ? elements.label().text() : Component.empty();
        Component tooltip = elements.tooltip() != null ? elements.tooltip().text() : null;
        return ActionButton.create(
                label,
                tooltip,
                element.width(),
                DialogAction.staticAction(ClickEvent.showDialog(this.render(elements.dialog(), ctx)))
        );
    }

}
