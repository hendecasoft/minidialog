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

import moe.hendeca.minidialog.element.*;
import moe.hendeca.minidialog.except.DialogRenderException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.modcommon.MinecraftAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.ItemBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.input.BooleanInput;
import net.minecraft.server.dialog.input.NumberRangeInput;
import net.minecraft.server.dialog.input.SingleOptionInput;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.*;

@NullMarked
@ApiStatus.Internal
public final class VanillaDialogRenderer extends AbstractDialogRenderer<Holder<Dialog>> {

    private static final ResourceKey<Registry<Dialog>> DIALOG_REGISTRY = ResourceKey.createRegistryKey(Registries.DIALOG.identifier());
    private static final ResourceKey<Registry<Item>> ITEM_REGISTRY = ResourceKey.createRegistryKey(Registries.ITEM.identifier());
    private static final Identifier AIR_ITEM = Identifier.fromNamespaceAndPath(Identifier.DEFAULT_NAMESPACE, "air");

    //

    private final MinecraftAudiences audiences;
    private final @Nullable RegistryAccess registryAccess;

    public VanillaDialogRenderer(
            MinecraftAudiences audiences,
            @Nullable RegistryAccess registryAccess
    ) {
        this.audiences = audiences;
        this.registryAccess = registryAccess;
    }

    //

    @Override
    public Holder<Dialog> render(DialogElement element, DialogRenderContext ctx) throws DialogRenderException {
        // Reference
        Key src = element.src();
        if (src != null) {
            if (!element.isReference()) {
                throw new DialogRenderException("dialog may not have other properties or children when src is set");
            }
            Holder<Dialog> reference = dialogReference(this.registryAccess, src);
            if (reference == null) {
                throw new DialogRenderException("invalid dialog key " + src + " (not found in registry)");
            }
            return reference;
        }

        // Non-reference
        Flattener.DialogElements children = Flattener.DIALOG.flatten(element);
        Schema schema = Schema.match(children);
        return Holder.direct(switch (schema) {
            case NOTICE -> new NoticeDialog(
                    this.renderCommon(element, children, ctx),
                    children.footer() == null || children.footer().children().isEmpty() ?
                            NoticeDialog.DEFAULT_ACTION :
                            this.renderAction(
                                    (ActionElement) children.footer().children().getFirst(),
                                    0,
                                    true,
                                    ctx
                            )
            );
            case CONFIRMATION -> {
                FooterElement footer = children.footer();
                assert footer != null;
                yield new ConfirmationDialog(
                        this.renderCommon(element, children, ctx),
                        this.renderAction(
                                (ActionElement) footer.children().getFirst(),
                                0,
                                true,
                                ctx
                        ),
                        this.renderAction(
                                (ActionElement) footer.children().get(1),
                                1,
                                true,
                                ctx
                        )
                );
            }
            case MULTI_ACTION -> {
                CommonDialogData common = this.renderCommon(element, children, ctx);
                ActionsElement actions = children.actions();
                FooterElement footer = children.footer();
                assert actions != null;

                List<ActionButton> buttons = new ArrayList<>(actions.children().size());
                for (Element child : actions.children()) {
                    if (!(child instanceof ActionElement action)) continue;
                    buttons.add(this.renderAction(
                            action,
                            actions.indexOf(action),
                            false,
                            ctx
                    ));
                }

                Optional<ActionButton> exitAction;
                if (footer != null && !footer.children().isEmpty()) {
                    exitAction = Optional.of(this.renderAction(
                            (ActionElement) footer.children().getFirst(),
                            0,
                            true,
                            ctx
                    ));
                } else {
                    exitAction = Optional.empty();
                }

                yield new MultiActionDialog(
                        common,
                        buttons,
                        exitAction,
                        actions.columns()
                );
            }
        });
    }

    private CommonDialogData renderCommon(
            DialogElement element,
            Flattener.DialogElements children,
            DialogRenderContext ctx
    ) throws DialogRenderException {
        List<? extends Element> tmp;

        List<DialogBody> body;
        if (children.body() != null && !(tmp = children.body().children()).isEmpty()) {
            body = new ArrayList<>(tmp.size());
            for (Element child : tmp) {
                if (child instanceof MessageElement msg) {
                    body.add(this.renderMessage(msg));
                } else if (child instanceof ItemElement itm) {
                    body.add(this.renderItem(itm));
                }
            }
        } else {
            body = Collections.emptyList();
        }

        List<Input> inputs;
        if (children.inputs() != null && !(tmp = children.inputs().children()).isEmpty()) {
            inputs = new ArrayList<>(tmp.size());
            for (Element child : tmp) {
                if (!(child instanceof InputElement input)) continue;
                int index = children.inputs().indexOf(input);
                inputs.add(this.renderInput(input, index, ctx));
            }
        } else {
            inputs = Collections.emptyList();
        }

        return new CommonDialogData(
                this.audiences.asNative(children.title() != null ? children.title().text() : Component.empty()),
                children.externalTitle() != null ?
                        Optional.of(this.audiences.asNative(children.externalTitle().text())) :
                        Optional.empty(),
                element.canCloseWithEscape(),
                element.pause(),
                this.renderAfterAction(element.afterAction()),
                body,
                inputs
        );
    }

    private DialogAction renderAfterAction(DialogElement.AfterAction action) {
        return switch (action) {
            case NONE -> DialogAction.NONE;
            case WAIT_FOR_RESPONSE -> DialogAction.WAIT_FOR_RESPONSE;
            case CLOSE -> DialogAction.CLOSE;
        };
    }

    private PlainMessage renderMessage(MessageElement element) {
        return new PlainMessage(
                this.audiences.asNative(element.text()),
                element.width()
        );
    }

    private ItemBody renderItem(ItemElement item) {
        Flattener.ItemElements children = Flattener.ITEM.flatten(item);

        Item resolvedItem = item(this.registryAccess, item.src());
        if (resolvedItem == null) resolvedItem = BuiltInRegistries.ITEM.getValue(AIR_ITEM);
        ItemStackTemplate template = new ItemStackTemplate(resolvedItem, item.count());

        Optional<PlainMessage> description;
        if (children.description() != null) {
            description = Optional.of(new PlainMessage(
                    this.audiences.asNative(children.description().text()),
                    children.description().width()
            ));
        } else {
            description = Optional.empty();
        }

        return new ItemBody(
                template,
                description,
                item.showDecoration(),
                item.showTooltip(),
                item.width(),
                item.height()
        );
    }

    private Input renderInput(InputElement input, int index, DialogRenderContext ctx) {
        String key = ctx.keyFactory().inputKey(index, input.id());
        return new Input(key, switch (input) {
            case TextInputElement t -> this.renderTextInputControl(t);
            case RangeInputElement r -> this.renderRangeInputControl(r);
            case BooleanInputElement b -> this.renderBooleanInputControl(b);
            case OptionInputElement o -> this.renderOptionInputControl(o, ctx);
        });
    }

    private TextInput renderTextInputControl(TextInputElement element) {
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

        Optional<TextInput.MultilineOptions> multiline;
        if (children.multiline() == null) {
            multiline = Optional.empty();
        } else {
            multiline = Optional.of(new TextInput.MultilineOptions(
                    Optional.ofNullable(children.multiline().maxLines()),
                    Optional.ofNullable(children.multiline().height())
            ));
        }

        return new TextInput(
                element.width(),
                this.audiences.asNative(label),
                labelVisible,
                element.initial() == null ? "" : element.initial(),
                element.maxLength(),
                multiline
        );
    }

    private NumberRangeInput renderRangeInputControl(RangeInputElement element) {
        LabelElement labelElement = Flattener.INPUT.flatten(element).label();

        Component label;
        String labelFormat;
        if (labelElement == null) {
            label = Component.empty();
            labelFormat = "options.generic_value";
        } else {
            label = labelElement.text();
            labelFormat = labelElement.format();
        }

        return new NumberRangeInput(
                element.width(),
                this.audiences.asNative(label),
                labelFormat,
                new NumberRangeInput.RangeInfo(
                        (float) element.start(),
                        (float) element.end(),
                        element.initial() == null ? Optional.empty() : Optional.of(element.initial().floatValue()),
                        element.step() == null ? Optional.empty() : Optional.of(element.step().floatValue())
                )
        );
    }

    private BooleanInput renderBooleanInputControl(BooleanInputElement element) {
        LabelElement labelElement = Flattener.INPUT.flatten(element).label();
        Component label = labelElement == null ? Component.empty() : labelElement.text();
        return new BooleanInput(
                this.audiences.asNative(label),
                element.initial(),
                "true",
                "false"
        );
    }

    private SingleOptionInput renderOptionInputControl(OptionInputElement element, DialogRenderContext ctx) {
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

        List<OptionElement> optionElements = children.options();
        if (optionElements.isEmpty()) throw new DialogRenderException("Option input element has no option elements");

        List<SingleOptionInput.Entry> entries = new ArrayList<>(optionElements.size());
        String initialValue = element.initial();
        boolean permitInitial = true;
        for (OptionElement optionElement : optionElements) {
            int index = element.indexOf(optionElement);
            String key = ctx.keyFactory().optionKey(index, optionElement.id());

            boolean initial;
            if (permitInitial) {
                initial = Objects.equals(initialValue, optionElement.value());
                if (initial) permitInitial = false;
            } else {
                initial = false;
            }

            entries.add(new SingleOptionInput.Entry(
                    key,
                    Optional.of(this.audiences.asNative(optionElement.text())),
                    initial
            ));
        }

        return new SingleOptionInput(
                element.width(),
                entries,
                this.audiences.asNative(label),
                labelVisible
        );
    }

    private ActionButton renderAction(
            ActionElement element,
            int index,
            boolean footer,
            DialogRenderContext ctx
    ) {
        return switch (element) {
            case OpenUrlActionElement q -> {
                URI uri;
                try {
                    uri = URI.create(q.url());
                } catch (IllegalArgumentException e) {
                    throw new DialogRenderException("\"" + q.url() + "\" is not a valid URI", e);
                }
                yield new ActionButton(
                        this.renderCommonButtonData(element),
                        Optional.of(new StaticAction(new ClickEvent.OpenUrl(uri))));
            }
            case RunCommandActionElement q -> new ActionButton(
                    this.renderCommonButtonData(element),
                    Optional.of(new StaticAction(new ClickEvent.RunCommand(q.command()))));
            case SuggestCommandActionElement q -> new ActionButton(
                    this.renderCommonButtonData(element),
                    Optional.of(new StaticAction(new ClickEvent.SuggestCommand(q.command()))));
            case CopyToClipboardActionElement q -> new ActionButton(
                    this.renderCommonButtonData(element),
                    Optional.of(new StaticAction(new ClickEvent.CopyToClipboard(q.value()))));
            case CustomActionElement q -> {
                Key key = ctx.keyFactory().customActionKey(index, footer, q.id());
                yield new ActionButton(
                        this.renderCommonButtonData(element),
                        Optional.of(new CustomAll(
                                Identifier.fromNamespaceAndPath(key.namespace(), key.value()),
                                Optional.empty()
                        )));
            }
            case ShowDialogActionElement q -> {
                Flattener.ShowDialogActionElements children = Flattener.SHOW_DIALOG_ACTION.flatten(q);
                yield new ActionButton(
                        this.renderCommonButtonData(element, children),
                        Optional.of(new StaticAction(new ClickEvent.ShowDialog(
                                this.render(children.dialog(), ctx)
                        )))
                );
            }
        };
    }

    private CommonButtonData renderCommonButtonData(ActionElement element) {
        return this.renderCommonButtonData(element, Flattener.ACTION.flatten(element));
    }

    private CommonButtonData renderCommonButtonData(ActionElement element, Flattener.ActionElements children) {
        return new CommonButtonData(
                this.audiences.asNative(children.label() == null ? Component.empty() : children.label().text()),
                element.width()
        );
    }

    private static @Nullable Holder<Dialog> dialogReference(@Nullable RegistryAccess access, Key src) {
        if (access == null) throw new DialogRenderException("Cannot resolve dialog reference without registry");
        return access.lookup(DIALOG_REGISTRY)
                .flatMap(r -> r.get(Identifier.fromNamespaceAndPath(src.namespace(), src.value())))
                .orElse(null);
    }

    private static @Nullable Item item(@Nullable RegistryAccess access, Key src) {
        if (access == null) throw new DialogRenderException("Cannot resolve dialog reference without registry");
        return access.lookup(ITEM_REGISTRY)
                .flatMap(r -> r.get(Identifier.fromNamespaceAndPath(src.namespace(), src.value())))
                .map(Holder.Reference::value)
                .orElse(null);
    }

}
