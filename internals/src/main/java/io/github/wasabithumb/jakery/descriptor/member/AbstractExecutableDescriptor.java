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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@NullMarked
@ApiStatus.Internal
abstract class AbstractExecutableDescriptor<M extends Executable>
        extends AbstractMemberDescriptor<M, NoSuchMethodException>
        implements ExecutableDescriptor<M>
{

    private static @Unmodifiable List<TypeDescriptor> move(List<TypeDescriptor> src) {
        final int size = src.size();
        if (size > 255) throw new IllegalArgumentException("Too many arguments (count " + size + " exceeds 255)");
        List<TypeDescriptor> ret = new ArrayList<>(size);
        ret.addAll(src);
        return Collections.unmodifiableList(ret);
    }

    //

    protected final List<TypeDescriptor> arguments;

    protected AbstractExecutableDescriptor(
            TypeDescriptor declaringClass,
            List<TypeDescriptor> arguments
    ) {
        super(declaringClass);
        this.arguments = move(arguments);
    }

    //

    @Override
    public @Unmodifiable List<TypeDescriptor> arguments() {
        return this.arguments;
    }

    protected final Class<?>[] resolveArguments(ClassLoader classLoader) throws ClassNotFoundException {
        int len = this.arguments.size();
        Class<?>[] ret = new Class<?>[len];

        for (int i = 0; i < len; i++) {
            TypeDescriptor desc = this.arguments.get(i);
            ret[i] = desc.resolve(classLoader);
        }

        return ret;
    }

}
