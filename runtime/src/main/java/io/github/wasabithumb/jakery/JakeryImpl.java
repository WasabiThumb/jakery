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
package io.github.wasabithumb.jakery;

import io.github.wasabithumb.jakery.descriptor.Descriptor;
import io.github.wasabithumb.jakery.descriptor.member.ConstructorDescriptor;
import io.github.wasabithumb.jakery.descriptor.member.FieldDescriptor;
import io.github.wasabithumb.jakery.descriptor.member.MethodDescriptor;
import io.github.wasabithumb.jakery.descriptor.type.TypeDescriptor;
import io.github.wasabithumb.jakery.file.JakeFile;
import io.github.wasabithumb.jakery.file.JakeFileGroup;
import io.github.wasabithumb.jakery.find.ClassFinder;
import io.github.wasabithumb.jakery.util.ms.MultiString;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

@NullMarked
@ApiStatus.Internal
final class JakeryImpl implements Jakery {

    private static final WeakHashMap<ClassLoader, JakeryImpl> CACHE = new WeakHashMap<>();
    private static final StampedLock CACHE_LOCK = new StampedLock();

    static JakeryImpl getOrCreate(ClassLoader loader) {
        long stamp = CACHE_LOCK.readLock();
        try {
            JakeryImpl value = CACHE.get(loader);
            if (value != null) return value;

            long tmp = CACHE_LOCK.tryConvertToWriteLock(stamp);
            if (tmp != -1) {
                stamp = tmp;
            } else {
                CACHE_LOCK.unlock(stamp);
                stamp = CACHE_LOCK.writeLock();
                value = CACHE.get(loader);
                if (value != null) return value;
            }

            try {
                value = load(loader);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load Jakery index file", e);
            }
            CACHE.put(loader, value);
            return value;
        } finally {
            CACHE_LOCK.unlock(stamp);
        }
    }

    private static JakeryImpl load(ClassLoader loader) throws IOException {
        JakeFile file;
        try (InputStream in = indexStream(loader)) {
            file = JakeFile.read(in);
        }
        return new JakeryImpl(loader, file);
    }

    private static InputStream indexStream(ClassLoader loader) {
        InputStream stream = loader.getResourceAsStream("/META-INF/.jakery");
        if (stream == null) {
            stream = ClassFinder.forClassLoader(loader, false)
                    .getResource(MultiString.of("META-INF", ".jakery"));

            if (stream == null) {
                throw new IllegalStateException("Jakery index file could not be found");
            }
        }
        return stream;
    }

    //

    private final ClassLoader classLoader;
    private final JakeFile file;

    JakeryImpl(
            ClassLoader classLoader,
            JakeFile file
    ) {
        this.classLoader = classLoader;
        this.file = file;
    }

    //

    @Override
    public @Unmodifiable Set<Class<?>> typeGroup(String name) {
        return new TypeGroup(
                this.classLoader,
                this.group(name, JakeFileGroup.Type.TYPE)
                        .asTypeGroup()
                        .elements()
        );
    }

    @Override
    public @Unmodifiable Set<Field> fieldGroup(String name) {
        return new FieldGroup(
                this.classLoader,
                this.group(name, JakeFileGroup.Type.FIELD)
                        .asFieldGroup()
                        .elements()
        );
    }

    @Override
    public @Unmodifiable Set<Method> methodGroup(String name) {
        return new MethodGroup(
                this.classLoader,
                this.group(name, JakeFileGroup.Type.METHOD)
                        .asMethodGroup()
                        .elements()
        );
    }

    @Override
    public @Unmodifiable Set<Constructor<?>> constructorGroup(String name) {
        return new ConstructorGroup(
                this.classLoader,
                this.group(name, JakeFileGroup.Type.CONSTRUCTOR)
                        .asConstructorGroup()
                        .elements()
        );
    }

    private JakeFileGroup<?> group(String name, JakeFileGroup.Type expectedType) {
        JakeFileGroup<?> ret = this.file.group(name);
        if (ret == null) throw new IllegalStateException("No group exists with name \"" + name + "\"");
        if (ret.type() != expectedType) {
            throw new IllegalStateException(
                    "Group \"" + name + "\" is of type " + ret.type().name() +
                    " (expected " + expectedType.name() + ")"
            );
        }
        return ret;
    }

    //

    private static final class TypeGroup extends Group<TypeDescriptor, Class<?>> {

        TypeGroup(ClassLoader classLoader, Set<TypeDescriptor> backing) {
            super(classLoader, backing);
        }

        @Override
        protected Class<?> fromDescriptor(TypeDescriptor value) throws ReflectiveOperationException {
            return value.resolve(this.classLoader);
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Class<?>)) return false;
            return this.backing.contains(TypeDescriptor.of((Class<?>) o));
        }

    }

    private static final class FieldGroup extends Group<FieldDescriptor, Field> {

        FieldGroup(ClassLoader classLoader, Set<FieldDescriptor> backing) {
            super(classLoader, backing);
        }

        @Override
        protected Field fromDescriptor(FieldDescriptor value) throws ReflectiveOperationException {
            return value.resolve(this.classLoader);
        }

        @Override
        @SuppressWarnings("PatternValidation")
        public boolean contains(Object o) {
            if (!(o instanceof Field)) return false;
            Field field = (Field) o;
            return this.backing.contains(FieldDescriptor.of(
                    TypeDescriptor.of(field.getDeclaringClass()),
                    field.getName()
            ));
        }

    }

    private static final class MethodGroup extends Group<MethodDescriptor, Method> {

        MethodGroup(ClassLoader classLoader, Set<MethodDescriptor> backing) {
            super(classLoader, backing);
        }

        @Override
        protected Method fromDescriptor(MethodDescriptor value) throws ReflectiveOperationException {
            return value.resolve(this.classLoader);
        }

        @Override
        @SuppressWarnings("PatternValidation")
        public boolean contains(Object o) {
            if (!(o instanceof Method)) return false;
            Method method = (Method) o;
            return this.backing.contains(MethodDescriptor.of(
                    TypeDescriptor.of(method.getDeclaringClass()),
                    method.getName(),
                    Arrays.stream(method.getParameterTypes()).map(TypeDescriptor::of).collect(Collectors.toList())
            ));
        }

    }

    private static final class ConstructorGroup extends Group<ConstructorDescriptor, Constructor<?>> {

        ConstructorGroup(ClassLoader classLoader, Set<ConstructorDescriptor> backing) {
            super(classLoader, backing);
        }

        @Override
        protected Constructor<?> fromDescriptor(ConstructorDescriptor value) throws ReflectiveOperationException {
            return value.resolve(this.classLoader);
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Constructor<?>)) return false;
            Constructor<?> constructor = (Constructor<?>) o;
            return this.backing.contains(ConstructorDescriptor.of(
                    TypeDescriptor.of(constructor.getDeclaringClass()),
                    Arrays.stream(constructor.getParameterTypes()).map(TypeDescriptor::of).collect(Collectors.toList())
            ));
        }

    }

    private static abstract class Group<D extends Descriptor, T> extends AbstractSet<T> {

        protected final ClassLoader classLoader;
        protected final Set<D> backing;

        Group(
                ClassLoader classLoader,
                Set<D> backing
        ) {
            this.classLoader = classLoader;
            this.backing = backing;
        }

        //

        @Override
        public int size() {
            return this.backing.size();
        }

        @Override
        public Iterator<T> iterator() {
            return new Iter<>(this);
        }

        protected abstract T fromDescriptor(D value) throws ReflectiveOperationException;

        //

        private static final class Iter<D extends Descriptor, T> implements Iterator<T> {

            private final Group<D, T> parent;
            private final Iterator<D> backing;

            Iter(Group<D, T> parent) {
                this.parent = parent;
                this.backing = parent.backing.iterator();
            }

            //

            @Override
            public boolean hasNext() {
                return this.backing.hasNext();
            }

            @Override
            public T next() {
                D next = this.backing.next();
                T value;
                try {
                    value = this.parent.fromDescriptor(next);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Group descriptor " + next + " could not be resolved", e);
                }
                return value;
            }

        }

    }

}
