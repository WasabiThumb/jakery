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
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;

@NullMarked
@ApiStatus.Internal
final class ConcatMultiString extends AbstractMultiString {

    static ConcatMultiString of(MultiString... src) {
        int len = src.length;
        MultiString[] cpy = new MultiString[len];

        for (int i = 0; i < len; i++) {
            MultiString next = src[i];
            if (next instanceof ConcatMultiString) return ofUnwrapping(src, i);
            cpy[i] = next;
        }

        return new ConcatMultiString(cpy);
    }

    private static ConcatMultiString ofUnwrapping(MultiString[] src, int start) {
        int nelem = start;
        MultiString[] startUnwrapped = ((ConcatMultiString) src[start]).sub;
        nelem += startUnwrapped.length;

        for (int i = (start + 1); i < src.length; i++) {
            MultiString next = src[i];
            if (next instanceof ConcatMultiString) {
                nelem += ((ConcatMultiString) next).sub.length;
            } else {
                nelem++;
            }
        }

        MultiString[] elements = new MultiString[nelem];
        int head = 0;

        System.arraycopy(src, 0, elements, 0, start);
        head += start;

        System.arraycopy(startUnwrapped, 0, elements, head, startUnwrapped.length);
        head += startUnwrapped.length;

        for (int i = (start + 1); i < src.length; i++) {
            MultiString next = src[i];
            if (next instanceof ConcatMultiString) {
                MultiString[] sub = ((ConcatMultiString) next).sub;
                System.arraycopy(sub, 0, elements, head, sub.length);
                head += sub.length;
            } else {
                elements[head++] = next;
            }
        }

        return new ConcatMultiString(elements);
    }

    //

    private final MultiString[] sub;

    private ConcatMultiString(MultiString[] sub) {
        this.sub = sub;
    }

    //


    @Override
    public boolean isEmpty() {
        return this.sub.length == 0 || this.size() == 0;
    }

    @Override
    public int size() {
        int total = 0;
        for (MultiString strings : this.sub) total += strings.size();
        return total;
    }

    @Override
    public String get(int index) {
        final int originalIndex = index;
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + this.size());
        }

        int accumulator = 0;
        int size;
        for (MultiString next : this.sub) {
            size = next.size();
            if (index < size) return next.get(index);
            index -= size;
            accumulator += size;
        }

        if (originalIndex >= accumulator) {
            throw new IndexOutOfBoundsException("Index " + originalIndex + " out of bounds for length " + accumulator);
        } else {
            throw new AssertionError("Valid index " + originalIndex + " not successfully mapped");
        }
    }

    @Override
    public String last() {
        for (int i = this.sub.length - 1; i >= 0; i--) {
            MultiString ms = this.sub[i];
            if (!ms.isEmpty()) return ms.last();
        }
        throw new NoSuchElementException();
    }

    @Override
    public Iterator<String> iterator() {
        return new Iter(this);
    }

    @Override
    public String toString(char sep) {
        int size = this.sub.length;
        char[][] parts = new char[size][];
        int total = 0;

        for (int i = 0; i < size; i++) {
            if (i != 0) total++;
            char[] part = this.sub[i].toString(sep).toCharArray();
            total += part.length;
            parts[i] = part;
        }

        char[] ret = new char[total];
        int head = 0;

        for (int i = 0; i < size; i++) {
            if (i != 0) ret[head++] = sep;
            char[] part = parts[i];
            System.arraycopy(part, 0, ret, head, part.length);
            head += part.length;
        }

        return new String(ret);
    }

    //

    private static final class Iter implements Iterator<String> {

        private final ConcatMultiString parent;
        private int head;
        private @Nullable Iterator<String> sub;

        Iter(ConcatMultiString parent) {
            this.parent = parent;
            this.head = 0;
            this.sub = null;
        }

        //

        /**
         * @apiNote Always returns an iterator for which {@code hasNext} is {@code true}
         */
        @Contract("true -> !null")
        private @Nullable Iterator<String> nextSubIterator(boolean require) {
            Iterator<String> ret = this.sub;
            while (ret == null || !ret.hasNext()) {
                if (this.head >= this.parent.sub.length) {
                    if (require) throw new NoSuchElementException();
                    return null;
                }
                this.sub = ret = this.parent.sub[this.head++].iterator();
            }
            return ret;
        }

        @Override
        public boolean hasNext() {
            return this.nextSubIterator(false) != null;
        }

        @Override
        public String next() {
            return this.nextSubIterator(true).next();
        }

    }

}
