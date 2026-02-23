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
package io.github.wasabithumb.jakery.descriptor.member;

import io.github.wasabithumb.jakery.descriptor.type.TypeDescriptor;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
@ApiStatus.Internal
final class MemberDescriptorOps {

    static FieldDescriptor newFieldDescriptor(
            TypeDescriptor declaringClass,
            String name
    ) {
        //noinspection PatternValidation
        return new FieldDescriptorImpl(declaringClass, validatedName(name));
    }

    static MethodDescriptor newMethodDescriptor(
            TypeDescriptor declaringClass,
            String name,
            List<TypeDescriptor> arguments
    ) {
        //noinspection PatternValidation
        return new MethodDescriptorImpl(declaringClass, validatedName(name), arguments);
    }

    static ConstructorDescriptor newConstructorDescriptor(
            TypeDescriptor declaringClass,
            List<TypeDescriptor> arguments
    ) {
        return new ConstructorDescriptorImpl(declaringClass, arguments);
    }

    @Contract("_ -> param1")
    @SuppressWarnings("PatternValidation")
    static @Pattern(MemberDescriptor.NAME_PATTERN) String validatedName(String name) throws IllegalArgumentException {
        int len = name.length();
        char c;

        for (int i = 0; i < len; i++) {
            c = name.charAt(i);
            if ('a' <= c && c <= 'z') continue;
            if ('A' <= c && c <= 'Z') continue;
            if (c == '_' || c == '$') continue;
            if ('0' <= c && c <= '9') {
                if (i != 0) continue;
                throw new IllegalArgumentException("Digit may not be first character in member name");
            }
            throw new IllegalArgumentException("Invalid character at position " + i + " in member name");
        }

        return name;
    }

    //

    private MemberDescriptorOps() { }

}
