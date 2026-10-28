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

package moe.hendeca.minidialog.element;

import moe.hendeca.minidialog.except.DialogParseException;
import moe.hendeca.minidialog.node.Node;
import moe.hendeca.minidialog.token.Token;
import moe.hendeca.minidialog.token.TokenReader;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.IntFunction;

@NullMarked
@ApiStatus.Internal
public final class ElementParser {

    private static final Adapter<DialogElement> DIALOG_ADAPTER;
    private static final Map<String, Adapter<?>> ADAPTERS;
    static {
        Map<String, Adapter<?>> adapters = HashMap.newHashMap(16);
        adapters.put(DialogElement.NAME, DIALOG_ADAPTER = new Adapter.Dialog());
        adapters.put(TitleElement.NAME, new Adapter.BasicText.Title());
        adapters.put(ExternalTitleElement.NAME, new Adapter.BasicText.ExternalTitle());
        adapters.put(TooltipElement.NAME,       new Adapter.BasicText.Tooltip());
        adapters.put(BodyElement.NAME, new Adapter.BasicStructural.Body());
        adapters.put(InputsElement.NAME, new Adapter.BasicStructural.Inputs());
        adapters.put(MessageElement.NAME, new Adapter.Message());
        adapters.put(ItemElement.NAME, new Adapter.Item());
        adapters.put(DescriptionElement.NAME, new Adapter.Description());
        adapters.put(InputElement.NAME, new Adapter.Input());
        adapters.put(LabelElement.NAME, new Adapter.Label());
        adapters.put(MultilineElement.NAME, new Adapter.Multiline());
        adapters.put(OptionElement.NAME, new Adapter.Option());
        adapters.put(ActionsElement.NAME, new Adapter.Actions());
        adapters.put(ActionElement.NAME, new Adapter.Action());
        adapters.put(FooterElement.NAME, new Adapter.BasicStructural.Footer());
        ADAPTERS = Collections.unmodifiableMap(adapters);
    }

    //

    private final TokenReader reader;
    private boolean prolog;

    public ElementParser(TokenReader reader) {
        this.reader = reader;
        this.prolog = true;
    }

    //

    public DialogElement parse() {
        Token token;
        while (true) {
            token = this.reader.nextToken(this.prolog);
            switch (token) {
                case null -> this.raise("Document is empty");
                case Token.Prolog _, Token.Comment _ -> {}
                case Token.OpenTag t -> {
                    this.prolog = false;
                    if (!DialogElement.NAME.equals(t.name())) this.raise("Document may not begin with a <" + t.name() + "> tag");
                    return this.nextElementWithTokenAndAdapter(t, DIALOG_ADAPTER);
                }
                case Token.Text _ -> this.raise("Unexpected text token");
                case Token.CloseTag c -> this.raise("Unexpected closing tag </" + c.name() + ">");
            }
        }
    }

    private Element nextElementWithToken(Token.OpenTag token) {
        Adapter<?> adapter = ADAPTERS.get(token.name());
        if (adapter == null) this.raise("Unrecognized tag name: " + token.name());
        return this.nextElementWithTokenAndAdapter(token, adapter);
    }

    private <E extends Element> E nextElementWithTokenAndAdapter(Token.OpenTag token, Adapter<E> adapter) {
        AdaptContext ctx = new AdaptContext(this, token);
        AdaptResult<? extends E> result = adapter.adapt(ctx);
        ctx.seal();
        E ret = result.element;
        if (token.selfClosing()) return ret;

        Token next;
        while (true) {
            next = this.reader.nextToken(false);
            switch (next) {
                case null -> this.raise("Tag <" + token.name() + "> is not closed");
                case Token.Prolog _, Token.Comment _ -> {}
                case Token.OpenTag t -> {
                    if (!result.permits.contains(t.name()))
                        this.raise("Tag <" + t.name() + "> is not allowed within <" + token.name() + ">");
                    ret.append(this.nextElementWithToken(t));
                }
                case Token.Text t -> ret.append(Node.text(t.value()));
                case Token.CloseTag t -> {
                    if (t.name().equals(token.name())) return ret;
                    this.raise("Tag <" + token.name() + "> must close before tag <" + t.name() + ">");
                }
            }
        }
    }

    @Contract("_, _ -> fail")
    private void raise(String message, @Nullable Throwable suppressed) throws DialogParseException {
        DialogParseException ex = new DialogParseException(message, this.reader.source().line(), this.reader.source().column());
        if (suppressed != null) ex.addSuppressed(ex);
        throw ex;
    }

    @Contract("_ -> fail")
    private void raise(String message) throws DialogParseException {
        this.raise(message, null);
    }

    //

    private record AdaptResult<E extends Element>(
            E element,
            @Unmodifiable Set<String> permits
    ) {

        public AdaptResult(E element) {
            this(element, Collections.emptySet());
        }

    }

    private static final class AdaptContext {

        private static <T> @UnknownNullability T search(
                String needle,
                String[] haystack,
                int limit,
                IntFunction<@Nullable T> hit,
                IntFunction<@Nullable T> miss
        ) {
            int len = limit;
            int off = 0;
            while (len > 0) {
                int hl = len >>> 1;
                int s = off + hl;
                int c = needle.compareTo(haystack[s]);
                if (c == 0) return hit.apply(s);
                if (c < 0) {
                    len = hl;
                } else {
                    off += hl + 1;
                    len -= hl + 1;
                }
            }
            return miss.apply(off);
        }

        //

        final ElementParser parser;
        final String tagName;
        private final int count;
        private final String[] keys;
        private final String[] values;
        private final BitSet used;

        AdaptContext(ElementParser parser, Token.OpenTag token) {
            Map<String, String> attrs = token.attributes();
            int count = attrs.size();
            String[] keys = new String[count];
            String[] values = new String[count];

            Iterator<Map.Entry<String, String>> iter = attrs.entrySet().iterator();
            int head = 0;
            while (head < count) {
                if (!iter.hasNext()) throw new ConcurrentModificationException();
                Map.Entry<String, String> next = iter.next();
                String key = next.getKey();
                int j = search(
                        key,
                        keys,
                        head,
                        (int _) -> {
                            throw new IllegalStateException("duplicate key: " + key);
                        },
                        Integer::valueOf
                );
                if (j != head) {
                    System.arraycopy(keys, j, keys, j + 1, head - j);
                    System.arraycopy(values, j, values, j + 1, head - j);
                }
                keys[j] = key;
                values[j] = next.getValue();
                head++;
            }

            this.parser = parser;
            this.tagName = token.name();
            this.count = count;
            this.keys = keys;
            this.values = values;
            this.used = new BitSet(count);
        }

        //

        @Contract("_, true -> !null; _, false -> _")
        public @Nullable String get(String key, boolean required) {
            return search(
                    key,
                    this.keys,
                    this.count,
                    (int x) -> {
                        this.used.set(x);
                        return this.values[x];
                    },
                    (int _) -> {
                        if (required) {
                            this.parser.raise("Required property for " + this.tagName + " element is missing: " + key);
                        }
                        return null;
                    }
            );
        }

        @Contract("_, true -> !null; _, false -> _")
        public @Nullable Integer getInt(String key, boolean required) {
            String value = this.get(key, required);
            if (value == null) return null;
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException e) {
                this.parser.raise("Property \"" + key + "\" must be an integer", e);
                return null;
            }
        }

        @Contract("_, true, _ -> !null; _, false, _ -> _")
        public @Nullable Integer getIntAtLeast(String key, boolean required, int min) {
            Integer value = this.getInt(key, required);
            if (value == null) return null;
            if (value < min) {
                if (min == 0) this.parser.raise("Property \"" + key + "\" must not be negative");
                this.parser.raise("Property \"" + key + "\" must be at least " + min);
            }
            return value;
        }

        @Contract("_, true -> !null; _, false -> _")
        public @Nullable Double getFloat(String key, boolean required) {
            String value = this.get(key, required);
            if (value == null) return null;
            try {
                return Double.valueOf(value);
            } catch (NumberFormatException e) {
                this.parser.raise("Property \"" + e + "\" must be a float", e);
                return null;
            }
        }

        @Contract("_, true -> !null; _, false -> _")
        public @Nullable Double getFloatFinite(String key, boolean required) {
            Double ret = this.getFloat(key, required);
            if (ret == null) return null;
            if (Double.isFinite(ret)) return ret;
            this.parser.raise("Property \"" + key + "\" must be a finite number");
            return null;
        }

        @Contract("_, true -> !null; _, false -> _")
        public @Nullable Boolean getBool(String key, boolean required) {
            String value = this.get(key, required);
            if (value == null) return null;
            if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
            if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;
            this.parser.raise("Property \"" + key + "\" must be a boolean");
            return null;
        }

        @SuppressWarnings("PatternValidation")
        @Contract("_, true -> !null; _, false -> _")
        public @Nullable Key getKey(String key, boolean required) {
            String value = this.get(key, required);
            if (value == null) return null;
            try {
                return Key.key(value);
            } catch (InvalidKeyException e) {
                this.parser.raise("Property \"" + key + "\" must be a resource location", e);
                return null;
            }
        }

        public void applyId(Element target) {
            String id = this.get("id", false);
            if (id != null) target.id(id);
        }

        public void seal() {
            int unused = this.used.nextClearBit(0);
            if (unused >= this.count) return;
            StringBuilder msg = new StringBuilder("Unrecognized properties on " + this.tagName + " element: ");
            msg.append(this.keys[unused]);
            while ((unused = this.used.nextClearBit(unused + 1)) < this.count) {
                msg.append(", ");
                msg.append(this.keys[unused]);
            }
            this.parser.raise(msg.toString());
        }

    }

    private static sealed abstract class Adapter<E extends Element> {

        public abstract AdaptResult<? extends E> adapt(AdaptContext ctx) throws DialogParseException;

        //

        static sealed abstract class BasicText<E extends Element> extends Adapter<E> {

            protected abstract E instantiate();

            @Override
            public AdaptResult<E> adapt(AdaptContext ctx) throws DialogParseException {
                E element = this.instantiate();
                ctx.applyId(element);
                return new AdaptResult<>(element);
            }

            //

            static final class Title extends BasicText<TitleElement> {

                @Override
                protected TitleElement instantiate() {
                    return new TitleElement();
                }

            }

            static final class ExternalTitle extends BasicText<ExternalTitleElement> {

                @Override
                protected ExternalTitleElement instantiate() {
                    return new ExternalTitleElement();
                }

            }

            static final class Tooltip extends BasicText<TooltipElement> {

                @Override
                protected TooltipElement instantiate() {
                    return new TooltipElement();
                }

            }

        }

        static sealed abstract class BasicStructural<E extends Element> extends Adapter<E> {

            private final Set<String> permits;

            protected BasicStructural(String... permits) {
                this.permits = Set.of(permits);
            }

            //

            protected abstract E instantiate();

            @Override
            public AdaptResult<E> adapt(AdaptContext ctx) throws DialogParseException {
                E element = this.instantiate();
                ctx.applyId(element);
                return new AdaptResult<>(element, this.permits);
            }

            //

            static final class Body extends BasicStructural<BodyElement> {

                Body() {
                    super(MessageElement.NAME, ItemElement.NAME);
                }

                @Override
                protected BodyElement instantiate() {
                    return new BodyElement();
                }

            }

            static final class Inputs extends BasicStructural<InputsElement> {

                Inputs() {
                    super(InputElement.NAME);
                }

                @Override
                protected InputsElement instantiate() {
                    return new InputsElement();
                }

            }

            static final class Footer extends BasicStructural<FooterElement> {

                Footer() {
                    super(ActionElement.NAME);
                }

                @Override
                protected FooterElement instantiate() {
                    return new FooterElement();
                }

            }

        }

        static final class Dialog extends Adapter<DialogElement> {

            private static final Set<String> PERMITS = Set.of(
                    TitleElement.NAME,
                    ExternalTitleElement.NAME,
                    BodyElement.NAME,
                    InputsElement.NAME,
                    ActionsElement.NAME,
                    FooterElement.NAME
            );

            @Override
            public AdaptResult<DialogElement> adapt(AdaptContext ctx) throws DialogParseException {
                DialogElement ret = new DialogElement();
                ctx.applyId(ret);

                Integer version = ctx.getInt("version", false);
                if (version != null) {
                    if (version != 1) ctx.parser.raise("Unsupported dialog version " + version);
                    ret.version(version);
                }

                Boolean canCloseWithEscape = ctx.getBool("canCloseWithEscape", false);
                if (canCloseWithEscape != null) ret.canCloseWithEscape(canCloseWithEscape);

                Boolean pause = ctx.getBool("pause", false);
                if (pause != null) ret.pause(pause);

                String afterAction = ctx.get("afterAction", false);
                if (afterAction != null) {
                    DialogElement.AfterAction action = DialogElement.AfterAction.fromXmlName(afterAction);
                    if (action == null) ctx.parser.raise("Unrecognized afterAction constant \"" + afterAction + "\"");
                    ret.afterAction(action);
                }

                return new AdaptResult<>(ret, PERMITS);
            }

        }

        static final class Message extends Adapter<MessageElement> {

            @Override
            public AdaptResult<MessageElement> adapt(AdaptContext ctx) throws DialogParseException {
                MessageElement ret = new MessageElement();
                ctx.applyId(ret);

                Integer width = ctx.getIntAtLeast("width", false, 1);
                if (width != null) ret.width(width);

                return new AdaptResult<>(ret);
            }

        }

        static final class Item extends Adapter<ItemElement> {

            private static final Set<String> PERMITS = Set.of(DescriptionElement.NAME);

            @Override
            public AdaptResult<ItemElement> adapt(AdaptContext ctx) throws DialogParseException {
                Key src = ctx.getKey("src", false);
                ItemElement ret = new ItemElement(src == null ? Key.key("minecraft:air") : src);
                ctx.applyId(ret);

                Integer count = ctx.getIntAtLeast("count", false, 0);
                if (count != null) ret.count(count);

                Integer width = ctx.getIntAtLeast("width", false, 1);
                if (width != null) ret.width(width);

                Integer height = ctx.getIntAtLeast("height", false, 1);
                if (height != null) ret.height(height);

                Boolean showDecoration = ctx.getBool("showDecoration", false);
                if (showDecoration != null) ret.showDecoration(showDecoration);

                Boolean showTooltip = ctx.getBool("showTooltip", false);
                if (showTooltip != null) ret.showTooltip(showTooltip);

                return new AdaptResult<>(ret, PERMITS);
            }

        }

        static final class Description extends Adapter<DescriptionElement> {

            @Override
            public AdaptResult<DescriptionElement> adapt(AdaptContext ctx) throws DialogParseException {
                DescriptionElement ret = new DescriptionElement();
                ctx.applyId(ret);

                Integer width = ctx.getIntAtLeast("width", false, 1);
                if (width != null) ret.width(width);

                return new AdaptResult<>(ret);
            }
        }

        static final class Input extends Adapter<InputElement> {

            private static final Set<String> PERMITS_TEXT = Set.of(LabelElement.NAME, MultilineElement.NAME);
            private static final Set<String> PERMITS_OPTION = Set.of(LabelElement.NAME, OptionElement.NAME);
            private static final Set<String> PERMITS_OTHER = Set.of(LabelElement.NAME);

            @Override
            public AdaptResult<? extends InputElement> adapt(AdaptContext ctx) throws DialogParseException {
                String type = ctx.get("type", true);
                return switch (type.toLowerCase(Locale.ROOT)) {
                    case "text" -> this.adaptText(ctx);
                    case "boolean" -> this.adaptBoolean(ctx);
                    case "option" -> this.adaptOption(ctx);
                    case "range" -> this.adaptRange(ctx);
                    default -> {
                        ctx.parser.raise("Unrecognized type constant \"" + type + "\"");
                        yield null;
                    }
                };
            }

            private AdaptResult<TextInputElement> adaptText(AdaptContext ctx) throws DialogParseException {
                TextInputElement ret = new TextInputElement();
                ctx.applyId(ret);

                Integer width = ctx.getIntAtLeast("width", false, 1);
                if (width != null) ret.width(width);

                String initial = ctx.get("initial", false);
                if (initial != null) ret.initial(initial);

                Integer maxLength = ctx.getIntAtLeast("maxLength", false, 0);
                if (maxLength != null) ret.maxLength(maxLength);

                return new AdaptResult<>(ret, PERMITS_TEXT);
            }

            private AdaptResult<BooleanInputElement> adaptBoolean(AdaptContext ctx) throws DialogParseException {
                BooleanInputElement ret = new BooleanInputElement();
                ctx.applyId(ret);

                Boolean initial = ctx.getBool("initial", false);
                if (initial != null) ret.initial(initial);

                return new AdaptResult<>(ret, PERMITS_OTHER);
            }

            private AdaptResult<OptionInputElement> adaptOption(AdaptContext ctx) throws DialogParseException {
                OptionInputElement ret = new OptionInputElement();
                ctx.applyId(ret);

                Integer width = ctx.getIntAtLeast("width", false, 1);
                if (width != null) ret.width(width);

                String initial = ctx.get("initial", false);
                if (initial != null) ret.initial(initial);

                return new AdaptResult<>(ret, PERMITS_OPTION);
            }

            private AdaptResult<RangeInputElement> adaptRange(AdaptContext ctx) throws DialogParseException {
                double start = ctx.getFloatFinite("start", true);
                double end = ctx.getFloatFinite("end", true);
                if (end < start) ctx.parser.raise("Range end may not be less than range start");

                RangeInputElement ret = new RangeInputElement(start, end);
                ctx.applyId(ret);

                Integer width = ctx.getIntAtLeast("width", false, 1);
                if (width != null) ret.width(width);

                Double initial = ctx.getFloatFinite("initial", false);
                if (initial != null) {
                    if (initial < start || initial > end) ctx.parser.raise("Initial value must be within range");
                    ret.initial(initial);
                }

                Double step = ctx.getFloatFinite("step", false);
                if (step != null) {
                    if (step <= 0) ctx.parser.raise("Step must be greater than 0");
                    ret.step(step);
                }

                return new AdaptResult<>(ret, PERMITS_OTHER);
            }

        }

        static final class Label extends Adapter<LabelElement> {

            @Override
            public AdaptResult<LabelElement> adapt(AdaptContext ctx) throws DialogParseException {
                LabelElement ret = new LabelElement();
                ctx.applyId(ret);

                Boolean visible = ctx.getBool("visible", false);
                if (visible != null) ret.visible(visible);

                String format = ctx.get("format", false);
                if (format != null) ret.format(format);

                return new AdaptResult<>(ret);
            }
        }

        static final class Multiline extends Adapter<MultilineElement> {

            @Override
            public AdaptResult<MultilineElement> adapt(AdaptContext ctx) throws DialogParseException {
                MultilineElement ret = new MultilineElement();
                ctx.applyId(ret);

                Integer maxLines = ctx.getIntAtLeast("maxLines", false, 0);
                if (maxLines != null) ret.maxLines(maxLines);

                Integer height = ctx.getIntAtLeast("height", false, 1);
                if (height != null) ret.height(height);

                return new AdaptResult<>(ret);
            }

        }

        static final class Option extends Adapter<OptionElement> {

            @Override
            public AdaptResult<OptionElement> adapt(AdaptContext ctx) throws DialogParseException {
                OptionElement ret = new OptionElement();
                ctx.applyId(ret);

                String value = ctx.get("value", false);
                if (value != null) ret.value(value);

                return new AdaptResult<>(ret);
            }

        }

        static final class Actions extends Adapter<ActionsElement> {

            private static final Set<String> PERMITS = Set.of(ActionElement.NAME);

            @Override
            public AdaptResult<ActionsElement> adapt(AdaptContext ctx) throws DialogParseException {
                ActionsElement ret = new ActionsElement();
                ctx.applyId(ret);

                Integer columns = ctx.getIntAtLeast("columns", false, 1);
                if (columns != null) ret.columns(columns);

                return new AdaptResult<>(ret, PERMITS);
            }

        }

        static final class Action extends Adapter<ActionElement> {

            private static final Set<String> PERMITS_DIALOG = Set.of(LabelElement.NAME, TooltipElement.NAME, DialogElement.NAME);
            private static final Set<String> PERMITS_OTHER = Set.of(LabelElement.NAME, TooltipElement.NAME);

            @Override
            public AdaptResult<? extends ActionElement> adapt(AdaptContext ctx) throws DialogParseException {
                String type = ctx.get("type", true);
                return switch (type) {
                    case "openUrl" -> this.adaptOpenUrl(ctx);
                    case "runCommand" -> this.adaptRunCommand(ctx);
                    case "suggestCommand" -> this.adaptSuggestCommand(ctx);
                    case "copyToClipboard" -> this.adaptCopyToClipboard(ctx);
                    case "showDialog" -> this.adaptShowDialog(ctx);
                    case "custom" -> this.adaptCustom(ctx);
                    default -> {
                        ctx.parser.raise("Unrecognized type constant \"" + type + "\"");
                        yield null;
                    }
                };
            }

            private AdaptResult<OpenUrlActionElement> adaptOpenUrl(AdaptContext ctx) throws DialogParseException {
                String value = ctx.get("value", true);
                OpenUrlActionElement ret = new OpenUrlActionElement(value);
                applyIdWidth(ctx, ret);
                return new AdaptResult<>(ret, PERMITS_OTHER);
            }

            private AdaptResult<RunCommandActionElement> adaptRunCommand(AdaptContext ctx) throws DialogParseException {
                String value = ctx.get("value", true);
                RunCommandActionElement ret = new RunCommandActionElement(value);
                applyIdWidth(ctx, ret);
                return new AdaptResult<>(ret, PERMITS_OTHER);
            }

            private AdaptResult<SuggestCommandActionElement> adaptSuggestCommand(AdaptContext ctx) throws DialogParseException {
                String value = ctx.get("value", true);
                SuggestCommandActionElement ret = new SuggestCommandActionElement(value);
                applyIdWidth(ctx, ret);
                return new AdaptResult<>(ret, PERMITS_OTHER);
            }

            private AdaptResult<CopyToClipboardActionElement> adaptCopyToClipboard(AdaptContext ctx) throws DialogParseException {
                String value = ctx.get("value", true);
                CopyToClipboardActionElement ret = new CopyToClipboardActionElement(value);
                applyIdWidth(ctx, ret);
                return new AdaptResult<>(ret, PERMITS_OTHER);
            }

            private AdaptResult<ShowDialogActionElement> adaptShowDialog(AdaptContext ctx) throws DialogParseException {
                ShowDialogActionElement ret = new ShowDialogActionElement();
                applyIdWidth(ctx, ret);
                return new AdaptResult<>(ret, PERMITS_DIALOG);
            }

            private AdaptResult<CustomActionElement> adaptCustom(AdaptContext ctx) throws DialogParseException {
                CustomActionElement ret = new CustomActionElement();
                applyIdWidth(ctx, ret);
                return new AdaptResult<>(ret, PERMITS_OTHER);
            }

            private static void applyIdWidth(AdaptContext ctx, ActionElement element) throws DialogParseException {
                ctx.applyId(element);
                Integer width = ctx.getIntAtLeast("width", false, 1);
                if (width == null) return;
                element.width(width);
            }

        }

    }

}
