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
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@NullMarked
@ApiStatus.Internal
final class JakeFileIO {

    private static final byte[] HEADER = new byte[] { (byte) 'j', (byte) 'a', (byte) 'k', (byte) 'e' };

    //

    public static void write(OutputStream out, JakeFileImpl file) throws IOException {
        out.write(HEADER);

        JakeFilePool pool = file.pool();
        int poolSize = pool.size();
        int poolWidth = 4 - (Integer.numberOfLeadingZeros(poolSize) >> 3);
        writePool(out, pool, poolSize);

        Set<String> keys = file.keys();
        int groupCount = keys.size();
        writeVarUInt(out, groupCount);

        for (String key : keys) {
            JakeFileGroupImpl<?> group = file.group(key);
            assert group != null;
            writeGroup(out, group, poolWidth);
        }
    }

    private static void writeGroup(
            OutputStream out,
            JakeFileGroupImpl<?> group,
            @Range(from = 0, to = 4) int poolWidth
    ) throws IOException {
        JakeFileGroup.Type type = group.type();
        int typeByte = groupTypeToByte(type);
        out.write(typeByte);

        writeUtf8(out, group.name());

        JakeFileBlob blob = group.blob();
        writeVarUInt(out, blob.size());
        switch (poolWidth) {
            case 0:
                throw new IllegalStateException("Pool is empty while writing group");
            case 1:
                blob.writeU8(out);
                break;
            case 2:
                blob.writeU16(out);
                break;
            case 3:
            case 4:
                blob.writeU32(out);
                break;
            default:
                throw new AssertionError("Unreachable code");
        }
    }

    private static void writePool(OutputStream out, JakeFilePool pool, int poolSize) throws IOException {
        writeVarUInt(out, poolSize);
        for (int i = 0; i < poolSize; i++) {
            JakeFilePool.Entry entry = pool.get(i);
            writeUtf8(out, entry.asString());
        }
    }

    public static JakeFileImpl read(InputStream in) throws IOException {
        readHeader(in);

        int poolSize = readVarUInt(in);
        int poolWidth = 4 - (Integer.numberOfLeadingZeros(poolSize) >> 3);
        JakeFilePool pool = readPool(in, poolSize);

        int groupCount = readVarUInt(in);
        Map<String, JakeFileGroupImpl<?>> groups = new LinkedHashMap<>(Buffers.hashCapacity(groupCount));

        for (int i = 0; i < groupCount; i++) {
            JakeFileGroupImpl<?> group = readGroup(in, poolWidth, pool);
            JakeFileGroup<?> existing = groups.put(group.name, group);
            if (existing == null) continue;
            throw new IOException("Duplicate group");
        }

        return new JakeFileImpl(pool, groups);
    }

    private static JakeFileGroupImpl<?> readGroup(
            InputStream in,
            @Range(from = 0, to = 4) int poolWidth,
            JakeFilePool pool
    ) throws IOException {
        int typeByte = in.read();
        if (typeByte == -1) throw new EOFException("Expected group, got EOF");

        JakeFileGroup.Type type = byteToGroupType(typeByte);
        if (type == null) throw new IOException("Invalid group type (" + typeByte + ")");

        String name = readUtf8(in);
        int blobSize = readVarUInt(in);
        DataInputStream din = new DataInputStream(in);
        JakeFileBlob blob;

        switch (poolWidth) {
            case 0:
                throw new IOException("Cannot read group with empty pool");
            case 1:
                byte[] u8 = new byte[blobSize];
                din.readFully(u8);
                blob = JakeFileBlob.u8(u8);
                break;
            case 2:
                short[] u16 = new short[blobSize];
                for (int i = 0; i < blobSize; i++) u16[i] = din.readShort();
                blob = JakeFileBlob.u16(u16);
                break;
            case 3:
            case 4:
                int[] u32 = new int[blobSize];
                for (int i = 0; i < blobSize; i++) u32[i] = din.readInt();
                blob = JakeFileBlob.u32(u32);
                break;
            default:
                throw new AssertionError("Unreachable code");
        }

        switch (type) {
            case TYPE:
                return new JakeFileGroupImpl.OfTypes(name, pool, blob);
            case FIELD:
                return new JakeFileGroupImpl.OfFields(name, pool, blob);
            case METHOD:
                return new JakeFileGroupImpl.OfMethods(name, pool, blob);
            case CONSTRUCTOR:
                return new JakeFileGroupImpl.OfConstructors(name, pool, blob);
            default:
                throw new AssertionError("Unreachable code");
        }
    }

    private static JakeFilePool readPool(InputStream in, int poolSize) throws IOException {
        JakeFilePool ret = new JakeFilePool(poolSize);
        for (int i = 0; i < poolSize; i++) {
            String entryValue = readUtf8(in);
            JakeFilePool.Entry entry = JakeFilePool.entry(entryValue);
            int interned = ret.intern(entry);
            if (interned != i) {
                throw new IOException("Duplicate pool entry");
            }
        }
        return ret;
    }

    private static String readUtf8(InputStream in) throws IOException {
        int len = readVarUInt(in);
        byte[] buf = new byte[len];
        int head = 0;
        int read;
        while (head < len) {
            read = in.read(buf, head, len - head);
            if (read == -1) throw new EOFException("Truncated string");
            head += read;
        }
        return new String(buf, StandardCharsets.UTF_8);
    }

    private static void writeUtf8(OutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarUInt(out, bytes.length);
        out.write(bytes);
    }

    private static void readHeader(InputStream in) throws IOException {
        int count = HEADER.length;
        byte[] buf = new byte[count];
        int head = 0;
        int read;

        while (head < count) {
            read = in.read(buf, head, count - head);
            if (read == -1) throw new EOFException("Missing file header");
            head += read;
        }

        if (!Arrays.equals(buf, HEADER)) {
            throw new IOException("Malformed file header");
        }
    }

    private static @Range(from = 0, to = Integer.MAX_VALUE) int readVarUInt(InputStream in) throws IOException {
        int ret = 0;
        int n = 0;
        boolean loop = true;
        int b;

        for (; loop; n++) {
            b = in.read();
            if (b == -1) {
                throw new EOFException(
                        (n == 0) ?
                                "Expected VarUInt, got EOF" :
                                "Truncated VarUInt"
                );
            }
            if (n == 4) {
                if ((b & 0xF8) != 0) throw new IOException("Overlong VarUInt");
                loop = false;
            } else if ((b & 0x80) == 0) {
                loop = false;
            } else {
                b &= 0x7F;
            }
            ret |= (b << (n * 7));
        }

        return ret;
    }

    private static void writeVarUInt(OutputStream out, int value) throws IOException {
        if (value < 0) {
            throw new IllegalArgumentException("Cannot write negative value as VarUInt (got " + value + ")");
        }

        int tmp;
        while (true) {
            tmp = value & 0x7F;
            value >>= 7;
            if (value == 0) {
                out.write(tmp);
                break;
            }
            out.write(tmp | 0x80);
        }
    }

    private static int groupTypeToByte(JakeFileGroup.Type type) {
        switch (type) {
            case TYPE:
                return 't';
            case FIELD:
                return 'f';
            case METHOD:
                return 'm';
            case CONSTRUCTOR:
                return 'c';
            default:
                throw new AssertionError("Unreachable code");
        }
    }

    private static JakeFileGroup.@Nullable Type byteToGroupType(int b) {
        switch (b & 0xFF) {
            case (int) 't':
                return JakeFileGroup.Type.TYPE;
            case (int) 'f':
                return JakeFileGroup.Type.FIELD;
            case (int) 'm':
                return JakeFileGroup.Type.METHOD;
            case (int) 'c':
                return JakeFileGroup.Type.CONSTRUCTOR;
            default:
                return null;
        }
    }

    //

    private JakeFileIO() { }

}
