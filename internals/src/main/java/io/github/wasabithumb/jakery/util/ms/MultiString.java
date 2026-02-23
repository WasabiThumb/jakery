/*
 * Copyright 2026 Xavier Pedraza
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
package io.github.wasabithumb.jakery.util.ms;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.util.NoSuchElementException;

@NullMarked
@ApiStatus.NonExtendable
public interface MultiString extends Iterable<String>, Comparable<MultiString> {

    char DEFAULT_SEPARATOR = '.';

    static MultiString join(MultiString a, MultiString b) {
        if (a.isEmpty()) return b;
        return ConcatMultiString.of(a, b);
    }

    static MultiString of(CharSequence... parts) {
        if (parts.length == 1) return new SingleMultiString(parts[0].toString());
        return new ArrayMultiString(parts);
    }

    static MultiString parse(CharSequence text, char sep) {
        return new SplitMultiString(text.toString(), sep);
    }

    static MultiString parse(CharSequence text) {
        return parse(text, DEFAULT_SEPARATOR);
    }

    //

    default boolean isEmpty() {
        return this.size() == 0;
    }

    int size();

    String get(int index);

    String toString(char sep);

    default String last() {
        int size = this.size();
        if (size == 0) throw new NoSuchElementException();
        return this.get(size - 1);
    }

}
