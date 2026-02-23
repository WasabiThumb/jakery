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

import io.github.wasabithumb.jakery.descriptor.Descriptor;
import io.github.wasabithumb.jakery.descriptor.member.ConstructorDescriptor;
import io.github.wasabithumb.jakery.descriptor.member.FieldDescriptor;
import io.github.wasabithumb.jakery.descriptor.member.MethodDescriptor;
import io.github.wasabithumb.jakery.descriptor.type.TypeDescriptor;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.util.*;

@NullMarked
@ApiStatus.Internal
abstract class JakeFileGroupImpl<T extends Descriptor> implements JakeFileGroup<T> {

    protected final String name;
    protected final JakeFilePool pool;
    protected final JakeFileBlob blob;

    JakeFileGroupImpl(
            String name,
            JakeFilePool pool,
            JakeFileBlob blob
    ) {
        this.name = name;
        this.pool = pool;
        this.blob = blob;
    }

    //

    @Override
    public String name() {
        return this.name;
    }

    JakeFileBlob blob() {
        return this.blob;
    }

    protected abstract T read(JakeFileBlob.Reader reader);

    protected abstract int entrySize(int offset);

    protected int reportSize() {
        int count = 0;
        int head = 0;
        while (head < this.blob.size()) {
            head += this.entrySize(head);
            count++;
        }
        return count;
    }

    @Override
    public Set<T> elements() {
        return new Elements<>(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.type(), this.blob);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JakeFileGroupImpl<?>)) return false;
        JakeFileGroupImpl<?> other = (JakeFileGroupImpl<?>) obj;
        return this.name.equals(other.name()) &&
                this.type() == other.type() &&
                this.blob.equals(other.blob);
    }

    @Override
    public String toString() {
        return "JakeFileGroup{name=" + this.name + "}";
    }

    //

    static final class OfTypes extends JakeFileGroupImpl<TypeDescriptor> {

        OfTypes(String name, JakeFilePool pool, JakeFileBlob blob) {
            super(name, pool, blob);
        }

        //

        @Override
        public Type type() {
            return Type.TYPE;
        }

        @Override
        protected int entrySize(int offset) {
            return 1;
        }

        @Override
        protected int reportSize() {
            return this.blob.size();
        }

        @Override
        protected TypeDescriptor read(JakeFileBlob.Reader reader) {
            return this.pool.get(reader.next()).asDescriptor();
        }

        @Override
        public JakeFileGroup<TypeDescriptor> asTypeGroup() {
            return this;
        }

        //

        static final class Builder implements JakeFileGroup.Builder.OfTypes {

            private final String name;
            private final JakeFilePool pool;
            private final JakeFileBlob.Builder blob;

            Builder(String name, JakeFilePool pool) {
                this.name = name;
                this.pool = pool;
                this.blob = JakeFileBlob.builder();
            }

            //

            @Override
            public OfTypes add(TypeDescriptor descriptor) {
                int index = this.pool.intern(JakeFilePool.entry(descriptor));
                this.blob.put(index);
                return this;
            }

            @Override
            public JakeFileGroup<TypeDescriptor> build() {
                return new JakeFileGroupImpl.OfTypes(this.name, this.pool, this.blob.build());
            }
            
        }

    }

    static final class OfFields extends JakeFileGroupImpl<FieldDescriptor> {

        OfFields(String name, JakeFilePool pool, JakeFileBlob blob) {
            super(name, pool, blob);
        }

        //

        @Override
        public Type type() {
            return Type.FIELD;
        }

        @Override
        protected int entrySize(int offset) {
            return 2;
        }

        @Override
        protected int reportSize() {
            return this.blob.size() >> 1;
        }

        @Override
        @SuppressWarnings("PatternValidation")
        protected FieldDescriptor read(JakeFileBlob.Reader reader) {
            int cls = reader.next();
            int name = reader.next();
            return FieldDescriptor.of(
                    this.pool.get(cls).asDescriptor(),
                    this.pool.get(name).asString()
            );
        }

        @Override
        public JakeFileGroup<FieldDescriptor> asFieldGroup() {
            return this;
        }
        
        //
        
        static final class Builder implements JakeFileGroup.Builder.OfFields {
            
            private final String name;
            private final JakeFilePool pool;
            private final JakeFileBlob.Builder blob;

            Builder(String name, JakeFilePool pool) {
                this.name = name;
                this.pool = pool;
                this.blob = JakeFileBlob.builder();
            }

            //

            @Override
            public OfFields add(FieldDescriptor descriptor) {
                int cls = this.pool.intern(JakeFilePool.entry(descriptor.declaringClass()));
                int name = this.pool.intern(JakeFilePool.entry(descriptor.name()));
                this.blob.put(cls, name);
                return this;
            }

            @Override
            public JakeFileGroup<FieldDescriptor> build() {
                return new JakeFileGroupImpl.OfFields(this.name, this.pool, this.blob.build());
            }

        }

    }

    static final class OfMethods extends JakeFileGroupImpl<MethodDescriptor> {

        OfMethods(String name, JakeFilePool pool, JakeFileBlob blob) {
            super(name, pool, blob);
        }

        //

        @Override
        public Type type() {
            return Type.METHOD;
        }

        @Override
        protected int entrySize(int offset) {
            return 3 + this.blob.get(offset);
        }

        @Override
        @SuppressWarnings("PatternValidation")
        protected MethodDescriptor read(JakeFileBlob.Reader reader) {
            int paramCount = reader.next();
            int cls = reader.next();
            int name = reader.next();

            TypeDescriptor[] params = new TypeDescriptor[paramCount];
            for (int i = 0; i < params.length; i++) {
                params[i] = this.pool.get(reader.next()).asDescriptor();
            }

            return MethodDescriptor.of(
                    this.pool.get(cls).asDescriptor(),
                    this.pool.get(name).asString(),
                    Arrays.asList(params)
            );
        }

        @Override
        public JakeFileGroup<MethodDescriptor> asMethodGroup() {
            return this;
        }

        //

        static final class Builder implements JakeFileGroup.Builder.OfMethods {

            private final String name;
            private final JakeFilePool pool;
            private final JakeFileBlob.Builder blob;

            Builder(String name, JakeFilePool pool) {
                this.name = name;
                this.pool = pool;
                this.blob = JakeFileBlob.builder();
            }
            
            //
            
            @Override
            public OfMethods add(MethodDescriptor descriptor) {
                List<TypeDescriptor> args = descriptor.arguments();
                int argCount = args.size();
                int cls = this.pool.intern(JakeFilePool.entry(descriptor.declaringClass()));
                int name = this.pool.intern(JakeFilePool.entry(descriptor.name()));
                this.blob.put(argCount, cls, name);
                
                for (int i = 0; i < argCount; i++) {
                    TypeDescriptor arg = args.get(i);
                    int id = this.pool.intern(JakeFilePool.entry(arg));
                    this.blob.put(id);
                }
                
                return this;
            }

            @Override
            public JakeFileGroup<MethodDescriptor> build() {
                return new JakeFileGroupImpl.OfMethods(this.name, this.pool, this.blob.build());
            }
            
        }

    }

    static final class OfConstructors extends JakeFileGroupImpl<ConstructorDescriptor> {

        OfConstructors(String name, JakeFilePool pool, JakeFileBlob blob) {
            super(name, pool, blob);
        }

        //

        @Override
        public Type type() {
            return Type.CONSTRUCTOR;
        }

        @Override
        protected int entrySize(int offset) {
            return 2 + this.blob.get(offset);
        }

        @Override
        protected ConstructorDescriptor read(JakeFileBlob.Reader reader) {
            int paramCount = reader.next();
            int cls = reader.next();

            TypeDescriptor[] params = new TypeDescriptor[paramCount];
            for (int i = 0; i < params.length; i++) {
                params[i] = this.pool.get(reader.next()).asDescriptor();
            }

            return ConstructorDescriptor.of(
                    this.pool.get(cls).asDescriptor(),
                    Arrays.asList(params)
            );
        }

        @Override
        public JakeFileGroup<ConstructorDescriptor> asConstructorGroup() {
            return this;
        }
        
        //
        
        static final class Builder implements JakeFileGroup.Builder.OfConstructors {
            
            private final String name;
            private final JakeFilePool pool;
            private final JakeFileBlob.Builder blob;

            Builder(String name, JakeFilePool pool) {
                this.name = name;
                this.pool = pool;
                this.blob = JakeFileBlob.builder();
            }

            //

            @Override
            public OfConstructors add(ConstructorDescriptor descriptor) {
                List<TypeDescriptor> args = descriptor.arguments();
                int argCount = args.size();
                int cls = this.pool.intern(JakeFilePool.entry(descriptor.declaringClass()));
                this.blob.put(argCount, cls);

                for (int i = 0; i < argCount; i++) {
                    TypeDescriptor arg = args.get(i);
                    int id = this.pool.intern(JakeFilePool.entry(arg));
                    this.blob.put(id);
                }

                return this;
            }

            @Override
            public JakeFileGroup<ConstructorDescriptor> build() {
                return new JakeFileGroupImpl.OfConstructors(this.name, this.pool, this.blob.build());
            }

        }

    }

    //

    private static final class Elements<T extends Descriptor> extends AbstractSet<T> {

        private final JakeFileGroupImpl<T> parent;

        private Elements(JakeFileGroupImpl<T> parent) {
            this.parent = parent;
        }

        //

        @Override
        public int size() {
            return this.parent.reportSize();
        }

        @Override
        public Iterator<T> iterator() {
            return new Iter<>(this.parent);
        }

        //

        private static final class Iter<T extends Descriptor> implements Iterator<T> {

            private final JakeFileGroupImpl<T> parent;
            private final JakeFileBlob.Reader reader;

            private Iter(
                    JakeFileGroupImpl<T> parent
            ) {
                this.parent = parent;
                this.reader = parent.blob.newReader();
            }

            //

            @Override
            public boolean hasNext() {
                return this.reader.hasNext();
            }

            @Override
            public T next() {
                if (!this.reader.hasNext()) throw new NoSuchElementException();
                return this.parent.read(this.reader);
            }

        }

    }

}
