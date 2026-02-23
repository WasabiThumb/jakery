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

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Entry point for the Jakery runtime,
 * used to resolve type, field, method
 * and constructor groups declared
 * at compile time.
 * @see #jakery()
 */
@NullMarked
@ApiStatus.NonExtendable
public interface Jakery {

    /**
     * Provides a contextually relevant
     * {@link Jakery} instance.
     * @see #jakery(ClassLoader)
     */
    static Jakery jakery() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = ClassLoader.getSystemClassLoader();
        return jakery(cl);
    }

    /**
     * Provides a {@link Jakery} instance for the
     * given {@link ClassLoader}. This is useful for cases when
     * different class loaders may resolve separate index files.
     */
    static Jakery jakery(ClassLoader loader) {
        return JakeryImpl.getOrCreate(loader);
    }

    //

    /**
     * Retrieves a named group, asserting that it is a
     * group of types (classes). This set is resolved lazily
     * and should be copied for stable access.
     * @param name Name of the group. Should have a matching {@code @Group} annotation in the agent source.
     */
    @Unmodifiable Set<Class<?>> typeGroup(String name);

    /**
     * Retrieves a named group, asserting that it is a
     * group of fields. This set is resolved lazily
     * and should be copied for stable access.
     * @param name Name of the group. Should have a matching {@code @Group} annotation in the agent source.
     */
    @Unmodifiable Set<Field> fieldGroup(String name);

    /**
     * Retrieves a named group, asserting that it is a
     * group of methods. This set is resolved lazily
     * and should be copied for stable access.
     * @param name Name of the group. Should have a matching {@code @Group} annotation in the agent source.
     */
    @Unmodifiable Set<Method> methodGroup(String name);

    /**
     * Retrieves a named group, asserting that it is a
     * group of constructors. This set is resolved lazily
     * and should be copied for stable access.
     * @param name Name of the group. Should have a matching {@code @Group} annotation in the agent source.
     */
    @Unmodifiable Set<Constructor<?>> constructorGroup(String name);

}
