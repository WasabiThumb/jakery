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
final class SplitMultiString extends AbstractMultiString {

    private static int[] findOffsets(String src, char sep) {
        int srcLen = src.length();
        int count = 1;
        char c;

        for (int i = 0; i < srcLen; i++) {
            c = src.charAt(i);
            if (c != sep) continue;
            count++;
        }

        int[] ret = new int[count];
        int head = 0;
        int start = 0;

        for (int i = 0; i < srcLen; i++) {
            c = src.charAt(i);
            if (c != sep) continue;
            ret[head++] = start;
            start = i + 1;
        }

        ret[head] = start;
        return ret;
    }

    //

    private final String src;
    private final char sep;
    private final int[] offsets;

    SplitMultiString(String src, char sep) {
        this.src = src;
        this.sep = sep;
        this.offsets = findOffsets(src, sep);
    }

    //

    @Override
    public int size() {
        return this.offsets.length;
    }

    @Override
    public String get(int index) {
        if (index < 0 || index >= this.offsets.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + this.offsets.length);
        }
        int start = this.offsets[index];
        int end = this.src.indexOf(this.sep, start);
        if (end == -1) end = this.src.length();
        return this.src.substring(start, end);
    }

    @Override
    public String toString(char sep) {
        if (this.sep == sep) return this.src;
        return super.toString(sep);
    }

}
