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
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@NullMarked
@ApiStatus.Internal
final class JakeFileImpl implements JakeFile {

    private final JakeFilePool pool;
    private final Map<String, JakeFileGroupImpl<?>> map;

    JakeFileImpl(
            JakeFilePool pool,
            Map<String, JakeFileGroupImpl<?>> map
    ) {
        this.pool = pool;
        this.map = map;
    }

    //

    JakeFilePool pool() {
        return this.pool;
    }

    @Override
    public Set<String> keys() {
        return this.map.keySet();
    }

    @Override
    public @Nullable JakeFileGroupImpl<?> group(String key) {
        return this.map.get(key);
    }

    @Override
    public void write(OutputStream out) throws IOException {
        JakeFileIO.write(out, this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.pool, this.map);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JakeFileImpl)) return false;
        JakeFileImpl other = (JakeFileImpl) obj;
        return this.pool.equals(other.pool) &&
                this.map.equals(other.map);
    }

    @Override
    public String toString() {
        return this.map.toString();
    }

    //

    static final class Builder implements JakeFile.Builder {

        private final JakeFilePool pool;
        private final List<JakeFileGroupImpl<?>> groups;

        Builder() {
            this.pool = new JakeFilePool();
            this.groups = new LinkedList<>();
        }

        //

        @Override
        public Builder typeGroup(String name, Consumer<JakeFileGroup.Builder.OfTypes> configure) {
            return this.group(name, JakeFileGroupImpl.OfTypes.Builder::new, configure);
        }

        @Override
        public Builder fieldGroup(String name, Consumer<JakeFileGroup.Builder.OfFields> configure) {
            return this.group(name, JakeFileGroupImpl.OfFields.Builder::new, configure);
        }

        @Override
        public Builder methodGroup(String name, Consumer<JakeFileGroup.Builder.OfMethods> configure) {
            return this.group(name, JakeFileGroupImpl.OfMethods.Builder::new, configure);
        }

        @Override
        public Builder constructorGroup(String name, Consumer<JakeFileGroup.Builder.OfConstructors> configure) {
            return this.group(name, JakeFileGroupImpl.OfConstructors.Builder::new, configure);
        }

        private <T extends JakeFileGroup.Builder<?>> Builder group(
                String name,
                BiFunction<String, JakeFilePool, ? extends T> construct,
                Consumer<? super T> configure
        ) {
            T instance = construct.apply(name, this.pool);
            configure.accept(instance);
            this.groups.add((JakeFileGroupImpl<?>) instance.build());
            return this;
        }

        @Override
        public JakeFile build() {
            return new JakeFileImpl(
                    JakeFilePool.copyOf(this.pool),
                    this.createMap()
            );
        }

        private Map<String, JakeFileGroupImpl<?>> createMap() throws IllegalStateException {
            Map<String, JakeFileGroupImpl<?>> ret = new LinkedHashMap<>(Buffers.hashCapacity(this.groups.size()));
            for (JakeFileGroupImpl<?> next : this.groups) {
                String name = next.name();
                JakeFileGroupImpl<?> existing = ret.put(name, next);
                if (existing == null || existing.equals(next)) continue;
                throw new IllegalStateException("Multiple distinct groups with name \"" + name + "\"");
            }
            return Collections.unmodifiableMap(ret);
        }

    }

}
