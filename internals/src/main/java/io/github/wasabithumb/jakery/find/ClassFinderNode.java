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
package io.github.wasabithumb.jakery.find;

import io.github.wasabithumb.jakery.util.Buffers;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@NullMarked
@ApiStatus.Internal
final class ClassFinderNode {

    private int capacity;
    private int size;
    private String[] labels;
    private ClassFinderNode[] children;
    private @Nullable ClassFinderResource resource;

    private ClassFinderNode(int initialCapacity) {
        this.capacity = initialCapacity;
        this.size = 0;
        this.labels = new String[initialCapacity];
        this.children = new ClassFinderNode[initialCapacity];
    }

    ClassFinderNode() {
        this(16);
    }

    //

    public @Nullable ClassFinderResource resource() {
        return this.resource;
    }

    public void putResource(ClassFinderResource resource) {
        if (this.resource == null) {
            this.resource = resource;
        } else if (!this.resource.equals(resource)) {
            this.resource = ClassFinderResource.ambiguous();
        }
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public Stream<Entry> entries() {
        return IntStream.range(0, this.size)
                .mapToObj((int i) -> new Entry(this, i));
    }

    public @Nullable ClassFinderNode child(String label) {
        return this.child(label, null);
    }

    public ClassFinderNode childOrCreateBranch(String label) {
        return this.child(label, ClassFinderNode::new);
    }

    public ClassFinderNode childOrCreateLeaf(String label) {
        return this.child(label, () -> new ClassFinderNode(0));
    }

    @Contract("_, !null -> !null")
    private @Nullable ClassFinderNode child(String label, @Nullable Supplier<ClassFinderNode> generator) {
        int cmp;
        int i = 0;
        for (; i < this.size; i++) {
            cmp = label.compareTo(this.labels[i]);
            if (cmp == 0) return this.children[i];
            if (cmp < 0) break;
        }

        if (generator == null) return null;
        final ClassFinderNode ret = generator.get();

        if (this.size == this.capacity) {
            int nc = Buffers.doubleCapacity(this.capacity);
            String[] labelsCpy = new String[nc];
            ClassFinderNode[] childrenCpy = new ClassFinderNode[nc];
            System.arraycopy(this.labels, 0, labelsCpy, 0, this.size);
            System.arraycopy(this.children, 0, childrenCpy, 0, this.size);
            this.labels = labelsCpy;
            this.children = childrenCpy;
            this.capacity = nc;
        }

        if (i != this.size) {
            System.arraycopy(this.labels, i, this.labels, i + 1, this.size - i);
            System.arraycopy(this.children, i, this.children, i + 1, this.size - i);
        }

        this.labels[i] = label;
        this.children[i] = ret;
        this.size++;
        return ret;
    }

    //

    static final class Entry {

        private final ClassFinderNode parent;
        private final int index;

        private Entry(
                ClassFinderNode parent,
                int index
        ) {
            this.parent = parent;
            this.index = index;
        }

        //

        public String label() {
            return this.parent.labels[this.index];
        }

        public ClassFinderNode value() {
            return this.parent.children[this.index];
        }

    }

}
