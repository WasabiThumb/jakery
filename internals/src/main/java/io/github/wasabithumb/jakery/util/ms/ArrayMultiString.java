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

@NullMarked
@ApiStatus.Internal
final class ArrayMultiString extends AbstractMultiString {

    private static String[] move(CharSequence[] src) {
        int len = src.length;
        String[] dest = new String[len];
        for (int i = 0; i < len; i++) dest[i] = src[i].toString();
        return dest;
    }

    //

    private final String[] data;

    ArrayMultiString(CharSequence[] src) {
        this.data = move(src);
    }

    //

    @Override
    public int size() {
        return this.data.length;
    }

    @Override
    public String get(int index) {
        if (index < 0 || index >= this.data.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + this.data.length);
        }
        return this.data[index];
    }

}
