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
package io.github.wasabithumb.jakery.agent;

import io.github.wasabithumb.jakery.agent.patterns.PackageName;
import io.github.wasabithumb.jakery.agent.set.ClassSet;
import io.github.wasabithumb.jakery.file.JakeFile;
import io.github.wasabithumb.jakery.find.ClassFinder;
import io.github.wasabithumb.jakery.util.ms.MultiString;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.classfile.ClassModel;
import java.util.stream.Stream;

/**
 * Annotation-powered index generator for Jakery.
 * Subclasses are expected to declare one or more
 * methods annotated with {@link Group @Group}, each
 * generating a single Jakery group by returning
 * a {@link ClassSet},
 * {@link io.github.wasabithumb.jakery.agent.set.FieldSet FieldSet},
 * {@link io.github.wasabithumb.jakery.agent.set.MethodSet MethodSet}
 * or {@link io.github.wasabithumb.jakery.agent.set.ConstructorSet ConstructorSet}.
 * A {@link ClassSet} can be created using the {@link #find()} methods
 * or directly via {@link ClassSet#of(Class[]) ClassSet#of}.
 * This also serves as the entry point for creating field,
 * method and constructor sets.
 */
@NullMarked
public abstract class JakeryAgent {

    private @Nullable ClassFinder classFinder;

    public JakeryAgent() {
        this.classFinder = null;
    }

    //

    /**
     * Determines whether the Java runtime filesystem is searched.
     * This drastically increases initialization time, but is necessary
     * in order to find classes within the Java standard library.
     * By default, only the runtime classpath is searched.
     */
    @ApiStatus.OverrideOnly
    protected boolean shouldSearchJrt() {
        return false;
    }

    /**
     * Alias for {@code find(null, FindOption.DEEP)}.
     * @see #find(String, FindOption...)
     */
    protected final ClassSet find() {
        return this.find((String) null, FindOption.DEEP);
    }

    /**
     * Produces a stream of types representing classes on the classpath.
     * If {@link #shouldSearchJrt()} is true at earliest invocation, the
     * Java runtime will also be searched. This may take a while.
     * @param pkg The package to search in. If null, searches the root package.
     * @param options Determines which classes are reported.
     */
    protected final ClassSet find(@Nullable Package pkg, FindOption... options) {
        //noinspection PatternValidation
        return this.find(pkg == null ? null : pkg.getName(), options);
    }

    /**
     * Produces a stream of types representing classes on the classpath.
     * If {@link #shouldSearchJrt()} is true at earliest invocation, the
     * Java runtime will also be searched. This may take a while.
     * @param packageName The package to search in. If null, searches the root package.
     * @param options Determines which classes are reported.
     */
    protected final ClassSet find(@Nullable @NonNls @PackageName String packageName, FindOption... options) {
        int flags = 0;
        for (FindOption option : options) flags |= option.value;
        Stream<ClassModel> src = this.classFinder()
                .find(packageName == null ? null : MultiString.parse(packageName), flags)
                .map((ClassFinder.Entry entry) -> entry.read(ModelClassParser.modelClassParser()));
        return JakeryAgentMagic.newClassSet(src);
    }

    private synchronized ClassFinder classFinder() {
        ClassFinder ret = this.classFinder;
        if (ret == null) this.classFinder = ret = ClassFinder.forContextClass(this.getClass(), this.shouldSearchJrt());
        return ret;
    }

    /**
     * Write the groups declared by this agent as a
     * Jakery index file.
     */
    public final void write(OutputStream out) throws IOException {
        JakeFile.Builder builder = JakeFile.builder();
        for (ResolvedGroup group : JakeryAgentMagic.resolveGroups(this)) {
            group.data().apply(group.name(), builder);
        }
        JakeFile file = builder.build();
        file.write(out);
    }

    //

    protected static final class FindOption {

        /** If present, the classpath will be searched recursively to find classes in subpackages. */
        public static final FindOption DEEP;

        /** If present, all inner classes of a given class will be reported in addition to the given class. */
        public static final FindOption INNER;

        static {
            DEEP = new FindOption("DEEP", ClassFinder.FIND_DEEP);
            INNER = new FindOption("INNER", ClassFinder.FIND_INNER);
        }

        //

        private final String name;
        private final @MagicConstant(valuesFromClass = ClassFinder.class) int value;

        private FindOption(
                String name,
                @MagicConstant(valuesFromClass = ClassFinder.class) int value
        ) {
            this.name = name;
            this.value = value;
        }

        //

        @Override
        public int hashCode() {
            return Integer.hashCode(this.value);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FindOption other && this.value == other.value;
        }

        @Override
        public String toString() {
            return this.name;
        }

    }

}
