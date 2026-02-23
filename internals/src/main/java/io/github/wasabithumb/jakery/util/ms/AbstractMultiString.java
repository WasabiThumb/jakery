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

import java.util.*;

@NullMarked
@ApiStatus.Internal
abstract class AbstractMultiString implements MultiString {

    @Override
    public Iterator<String> iterator() {
        return new Iter(this);
    }

    @Override
    public Spliterator<String> spliterator() {
        return Spliterators.spliterator(
                this.iterator(),
                this.size(),
                Spliterator.NONNULL | Spliterator.ORDERED | Spliterator.SIZED | Spliterator.IMMUTABLE
        );
    }

    @Override
    public int compareTo(MultiString o) {
        int al = this.size();
        int bl = o.size();
        int ml;
        int r;

        if (al < bl) {
            ml = al;
            r = -1;
        } else if (al > bl) {
            ml = bl;
            r = 1;
        } else {
            ml = al;
            r = 0;
        }

        String ap;
        String bp;
        int cmp;

        for (int i = 0; i < ml; i++) {
            ap = this.get(i);
            bp = o.get(i);
            cmp = ap.compareTo(bp);
            if (cmp != 0) return cmp;
        }

        return r;
    }

    @Override
    public int hashCode() {
        int h = 7;
        for (int i = 0; i < this.size(); i++) {
            h = 31 * h + this.get(i).hashCode();
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MultiString)) return false;
        int size = this.size();
        MultiString other = (MultiString) obj;
        if (size != other.size()) return false;
        for (int i = 0; i < size; i++) {
            if (!this.get(i).equals(other.get(i))) return false;
        }
        return true;
    }

    @Override
    public String toString(char sep) {
        Iterator<String> iter = this.iterator();
        if (!iter.hasNext()) return "";

        List<char[]> values = new LinkedList<>();
        int len = 0;
        while (true) {
            char[] next = iter.next().toCharArray();
            values.add(next);
            len += next.length;

            if (!iter.hasNext()) break;
            len++;
        }

        char[] ret = new char[len];
        int head = 0;
        for (char[] value : values) {
            if (head != 0) ret[head++] = sep;
            System.arraycopy(value, 0, ret, head, value.length);
            head += value.length;
        }

        return new String(ret);
    }

    @Override
    public String toString() {
        return this.toString(DEFAULT_SEPARATOR);
    }

    //

    private static final class Iter implements Iterator<String> {

        private final MultiString parent;
        private int head;

        Iter(MultiString parent) {
            this.parent = parent;
            this.head = 0;
        }

        //

        @Override
        public boolean hasNext() {
            return this.head < this.parent.size();
        }

        @Override
        public String next() {
            if (this.head >= this.parent.size()) throw new NoSuchElementException();
            return this.parent.get(this.head++);
        }

    }

}
