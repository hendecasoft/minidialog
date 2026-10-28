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

package moe.hendeca.minidialog;

import moe.hendeca.minidialog.util.DialogTemplateArgument;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.security.SecureRandom;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NullMarked
@ApiStatus.Internal
public final class TokenizedDialogKeyFactory implements DialogKeyFactory {

    public static final @KeyPattern.Namespace String NAMESPACE = "minidialog";
    public static final @KeyPattern.Value String PREFIX_INPUT = "input";
    public static final @KeyPattern.Value String PREFIX_OPTION = "option";
    public static final @KeyPattern.Value String PREFIX_ACTION = "action";
    public static final @KeyPattern.Value String PREFIX_BUTTON = "button";

    private static final Random RANDOM = new SecureRandom();

    private static final Pattern VALUE_PATTERN =
            Pattern.compile("^([\\da-v]{13})_(input|option|action|button)_(\\d+)(?:_((?:\\d|[a-z]|_(?:_|[a-z]|[\\da-f]{4}))+))?$");

    //

    @SuppressWarnings("PatternValidation")
    private static @DialogTemplateArgument String idEscape(String id) {
        int len = id.length();
        StringBuilder ret = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char c = id.charAt(i);
            if (('0' <= c && c <= '9') ||
                    ('a' <= c && c <= 'z')
            ) {
                ret.append(c);
                continue;
            }
            if (c == '_') {
                ret.append("__");
                continue;
            }
            ret.append('_');
            if ('A' <= c && c <= 'Z') {
                ret.append((char) (c + 32));
                continue;
            }
            String u = Integer.toString(c & 0xFFFF, 16);
            ret.repeat('0', 4 - u.length());
            ret.append(u);
        }

        return ret.toString();
    }

    @Contract("null -> null; !null -> !null")
    private static @Nullable String idUnescape(@Nullable String id) {
        if (id == null) return null;
        int len = id.length();
        StringBuilder ret = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char c = id.charAt(i);
            if (('0' <= c && c <= '9') ||
                    ('a' <= c && c <= 'z')
            ) {
                ret.append(c);
                continue;
            }
            if (c != '_' || (i + 1) >= len) throw new IllegalArgumentException();
            char x = id.charAt(++i);
            if (x == '_') {
                ret.append('_');
                continue;
            }
            if ('a' <= x && x <= 'z') {
                ret.append((char) (x - 32));
                continue;
            }
            if ((i + 3) >= len) throw new IllegalArgumentException();
            String s = new String(new char[] {
                    x,
                    id.charAt(++i),
                    id.charAt(++i),
                    id.charAt(++i)
            });;
            ret.append((char) Integer.parseInt(s, 16));
        }

        return ret.toString();
    }

    @SuppressWarnings("PatternValidation")
    private static @DialogTemplateArgument String composeValue(
            @DialogTemplateArgument String token,
            @MagicConstant(stringValues = { PREFIX_INPUT, PREFIX_OPTION, PREFIX_ACTION, PREFIX_BUTTON }) String prefix,
            int index,
            @Nullable String id
    ) {
        StringBuilder value = new StringBuilder();
        value.append(token);
        value.append('_');
        value.append(prefix);
        value.append('_');
        value.append(index);
        if (id != null) {
            value.append('_');
            value.append(idEscape(id));
        }
        return value.toString();
    }

    @SuppressWarnings("PatternValidation")
    private static Key compose(
            @DialogTemplateArgument String token,
            @MagicConstant(stringValues = { PREFIX_INPUT, PREFIX_OPTION, PREFIX_ACTION, PREFIX_BUTTON }) String prefix,
            int index,
            @Nullable String id
    ) {
        return Key.key(NAMESPACE, composeValue(token, prefix, index, id));
    }

    @SuppressWarnings("PatternValidation")
    private static synchronized @DialogTemplateArgument String nextToken() {
        long k = RANDOM.nextLong();
        char[] chars = new char[13];
        for (int i = 0; i < 13; i++) {
            int n = (int) (k & 31L);
            k >>= 5;
            chars[i] = n < 10 ?
                    (char) ('0' + n) :
                    (char) ('a' + (n - 10));
        }
        return new String(chars);
    }

    //

    private final @DialogTemplateArgument String token;

    public TokenizedDialogKeyFactory() {
        this.token = nextToken();
    }

    //

    @Contract(pure = true)
    public @DialogTemplateArgument String token() {
        return this.token;
    }

    @Override
    @SuppressWarnings("PatternValidation")
    public @DialogTemplateArgument String inputKey(int index, @Nullable String id) {
        return composeValue(this.token, PREFIX_INPUT, index, id);
    }

    @Override
    @SuppressWarnings("PatternValidation")
    public @DialogTemplateArgument String optionKey(int index, @Nullable String id) {
        return composeValue(this.token, PREFIX_OPTION, index, id);
    }

    @Override
    public Key customActionKey(int index, boolean footer, @Nullable String id) {
        return compose(this.token, footer ? PREFIX_BUTTON : PREFIX_ACTION, index, id);
    }

    public @Nullable ParseResult parse(Key key) {
        if (!NAMESPACE.equals(key.namespace())) return null;
        return this.parse(key.value());
    }

    public @Nullable ParseResult parse(String key) {
        Matcher m = VALUE_PATTERN.matcher(key);
        if (!m.matches()) return null;
        if (!this.token.equals(m.group(1))) return null;

        ParseResult.Type type = switch (m.group(2)) {
            case "input" -> ParseResult.Type.INPUT;
            case "option" -> ParseResult.Type.OPTION;
            case "action" -> ParseResult.Type.ACTION;
            case "button" -> ParseResult.Type.BUTTON;
            default -> null;
        };
        if (type == null) return null;

        int index;
        try {
            index = Integer.parseInt(m.group(3));
        } catch (NumberFormatException ignored) {
            return null;
        }

        String id = (m.groupCount() > 3 && m.group(4) != null && !m.group(4).isEmpty()) ? m.group(4) : null;
        return new ParseResult(type, index, idUnescape(id));
    }

    //

    public record ParseResult(
            Type type,
            int index,
            @Nullable String id
    ) {

        public enum Type {
            INPUT,
            OPTION,
            ACTION,
            BUTTON
        }

    }

}
