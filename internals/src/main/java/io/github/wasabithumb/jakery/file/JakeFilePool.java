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

import io.github.wasabithumb.jakery.descriptor.type.TypeDescriptor;
import io.github.wasabithumb.jakery.util.Buffers;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@NullMarked
@ApiStatus.Internal
final class JakeFilePool {

    private static final int DEFAULT_STORE_CAPACITY = 16;

    //

    public static Entry entry(String value) {
        TypeDescriptor descriptor;
        try {
            //noinspection PatternValidation
            descriptor = TypeDescriptor.of(value);
        } catch (IllegalArgumentException ignored) {
            return new StringEntryImpl(value);
        }
        return new DescriptorEntryImpl(descriptor);
    }

    public static Entry entry(TypeDescriptor descriptor) {
        return new DescriptorEntryImpl(descriptor);
    }

    public static JakeFilePool copyOf(JakeFilePool other) {
        int count = other.count;
        int capacity = ((count * 4) / 3) + 1;
        JakeFilePool ret = new JakeFilePool(count, capacity, count);

        // Copy store
        System.arraycopy(other.store, 0, ret.store, 0, count);

        // Copy table
        Bucket bucket;
        for (int i = 0; i < other.tableCapacity; i++) {
            bucket = other.table[i];
            while (bucket != null) {
                int value = bucket.value;
                int h = hash(other.store[value], capacity);
                ret.table[h] = new Bucket(value, ret.table[h]);
                bucket = bucket.next;
            }
        }

        return ret;
    }

    //

    private Entry[] store;
    private int storeCapacity;
    private @Nullable Bucket[] table;
    private int tableCapacity;
    private int count;

    private JakeFilePool(
            int storeCapacity,
            int tableCapacity,
            int count
    ) {
        this.store = new Entry[storeCapacity];
        this.storeCapacity = storeCapacity;
        this.table = new Bucket[tableCapacity];
        this.tableCapacity = tableCapacity;
        this.count = count;
    }

    JakeFilePool(int initialStoreCapacity) {
        this(initialStoreCapacity, Buffers.hashCapacity(initialStoreCapacity), 0);
    }

    JakeFilePool() {
        this(DEFAULT_STORE_CAPACITY);
    }

    //

    public int size() {
        return this.count;
    }

    public @UnmodifiableView List<Entry> view() {
        return Collections.unmodifiableList(Arrays.asList(this.store).subList(0, this.count));
    }

    public Entry get(int index) {
        if (index < 0 || index >= this.count)
            throw new IndexOutOfBoundsException("index " + index + " out of bounds for length " + this.count);
        return this.store[index];
    }

    public int intern(Entry value) {
        int hash = this.hash(value);

        Bucket existingRoot = this.table[hash];
        Bucket existing = existingRoot;
        while (existing != null) {
            int index = existing.value;
            if (this.store[index].equals(value)) return index;
            existing = existing.next;
        }

        int index = this.addToStore(value);
        this.table[hash] = new Bucket(index, existingRoot);

        int storeCapacity = this.storeCapacity;
        int threshold = (storeCapacity * 3) / 4;
        if (this.count >= threshold) this.rekey(Buffers.doubleCapacity(storeCapacity));

        return index;
    }

    private int addToStore(Entry value) {
        int head = this.count;
        Entry[] store = this.store;
        int capacity = this.storeCapacity;

        if (head == capacity) {
            capacity = Buffers.doubleCapacity(capacity);
            store = new Entry[capacity];
            System.arraycopy(this.store, 0, store, 0, head);
            this.store = store;
            this.storeCapacity = capacity;
        }

        store[head] = value;
        this.count = head + 1;
        return head;
    }

    private void rekey(int newCapacity) {
        Bucket[] newTable = new Bucket[newCapacity];
        for (int i = 0; i < this.tableCapacity; i++) {
            Bucket bucket = this.table[i];
            while (bucket != null) {
                int value = bucket.value;
                int hash = hash(this.store[value], newCapacity);
                newTable[hash] = new Bucket(
                        value,
                        newTable[hash]
                );
                bucket = bucket.next;
            }
        }
        this.table = newTable;
        this.tableCapacity = newCapacity;
    }

    @Override
    public String toString() {
        return this.view().toString();
    }

    private int hash(Object value) {
        return hash(value, this.tableCapacity);
    }

    private static int hash(Object object, int mod) {
        return Integer.remainderUnsigned(object.hashCode(), mod);
    }

    //

    private static final class Bucket {

        final int value;
        final @Nullable Bucket next;

        Bucket(int value, @Nullable Bucket next) {
            this.value = value;
            this.next = next;
        }

    }

    public interface Entry {

        String asString();

        TypeDescriptor asDescriptor() throws IllegalStateException;

    }

    private static abstract class EntryImpl<T> implements Entry {

        protected final T value;

        private EntryImpl(T value) {
            this.value = value;
        }

        //

        @Override
        public int hashCode() {
            return this.value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof EntryImpl<?> &&
                    this.value.equals(((EntryImpl<?>) obj).value);
        }

        @Override
        public String toString() {
            return this.value.toString();
        }

    }

    private static final class StringEntryImpl extends EntryImpl<String> {

        private StringEntryImpl(String value) {
            super(value);
        }

        //

        @Override
        public String asString() {
            return this.value;
        }

        @Override
        public TypeDescriptor asDescriptor() throws IllegalStateException {
            throw new IllegalStateException("Pool entry is not a type descriptor");
        }

    }

    private static final class DescriptorEntryImpl extends EntryImpl<TypeDescriptor> {

        private DescriptorEntryImpl(TypeDescriptor value) {
            super(value);
        }

        //

        @Override
        public String asString() {
            return this.value.toString();
        }

        @Override
        public TypeDescriptor asDescriptor() {
            return this.value;
        }

    }

}
