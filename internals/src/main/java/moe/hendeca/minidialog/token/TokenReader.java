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

package moe.hendeca.minidialog.token;

import moe.hendeca.minidialog.except.DialogException;
import moe.hendeca.minidialog.except.DialogIOException;
import moe.hendeca.minidialog.except.DialogParseException;
import moe.hendeca.minidialog.util.LimitedTagResolverFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.function.BiConsumer;

@NullMarked
@ApiStatus.Internal
public final class TokenReader {

    private static final Map<String, TextMode> TEXT_TAGS = Map.of(
            "title", TextMode.FULL,
            "externalTitle", TextMode.FULL,
            "message", TextMode.LIMITED,
            "description", TextMode.LIMITED,
            "label", TextMode.FULL,
            "option", TextMode.FULL,
            "tooltip", TextMode.FULL
    );

    private static final String[] ENTITIES = new String[] {
            "amp", "&",
            "lt", "<",
            "gt", ">",
            "apos", "'",
            "quot", "\""
    };

    private static final String COMMENT_END = "-->";

    private static final MiniMessage MINI_FULL = MiniMessage.builder()
            .build();

    private static final MiniMessage MINI_LIMITED = MiniMessage.builder()
            .tags(LimitedTagResolverFactory.limitedTagResolver())
            .build();

    //

    private final Source source;
    private TextMode textMode;
    private @Nullable String textClosedBy;
    private @Nullable EnterTagInfo queuedTag;

    public TokenReader(Source source) {
        this.source = source;
        this.textMode = TextMode.NONE;
        this.textClosedBy = null;
        this.queuedTag = null;
    }

    //

    @Contract(pure = true)
    public Source source() {
        return this.source;
    }

    public @Nullable Token nextToken(boolean prolog) {
        // Read text if in text mode
        TextMode textMode = this.textMode;
        if (textMode != TextMode.NONE) {
            Token ret = this.nextText(textMode == TextMode.FULL ? MINI_FULL : MINI_LIMITED);
            this.textMode = TextMode.NONE;
            this.textClosedBy = null;
            return ret;
        }

        // Open the next tag
        EnterTagInfo info = this.queuedTag;
        if (info == null) {
            info = this.enterTag();
            if (info == null) return null;
        } else {
            this.queuedTag = null;
        }

        // Handle special tag types
        String tagName = info.name;
        boolean closing;
        if (tagName.charAt(0) == '?') {
            if (!prolog) this.source.raise("Markup declaration must be placed at start of file");
            if (info.terminatedBy == '>') this.source.raise("Processing instruction not terminated");
            if (!tagName.equals("?xml")) this.source.raise("Unsupported declaration: <" + tagName + ">");
            if (info.terminatedBy == ' ') {
                final @Nullable String[] encoding = new String[1];
                this.readTagAttrs((String key, String value) -> {
                    if ("encoding".equals(key)) encoding[0] = value;
                }, true);
                if (encoding[0] != null && !encoding[0].equalsIgnoreCase("UTF-8")) {
                    this.source.raise("Unsupported encoding: " + encoding[0]);
                }
            }
            return Token.prolog();
        } else if (tagName.charAt(0) == '!') {
            if (tagName.startsWith("!--")) {
                this.consumeComment(info);
                return Token.comment();
            }
            if (!prolog) this.source.raise("Markup declaration must be placed at start of file");
            if (!tagName.equals("!DOCTYPE")) this.source.raise("Unsupported declaration: <" + tagName + ">");
            if (info.terminatedBy == '>') return Token.prolog();
            int c;
            while (true) {
                c = this.source.next();
                if (c == -1) this.source.raise("Declaration is not closed");
                if (c == '"') {
                    this.finishString(null);
                } else if (c == '>') {
                    return Token.prolog();
                }
            }
        } else if (tagName.charAt(0) == '/') {
            if (tagName.length() == 1) this.source.raise("Closing tag name expected");
            closing = true;
        } else {
            closing = false;
        }

        // Immediately closed tag
        if (info.terminatedBy == '>') {
            if (closing) return Token.closeTag(tagName.substring(1));
            if (tagName.endsWith("/")) return Token.openTag(tagName.substring(0, tagName.length() - 1)).selfClosing(true).build();
            TextMode tm = TEXT_TAGS.get(tagName);
            if (tm != null) {
                this.textMode = tm;
                this.textClosedBy = tagName;
            }
            return Token.openTag(tagName).build();
        } else if (closing) {
            int x = this.nextNonWhitespace();
            if (x == -1) this.source.raise("Tag is not closed");
            if (x != '>') this.source.raise("Closing tag may not have attributes");
            return Token.closeTag(tagName.substring(1));
        }

        // Tag with properties
        Token.OpenTag.Builder b = Token.openTag(tagName);
        boolean selfClosing = this.readTagAttrs(b::attribute, false);
        b.selfClosing(selfClosing);
        if (!selfClosing) {
            TextMode tm = TEXT_TAGS.get(tagName);
            if (tm != null) {
                this.textMode = tm;
                this.textClosedBy = tagName;
            }
        }
        return b.build();
    }

    private Token.Text nextText(MiniMessage mini) {
        StringBuilder buf = new StringBuilder();
        Queue<Integer> carry = new LinkedList<>();
        int ws = 0;
        boolean newline = false;
        int c;
        while (true) {
            c = carry.isEmpty() ? this.source.next() : carry.poll();
            if (c == -1) break;
            if (c == ' ') {
                ws++;
                continue;
            }
            if (c == '\n') {
                ws++;
                newline = true;
                continue;
            }
            if (ws != 0) {
                if (!buf.isEmpty()) {
                    if (newline) {
                        buf.append(' ');
                    } else {
                        buf.repeat(" ", ws);
                    }
                }
                ws = 0;
                newline = false;
            }
            if (c == '\\') {
                c = this.source.next();
                if (c == '<' || c == '\\') {
                    buf.append('\\');
                    continue;
                }
                buf.append('\\');
                carry.add(c);
                continue;
            }
            if (c == '<') {
                EnterTagInfo term = this.finishTextTerminator(carry);
                if (term == null) {
                    buf.append('<');
                    continue;
                }
                this.queuedTag = term;
                break;
            }
            buf.appendCodePoint(c);
        }

        // Trim trailing spaces
        int len = buf.length();
        while (len != 0 && buf.charAt(len - 1) == ' ') {
            len--;
        }
        buf.setLength(len);

        // Collect and strip CDATA
        String source = buf.toString();
        source = stripCData(source);

        // Parse
        Component text;
        try {
            text = mini.deserialize(source);
        } catch (ParsingException e) {
            DialogParseException ex = new DialogParseException("Failed to parse MiniMessage text", this.source.line(), this.source.column());
            ex.addSuppressed(e);
            throw ex;
        }
        return Token.text(text);
    }

    private String stripCData(String src) {
        if (!src.startsWith("<![CDATA[")) return src;
        if (!src.endsWith("]]>")) this.source.raise("Unclosed CDATA");
        return src.substring(9, src.length() - 3);
    }

    private void consumeComment(EnterTagInfo info) {
        // Check if the comment is self-closing
        if (info.terminatedBy == '>' && "!----".equals(info.name)) return;

        // Find the end of the comment
        int c;
        int head = 0;
        while (true) {
            c = this.source.next();
            if (c == -1) this.source.raise("Unclosed comment");
            if (c == COMMENT_END.charAt(head++)) {
                if (head == COMMENT_END.length()) return;
            } else {
                head = 0;
            }
        }
    }

    private @Nullable EnterTagInfo finishTextTerminator(Queue<Integer> carry) {
        String closedBy = this.textClosedBy;
        if (closedBy == null) return null;

        int c = this.source.next();
        carry.add(c);
        if (c != '/') return null;

        int head = 0;
        while (head < closedBy.length()) {
            c = this.source.next();
            carry.add(c);
            int j = closedBy.codePointAt(head++);
            if (j > 0xFFFF) head++;
            if (c != j) return null;
        }

        c = this.source.next();
        carry.add(c);
        if (c == ' ' || c == '>') return new EnterTagInfo("/" + closedBy, c);
        return null;
    }

    private int nextNonWhitespace() {
        int next;
        while (true) {
            next = this.source.next();
            if (next == '\t' || next == ' ' || next == '\n') continue;
            return next;
        }
    }

    @Contract(mutates = "param1")
    private void finishString(@Nullable StringBuilder sb) {
        int c;
        while (true) {
            c = this.source.next();
            if (c == -1) this.source.raise("Unclosed string");
            if (c == '"') return;
            if (c != '&') {
                if (sb != null) sb.appendCodePoint(c);
                continue;
            }

            // Read an entity
            StringBuilder buf = new StringBuilder();
            BitSet candidates = new BitSet(ENTITIES.length >> 1);
            candidates.set(0, candidates.length(), true);
            while (true) {
                c = this.source.next();
                if (c == -1) this.source.raise("Unterminated entity");
                if (c == ';') break;
                buf.appendCodePoint(c);
                if (candidates.cardinality() == 0) continue;
                String part = buf.toString();
                int q = candidates.nextSetBit(0);
                while (q != -1) {
                    String candidate = ENTITIES[q << 1];
                    if (!candidate.startsWith(part)) candidates.set(q, false);
                    q = candidates.nextSetBit(q + 1);
                }
            }
            int match = candidates.nextSetBit(0);
            if (match == -1) this.source.raise("Unrecognized entity: " + buf);
            if (sb != null) sb.append(ENTITIES[(match << 1) | 1]);
        }
    }

    private @Nullable EnterTagInfo enterTag() {
        int c = this.nextNonWhitespace();
        if (c == -1) return null;
        if (c != '<') this.source.raise("Invalid start of token (got " + identify(c) + ")");

        StringBuilder name = new StringBuilder();
        while (true) {
            c = this.source.next();
            if (c == -1) this.source.raise("Tag is not closed");
            if (c == ' ') break;
            if (c == '>') break;
            if (name.isEmpty()) {
                if (c == '?' || c == '!') {
                    name.appendCodePoint(c);
                    continue;
                }
            } else if (c == '?' && name.charAt(0) == '?') {
                break;
            }
            if (('a' <= c && c <= 'z') ||
                    ('A' <= c && c <= 'Z') ||
                    c == '-' || c == '/'
            ) {
                name.appendCodePoint(c);
                continue;
            }
            this.source.raise("Invalid character in tag name: " + identify(c));
        }

        if (name.isEmpty()) this.source.raise("Tag name expected");
        return new EnterTagInfo(name.toString(), c);
    }

    private boolean readTagAttrs(BiConsumer<String, String> dest, boolean pi) {
        Set<String> keys = new HashSet<>();
        StringBuilder keyBuffer = new StringBuilder();
        boolean finalized = false;
        boolean selfClosing = false;
        int c;

        while (true) {
            c = this.nextNonWhitespace();
            if (c == -1) this.source.raise("Tag is not closed");
            if (c == '>') {
                if (pi) this.source.raise("Expected to encounter '?' before '>' in processing instruction");
                break;
            }
            if (c == '?') {
                if (pi) {
                    c = this.source.next();
                    if (c != '>') this.source.raise("Processing instruction not terminated");
                    break;
                }
                this.source.raise("Disallowed character: " + identify(c));
            }
            if (c == '/') {
                c = this.source.next();
                if (c != '>') this.source.raise("Tag start is not closed");
                selfClosing = true;
                break;
            }
            if (('a' <= c && c <= 'z') ||
                    ('A' <= c && c <= 'Z') ||
                    c == '-'
            ) {
                if (finalized) break;
                keyBuffer.appendCodePoint(c);
                continue;
            }
            if (c == ' ') {
                finalized = true;
                continue;
            }
            if (c != '=') this.source.raise("Invalid character in attribute name: " + identify(c));
            c = this.nextNonWhitespace();
            if (c != '"') this.source.raise("Expected attribute value");
            StringBuilder valueBuilder = new StringBuilder();
            this.finishString(valueBuilder);
            String key = keyBuffer.toString();
            String value = valueBuilder.toString();
            keyBuffer.setLength(0);
            finalized = false;
            if (!keys.add(key)) this.source.raise("Duplicate attribute: " + key);
            dest.accept(key, value);
        }

        if (!keyBuffer.isEmpty()) {
            this.source.raise("Expected attribute value");
        }

        return selfClosing;
    }

    private static String identify(int c) {
        if (c == -1) return "EOF";
        if (c == '\t') return "TAB";
        if (c == '\r' || c == '\n') return "EOL";
        if (c == ' ') return "SPACE";
        if (Character.isLetterOrDigit(c)) {
            if (c < 0x10000) return "'" + (char) c + "'";
            return "'" + Character.highSurrogate(c) + Character.lowSurrogate(c) + "'";
        }
        String s = Integer.toString(c, 16);
        if (c < 0x100) return "'\\x" + "0".repeat(2 - s.length()) + s + "'";
        if (c < 0x10000) return "'\\u" + "0".repeat(4 - s.length()) + s + "'";
        return "'\\U" + "0".repeat(6 - s.length()) + s + "'";
    }

    //

    public interface Source {

        static Source string(String string) {
            return new StringSource(string);
        }

        @Contract("_ -> new")
        static Source reader(Reader reader) {
            return new ReaderSource(reader);
        }

        //

        int line();

        int column();

        @Range(from = -1, to = 0x10FFFF) int next() throws DialogException;

        @Contract("_ -> fail")
        void raise(String message) throws DialogParseException;

    }

    private static abstract class AbstractSource implements Source {

        private int line;
        private int column;
        private int carry = -1;

        protected AbstractSource() {
            this.line = 1;
            this.column = 0;
        }

        //

        protected abstract @Range(from = -1, to = 0xFFFF) int nextChar() throws DialogException;

        public int line() {
            return this.line;
        }

        public int column() {
            return this.column;
        }

        @Override
        public @Range(from = -1, to = 0x10FFFF) int next() throws DialogException {
            // Poll
            int c = this.carry;
            if (c == -1) {
                c = this.nextChar();
            } else {
                this.carry = -1;
            }

            if (c == -1) {
                // EOF
                return -1;
            } else if (c == 0x0D) {
                // Line ending (\r, \r\n)
                this.line++;
                this.column = 0;
                int k = this.nextChar();
                if (k != 0x0A) this.carry = k;
                return 0x0A;
            } else if (c == 0x0A) {
                // Line ending (\n)
                this.line++;
                this.column = 0;
                return 0x0A;
            }

            // Scalar
            this.column++;
            if (c < 0xD800 || c > 0xDFFF) return c;

            // Surrogate
            if (c >= 0xDC00) this.raise("Unpaired low surrogate: " + identify(c));
            int q = this.nextChar();
            if (q < 0xDC00 || q > 0xDFFF) this.raise("Unpaired high surrogate: " + identify(q));
            this.column++;
            return 0x10000 + (((c & 0x3FF) << 10) | (q & 0x3FF));
        }

        @Override
        @Contract("_ -> fail")
        public void raise(String message) throws DialogParseException {
            throw new DialogParseException(message, this.line, this.column);
        }

    }

    private static final class ReaderSource extends AbstractSource {

        private final Reader reader;

        ReaderSource(Reader reader) {
            this.reader = reader;
        }

        //

        @Override
        protected @Range(from = -1, to = 0xFFFF) int nextChar() throws DialogException {
            int n;
            try {
                n = this.reader.read();
            } catch (IOException e) {
                throw new DialogIOException(e);
            }
            return n;
        }

    }

    private static final class StringSource extends AbstractSource {

        private final String data;
        private int head;

        StringSource(String data) {
            this.data = data;
            this.head = 0;
        }

        //

        @Override
        protected @Range(from = -1, to = 0xFFFF) int nextChar() throws DialogException {
            int head = this.head;
            if (head >= this.data.length()) return -1;
            char c = this.data.charAt(head);
            this.head = head + 1;
            return c;
        }

    }

    private record EnterTagInfo(
            String name,
            int terminatedBy
    ) { }

    private enum TextMode {
        /** No MiniMessage parsing */
        NONE,

        /** Use MiniMessage: all components allowed */
        FULL,

        /** Use MiniMessage: no NBT, score or selector components */
        LIMITED
    }

}
