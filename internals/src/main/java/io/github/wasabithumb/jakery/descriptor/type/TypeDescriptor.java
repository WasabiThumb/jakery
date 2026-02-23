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
package io.github.wasabithumb.jakery.descriptor.type;

import io.github.wasabithumb.jakery.descriptor.Descriptor;
import org.intellij.lang.annotations.Language;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@ApiStatus.NonExtendable
public interface TypeDescriptor extends Descriptor {

    @Language("RegExp")
    String CLASS_NAME_PATTERN = "(?:(?:[a-z_][a-z0-9+\\-_]*/)*)?[A-Za-z][A-Za-z0-9_]*(?:\\$[A-Za-z][A-Za-z0-9_]*)*";

    @Language("RegExp")
    String PATTERN = "(\\[*)([JICSBDFZV]|L(" + CLASS_NAME_PATTERN + ");)";

    //

    static TypeDescriptor of(@Pattern(PATTERN) String classifier) throws IllegalArgumentException {
        return TypeDescriptorOps.of(classifier);
    }

    static TypeDescriptor of(CharSequence classifier) throws IllegalArgumentException {
        return TypeDescriptorOps.of(classifier);
    }

    static TypeDescriptor of(Class<?> type) {
        return TypeDescriptorOps.of(type);
    }

    static @Nullable TypeDescriptor primitiveOf(char c) {
        return TypeDescriptorOps.primitiveOf(c);
    }

    //

    int length();

    Class<?> resolve(ClassLoader classLoader) throws ClassNotFoundException;

    default boolean isClassName() {
        return false;
    }

    default @Pattern(CLASS_NAME_PATTERN) String asClassName() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Pattern(PATTERN) String toString();

}
