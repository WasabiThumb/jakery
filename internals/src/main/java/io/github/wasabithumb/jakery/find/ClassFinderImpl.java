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

import io.github.wasabithumb.jakery.descriptor.type.TypeDescriptor;
import io.github.wasabithumb.jakery.find.except.ClassFinderConflictException;
import io.github.wasabithumb.jakery.find.except.ClassFinderException;
import io.github.wasabithumb.jakery.find.except.ClassFinderIOException;
import io.github.wasabithumb.jakery.util.ms.MultiString;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.stream.Stream;

@NullMarked
@ApiStatus.Internal
final class ClassFinderImpl implements ClassFinder {

    private final ClassFinderNode root;

    ClassFinderImpl() {
        this.root = new ClassFinderNode();
    }

    //

    public void registerResource(MultiString path, ClassFinderResource resource) {
        this.resolve(path, false, true).putResource(resource);
    }

    @Override
    public Stream<Entry> find(@Nullable MultiString packageName, int flags) throws ClassFinderException {
        ClassFinderNode node = this.resolve(packageName, true, false);
        if (node == null) return Stream.empty();
        return this.find0(
                (packageName == null) ? MultiString.of() : packageName,
                node,
                flags
        );
    }

    @SuppressWarnings("PatternValidation")
    private Stream<Entry> find0(MultiString prefix, ClassFinderNode node, int flags) throws ClassFinderException {
        Stream<Entry> ret = node.entries()
                .filter((ClassFinderNode.Entry entry) -> {
                    if (entry.value().resource() == null) return false;
                    return acceptClassFileWithName(entry.label(), (flags & FIND_INNER) != 0);
                }).map((ClassFinderNode.Entry entry) -> {
                    String name = entry.label();
                    name = name.substring(0, name.length() - 6);
                    String descriptor = "L" + MultiString.join(prefix, MultiString.of(name)).toString('/') + ";";
                    ClassFinderResource resource = entry.value().resource();
                    assert resource != null;
                    return new EntryImpl(TypeDescriptor.of(descriptor), resource);
                });

        if ((flags & FIND_DEEP) == 0) {
            return ret;
        }

        Iterator<ClassFinderNode.Entry> iter = node.entries().iterator();
        ClassFinderNode.Entry next;
        while (iter.hasNext()) {
            next = iter.next();
            MultiString joinedPrefix = MultiString.join(prefix, MultiString.of(next.label()));
            Stream<Entry> sub = this.find0(joinedPrefix, next.value(), flags);
            ret = Stream.concat(ret, sub);
        }

        return ret;
    }

    @Override
    public @Nullable InputStream getResource(MultiString path) throws ClassFinderException {
        ClassFinderNode node = this.resolve(path, false, false);
        if (node == null) return null;
        ClassFinderResource resource = node.resource();
        if (resource == null) return null;
        if (resource.isAmbiguous()) throw new ClassFinderConflictException(path.toString('/'));
        try {
            return resource.open();
        } catch (IOException e) {
            throw new ClassFinderIOException("Failed to read resource " + path.toString('/'), e);
        }
    }

    @Contract("_, _, true -> !null")
    private @Nullable ClassFinderNode resolve(@Nullable MultiString path, boolean allowEmpty, boolean create) {
        ClassFinderNode ret = this.root;
        Iterator<String> iter;

        if (path == null || !(iter = path.iterator()).hasNext()) {
            if (allowEmpty) return ret;
            throw new IllegalArgumentException("Refusing to resolve empty path");
        }

        String next;
        while (true) {
            next = iter.next();
            if (iter.hasNext()) {
                if (create) {
                    ret = ret.childOrCreateBranch(next);
                } else {
                    ret = ret.child(next);
                    if (ret == null) return null;
                }
            } else {
                if (create) {
                    ret = ret.childOrCreateLeaf(next);
                } else {
                    ret = ret.child(next);
                }
                break;
            }
        }

        return ret;
    }

    private static boolean acceptClassFileWithName(String name, boolean allowInner) {
        if (!name.endsWith(".class")) return false;
        if (name.indexOf('-') != -1) return false;
        int whereSep = name.indexOf('$');
        while (whereSep != -1) {
            if (!allowInner) return false;
            int start = whereSep + 1;
            if (start >= (name.length() - 6)) return false;
            char c0 = name.charAt(start);
            if ('0' <= c0 && c0 <= '9') return false;
            whereSep = name.indexOf('$', start);
        }
        return true;
    }

    //

    private static final class EntryImpl implements Entry {

        private final TypeDescriptor descriptor;
        private final ClassFinderResource resource;

        EntryImpl(
                TypeDescriptor descriptor,
                ClassFinderResource resource
        ) {
            this.descriptor = descriptor;
            this.resource = resource;
        }

        //

        @Override
        public TypeDescriptor descriptor() {
            return this.descriptor;
        }

        @Override
        public InputStream read() throws ClassFinderException {
            try {
                return this.resource.open();
            } catch (IOException e) {
                throw new ClassFinderIOException("Failed to read class file", e);
            }
        }

    }

}
