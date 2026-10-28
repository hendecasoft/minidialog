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

import moe.hendeca.minidialog.node.Node;
import moe.hendeca.minidialog.node.NodeType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

@NullMarked
@ApiStatus.NonExtendable
public interface Element extends Node {

    @Contract("-> new")
    static DialogElement dialog() {
        return new DialogElement();
    }

    @Contract("-> new")
    static TitleElement title() {
        return new TitleElement();
    }

    @Contract("_ -> new")
    static TitleElement title(Component text) {
        return title().text(text);
    }

    @Contract("-> new")
    static ExternalTitleElement externalTitle() {
        return new ExternalTitleElement();
    }

    @Contract("_ -> new")
    static ExternalTitleElement externalTitle(Component text) {
        return externalTitle().text(text);
    }

    @Contract("-> new")
    static BodyElement body() {
        return new BodyElement();
    }

    @Contract("-> new")
    static MessageElement message() {
        return new MessageElement();
    }

    @Contract("_ -> new")
    static MessageElement message(Component text) {
        return message().text(text);
    }

    @Contract("_ -> new")
    static ItemElement item(Key src) {
        return new ItemElement(src);
    }

    @Contract("_, _ -> new")
    static ItemElement item(Key src, int count) {
        return item(src).count(count);
    }

    @Contract("_, _, _ -> new")
    static ItemElement item(Key src, int count, Component description) {
        return item(src).count(count).append(description().text(description));
    }

    @Contract("_, _ -> new")
    static ItemElement item(Key src, Component description) {
        return item(src).append(description().text(description));
    }

    @Contract("-> new")
    static DescriptionElement description() {
        return new DescriptionElement();
    }

    @Contract("_ -> new")
    static DescriptionElement description(Component text) {
        return description().text(text);
    }

    @Contract("-> new")
    static InputsElement inputs() {
        return new InputsElement();
    }

    @Contract("-> new")
    static TextInputElement textInput() {
        return new TextInputElement();
    }

    @Contract("_, _ -> new")
    static TextInputElement textInput(@Nullable String id, Component label) {
        return textInput().id(id).append(label(label));
    }

    @Contract("-> new")
    static BooleanInputElement booleanInput() {
        return new BooleanInputElement();
    }

    @Contract("_, _ -> new")
    static BooleanInputElement booleanInput(@Nullable String id, Component label) {
        return booleanInput().id(id).append(label(label));
    }

    @Contract("-> new")
    static OptionInputElement optionInput() {
        return new OptionInputElement();
    }

    @Contract("_, _ -> new")
    static OptionInputElement optionInput(@Nullable String id, Component label) {
        return optionInput().id(id).append(label(label));
    }

    @Contract("_, _ -> new")
    static RangeInputElement rangeInput(double start, double end) {
        return new RangeInputElement(start, end);
    }

    @Contract("_, _, _, _ -> new")
    static RangeInputElement rangeInput(@Nullable String id, Component label, double start, double end) {
        return rangeInput(start, end).id(id).append(label(label));
    }

    @Contract("-> new")
    static LabelElement label() {
        return (new LabelElement());
    }

    @Contract("_ -> new")
    static LabelElement label(Component text) {
        return label().text(text);
    }

    @Contract("-> new")
    static MultilineElement multiline() {
        return new MultilineElement();
    }

    @Contract("_ -> new")
    static MultilineElement multiline(int maxLines) {
        return multiline().maxLines(maxLines);
    }

    @Contract("_, _ -> new")
    static MultilineElement multiline(int maxLines, int height) {
        return multiline().maxLines(maxLines).height(height);
    }

    @Contract("-> new")
    static OptionElement option() {
        return new OptionElement();
    }

    @Contract("_, _ -> new")
    static OptionElement option(@Nullable String value, Component text) {
        return option().value(value).text(text);
    }

    @Contract("-> new")
    static ActionsElement actions() {
        return new ActionsElement();
    }

    @Contract("_ -> new")
    static ActionsElement actions(int columns) {
        return actions().columns(columns);
    }

    @Contract("_ -> new")
    static OpenUrlActionElement openUrlAction(String url) {
        return new OpenUrlActionElement(url);
    }

    @Contract("_, _ -> new")
    static OpenUrlActionElement openUrlAction(String url, Component label) {
        return openUrlAction(url).append(label(label));
    }

    @Contract("_ -> new")
    static RunCommandActionElement runCommandAction(String command) {
        return new RunCommandActionElement(command);
    }

    @Contract("_, _ -> new")
    static RunCommandActionElement runCommandAction(String command, Component label) {
        return runCommandAction(command).append(label(label));
    }

    @Contract("_ -> new")
    static SuggestCommandActionElement suggestCommandAction(String command) {
        return new SuggestCommandActionElement(command);
    }

    @Contract("_, _ -> new")
    static SuggestCommandActionElement suggestCommandAction(String command, Component label) {
        return suggestCommandAction(command).append(label(label));
    }

    @Contract("_ -> new")
    static CopyToClipboardActionElement copyToClipboardAction(String value) {
        return new CopyToClipboardActionElement(value);
    }

    @Contract("_, _ -> new")
    static CopyToClipboardActionElement copyToClipboardAction(String value, Component label) {
        return copyToClipboardAction(value).append(label(label));
    }

    @Contract("-> new")
    static ShowDialogActionElement showDialogAction() {
        return new ShowDialogActionElement();
    }

    @Contract("_, _ -> new")
    static ShowDialogActionElement showDialogAction(DialogElement dialog, Component label) {
        return showDialogAction().append(dialog).append(label(label));
    }

    @Contract("-> new")
    static CustomActionElement customAction() {
        return new CustomActionElement();
    }

    @Contract("_, _ -> new")
    static CustomActionElement customAction(@Nullable String id, Component label) {
        return customAction().id(id).append(label(label));
    }

    @Contract("-> new")
    static TooltipElement tooltip() {
        return new TooltipElement();
    }

    @Contract("_ -> new")
    static TooltipElement tooltip(Component text) {
        return tooltip().text(text);
    }

    @Contract("-> new")
    static FooterElement footer() {
        return new FooterElement();
    }

    //

    @Contract(pure = true)
    @Nullable String id();

    @Contract(value = "_ -> this", mutates = "this")
    Element id(@Nullable String id);

    @Contract(pure = true)
    Component text() throws UnsupportedOperationException;

    @Contract(value = "_ -> this", mutates = "this")
    Element text(Component text) throws UnsupportedOperationException;

    @Contract(pure = true)
    String name();

    @UnmodifiableView
    List<? extends Node> children();

    @Range(from = -1, to = Integer.MAX_VALUE) int indexOf(Element child);

    @Contract(mutates = "this")
    void clear();

    @Contract(mutates = "this")
    void add(Node child) throws IllegalArgumentException;

    @Contract(mutates = "this")
    boolean remove(Node child);

    @Contract("-> new")
    ElementObserver newObserver();

    @Contract("-> new")
    Element copy();

    @Contract(value = "_ -> this", mutates = "this")
    default Element append(Node child) {
        this.add(child);
        return this;
    }

    @Override
    @Contract(pure = true)
    default NodeType nodeType() {
        return NodeType.ELEMENT;
    }

    @Override
    @Contract(value = "-> fail", pure = true)
    default Component asText() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Contract(value = "-> this", pure = true)
    default Element asElement() {
        return this;
    }

}
