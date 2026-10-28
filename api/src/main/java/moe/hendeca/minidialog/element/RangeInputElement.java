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
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class RangeInputElement extends InputElement {

    private static final PropertySet PROPERTIES = new PropertySet(
            Property.ID,
            Property.RANGE_INPUT_WIDTH,
            Property.RANGE_INPUT_START,
            Property.RANGE_INPUT_END,
            Property.RANGE_INPUT_STEP,
            Property.RANGE_INPUT_INITIAL
    );

    private static final ElementTypeUnion CHILD_TYPES = new ElementTypeUnion(
            LabelElement.class
    );

    private static void parseRange(double start, double end) throws IllegalArgumentException {
        if (!Double.isFinite(start)) throw new IllegalArgumentException("start must be finite (got " + start + ")");
        if (!Double.isFinite(end)) throw new IllegalArgumentException("end must be finite (got " + end + ")");
        if (end >= start) return;
        throw new IllegalArgumentException("invalid range (" + start + " - " + end + ")");
    }

    //

    private RangeInputElement() {
        super(Kind.RANGE_INPUT, PROPERTIES, CHILD_TYPES);
    }

    public RangeInputElement(double start, double end) throws IllegalArgumentException {
        this();
        parseRange(start, end);
        this.props.set(Property.RANGE_INPUT_START, start);
        this.props.set(Property.RANGE_INPUT_END, end);
    }

    //

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public RangeInputElement id(@Nullable String id) {
        return (RangeInputElement) super.id(id);
    }

    @Override
    @Contract("_ -> fail")
    public RangeInputElement text(Component text) throws UnsupportedOperationException {
        return (RangeInputElement) super.text(text);
    }

    @Contract(pure = true)
    public int width() {
        return this.props.get(Property.RANGE_INPUT_WIDTH);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public RangeInputElement width(int width) throws IllegalArgumentException {
        if (width < 1) throw new IllegalArgumentException("width may not be less than 1 (got" + width + ")");
        this.props.set(Property.RANGE_INPUT_WIDTH, width);
        return this;
    }

    @Contract(pure = true)
    public double start() {
        return this.props.getRequired(Property.RANGE_INPUT_START);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public RangeInputElement start(double start) throws IllegalArgumentException {
        parseRange(start, this.props.getRequired(Property.RANGE_INPUT_END));
        this.props.set(Property.RANGE_INPUT_START, start);
        return this;
    }

    @Contract(pure = true)
    public double end() {
        return this.props.getRequired(Property.RANGE_INPUT_END);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public RangeInputElement end(double end) throws IllegalArgumentException {
        parseRange(this.props.getRequired(Property.RANGE_INPUT_START), end);
        this.props.set(Property.RANGE_INPUT_END, end);
        return this;
    }

    @Contract(pure = true)
    public @Nullable Double step() {
        return this.props.get(Property.RANGE_INPUT_STEP);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public RangeInputElement step(@Nullable Double step) throws IllegalArgumentException {
        if (step != null && !Double.isFinite(step)) throw new IllegalArgumentException("step must be finite (got " + step + ")");
        this.props.set(Property.RANGE_INPUT_STEP, step);
        return this;
    }

    @Contract(pure = true)
    public @Nullable Double initial() {
        return this.props.get(Property.RANGE_INPUT_INITIAL);
    }

    @Contract(value = "_ -> this", mutates = "this")
    public RangeInputElement initial(@Nullable Double initial) throws IllegalArgumentException {
        if (initial != null && !Double.isFinite(initial)) throw new IllegalArgumentException("initial must be finite (got " + initial + ")");
        this.props.set(Property.RANGE_INPUT_INITIAL, initial);
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public RangeInputElement append(Node child) {
        return (RangeInputElement) super.append(child);
    }

    @Override
    public RangeInputElement copy() {
        RangeInputElement ret = new RangeInputElement();
        this.copyTo(ret);
        return ret;
    }
}
