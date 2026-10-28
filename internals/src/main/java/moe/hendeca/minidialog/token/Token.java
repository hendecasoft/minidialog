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

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

import java.util.*;

@NullMarked
@ApiStatus.Internal
public sealed interface Token {

    static OpenTag.Builder openTag(String name) {
        return new OpenTag.Builder(name);
    }

    static CloseTag closeTag(String name) {
        return new CloseTag(name);
    }

    static Text text(Component text) {
        return new Text(text);
    }

    static Comment comment() {
        return Comment.INSTANCE;
    }

    static Prolog prolog() {
        return Prolog.INSTANCE;
    }

    //

    record OpenTag(
            String name,
            @Unmodifiable Map<String, String> attributes,
            boolean selfClosing
    ) implements Token {

        public static final class Builder {

            private final String name;
            private final List<Map.Entry<String, String>> attrs = new LinkedList<>();
            private boolean selfClosing;

            Builder(String name) {
                this.name = Objects.requireNonNull(name, "name must not be null");
                this.selfClosing = false;
            }

            //

            @Contract(value = "_, _ -> this", mutates = "this")
            public Builder attribute(String key, String value) {
                Objects.requireNonNull(key, "key must not be null");
                Objects.requireNonNull(value, "value must not be null");
                this.attrs.add(Map.entry(key, value));
                return this;
            }

            @Contract("_ -> this")
            public Builder selfClosing(boolean selfClosing) {
                this.selfClosing = selfClosing;
                return this;
            }

            @Contract("-> new")
            public OpenTag build() {
                return new OpenTag(this.name, this.moveAttrs(), this.selfClosing);
            }

            private @Unmodifiable Map<String, String> moveAttrs() {
                if (this.attrs.isEmpty()) return Collections.emptyMap();
                Map<String, String> ret = LinkedHashMap.newLinkedHashMap(this.attrs.size());
                for (Map.Entry<String, String> entry : this.attrs) {
                    String old = ret.put(entry.getKey(), entry.getValue());
                    if (old == null) continue;
                    throw new IllegalStateException("Duplicate attribute: " + entry.getKey());
                }
                return Collections.unmodifiableMap(ret);
            }

        }

    }

    record CloseTag(
            String name
    ) implements Token {

        public CloseTag {
            Objects.requireNonNull(name, "name must not be null");
        }

    }

    record Text(
            Component value
    ) implements Token {

        public Text {
            Objects.requireNonNull(value, "value must not be null");
        }

    }

    final class Prolog implements Token {
        public static final Prolog INSTANCE = new Prolog();
        private Prolog() { }
    }

    final class Comment implements Token {
        public static final Comment INSTANCE = new Comment();
        private Comment() { }
    }

}
