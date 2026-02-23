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
package io.github.wasabithumb.jakery.file;

import io.github.wasabithumb.jakery.util.Buffers;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@NullMarked
@ApiStatus.Internal
abstract class JakeFileBlob {

    public static JakeFileBlob u8(byte[] data) {
        return new U8(data);
    }

    public static JakeFileBlob u16(short[] data) {
        return new U16(data);
    }

    public static JakeFileBlob u32(int[] data) {
        return new U32(data);
    }

    public static Builder builder() {
        return new Builder();
    }

    //

    protected JakeFileBlob() { }

    //

    public abstract int size();

    public abstract int get(int index) throws IndexOutOfBoundsException;

    public Reader newReader() {
        return new Reader(this);
    }

    public void writeU8(OutputStream out) throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void writeU16(OutputStream out) throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void writeU32(OutputStream out) throws IOException {
        byte[] array = new byte[4];
        ByteBuffer view = ByteBuffer.wrap(array);
        view.order(ByteOrder.BIG_ENDIAN);

        for (int i = 0; i < this.size(); i++) {
            view.putInt(0, this.get(i));
            out.write(array, 0, 4);
        }
    }

    @Override
    public int hashCode() {
        int size = this.size();
        int h = 7;
        for (int i = 0; i < size; i++) {
            h = 31 * h + this.get(i);
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JakeFileBlob)) return false;
        JakeFileBlob other = (JakeFileBlob) obj;
        int size = this.size();
        if (size != other.size()) return false;
        for (int i = 0; i < size; i++ ){
            if (this.get(i) != other.get(i)) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');

        int size = this.size();
        for (int i = 0; i < size; i++) {
            if (i != 0) sb.append(", ");
            sb.append(this.get(i));
        }

        return sb.append(']')
                .toString();
    }

    //

    public static final class Reader {

        private final JakeFileBlob parent;
        private int head;

        private Reader(JakeFileBlob parent) {
            this.parent = parent;
            this.head = 0;
        }

        //

        public boolean hasNext() {
            return this.head < this.parent.size();
        }

        public int next() {
            return this.parent.get(this.head++);
        }

    }

    private static final class U8 extends JakeFileBlob {

        private final byte[] data;

        private U8(byte[] data) {
            this.data = data;
        }

        //

        @Override
        public int size() {
            return this.data.length;
        }

        @Override
        public int get(int index) {
            return this.data[index] & 0xFF;
        }

        @Override
        public void writeU8(OutputStream out) throws IOException {
            out.write(this.data);
        }

        @Override
        public void writeU16(OutputStream out) throws IOException {
            byte[] buf = new byte[2];
            for (byte b : this.data) {
                buf[1] = b;
                out.write(buf, 0, 2);
            }
        }

    }

    private static final class U16 extends JakeFileBlob {

        private final short[] data;

        private U16(short[] data) {
            this.data = data;
        }

        //

        @Override
        public int size() {
            return this.data.length;
        }

        @Override
        public int get(int index) {
            return this.data[index] & 0xFFFF;
        }

        @Override
        public void writeU16(OutputStream out) throws IOException {
            byte[] buf = new byte[2];
            for (short s : this.data) {
                buf[0] = (byte) (s >>> 8);
                buf[1] = (byte) (s & 0xFF);
                out.write(buf, 0, 2);
            }
        }

    }

    private static final class U32 extends JakeFileBlob {

        private final int[] data;

        private U32(int[] data) {
            this.data = data;
        }

        //

        @Override
        public int size() {
            return this.data.length;
        }

        @Override
        public int get(int index) {
            return this.data[index] & 0x7FFFFFFF;
        }

    }

    public static final class Builder {

        private int[] buf;
        private int bufCapacity;
        private int bufSize;
        private Width width;

        private Builder(int initialCapacity) {
            this.buf = new int[initialCapacity];
            this.bufCapacity = initialCapacity;
            this.bufSize = 0;
            this.width = Width.U8;
        }

        private Builder() {
            this(16);
        }

        //

        Builder put(byte value) {
            this.putSingle(value & 0xFF);
            return this;
        }

        Builder put(short value) {
            int iv = value & 0xFFFF;
            this.putSingle(iv);
            if (this.width == Width.U8 && (iv >> 8) != 0) this.width = Width.U16;
            return this;
        }

        Builder put(int value) {
            this.putSingle(value);
            Width width = Width.of(value);
            if (width.ordinal() > this.width.ordinal()) this.width = width;
            return this;
        }

        private void putSingle(int value) {
            this.ensureCapacity(1);
            this.buf[this.bufSize++] = value;
        }

        Builder put(byte... values) {
            this.ensureCapacity(values.length);
            for (byte value : values) {
                this.buf[this.bufSize++] = value & 0xFF;
            }
            return this;
        }

        Builder put(short... values) {
            this.ensureCapacity(values.length);
            boolean check = this.width == Width.U8;
            int iv;
            for (short value : values) {
                iv = value & 0xFFFF;
                if (check && (iv >> 8) != 0) {
                    this.width = Width.U16;
                    check = false;
                }
                this.buf[this.bufSize++] = iv;
            }
            return this;
        }

        Builder put(int... values) {
            final int length = values.length;
            this.ensureCapacity(length);

            Width currentWidth = this.width;
            int head = 0;

            if (currentWidth != Width.U32) {
                Width nextWidth;
                while (true) {
                    if (head >= length) return this;
                    int next = values[head++];
                    nextWidth = Width.of(next);
                    if (nextWidth.ordinal() > currentWidth.ordinal()) {
                        this.width = currentWidth = nextWidth;
                        if (nextWidth == Width.U32) break;
                    }
                    this.buf[this.bufSize++] = next;
                }
            }

            int rem = length - head;
            if (rem != 0) {
                System.arraycopy(values, head, this.buf, this.bufSize, rem);
                this.bufSize += rem;
            }
            return this;
        }

        private void ensureCapacity(int additional) {
            int target = this.bufSize + additional;
            int capacity = this.bufCapacity;
            if (target <= capacity) return;
            do {
                capacity = Buffers.doubleCapacity(capacity);
            } while (target > capacity);

            int[] cpy = new int[capacity];
            System.arraycopy(this.buf, 0, cpy, 0, this.bufSize);
            this.buf = cpy;
            this.bufCapacity = capacity;
        }

        JakeFileBlob build() {
            switch (this.width) {
                case U8:
                    byte[] bytes = new byte[this.bufSize];
                    for (int i = 0; i < this.bufSize; i++) bytes[i] = (byte) this.buf[i];
                    return u8(bytes);
                case U16:
                    short[] shorts = new short[this.bufSize];
                    for (int i = 0; i < this.bufSize; i++) shorts[i] = (short) this.buf[i];
                    return u16(shorts);
                case U32:
                    int[] ints = new int[this.bufSize];
                    System.arraycopy(this.buf, 0, ints, 0, this.bufSize);
                    return u32(ints);
                default:
                    throw new AssertionError();
            }
        }

        //

        private enum Width {
            U8,
            U16,
            U32;

            private static final Width[] LUT = new Width[] {
                    U32, U32, U16, U8, U8
            };

            static Width of(int value) {
                return LUT[Integer.numberOfLeadingZeros(value) >> 3];
            }
        }

    }

}
