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
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

@NullMarked
@ApiStatus.Internal
final class ConstructorDescriptorImpl extends AbstractExecutableDescriptor<Constructor<?>> implements ConstructorDescriptor {

    ConstructorDescriptorImpl(
            TypeDescriptor declaringClass,
            List<TypeDescriptor> arguments
    ) {
        super(declaringClass, arguments);
    }

    //

    @Override
    public Constructor<?> resolve(ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        Class<?> cls = this.declaringClass.resolve(classLoader);
        return cls.getDeclaredConstructor(this.resolveArguments(classLoader));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.declaringClass, this.arguments);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MethodDescriptor)) return false;
        MethodDescriptor other = (MethodDescriptor) obj;
        return this.declaringClass.equals(other.declaringClass()) &&
                this.arguments.equals(other.arguments());
    }

    @Override
    @SuppressWarnings("PatternValidation")
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.declaringClass.asClassName())
                .append('(');

        for (TypeDescriptor arg : this.arguments) {
            sb.append(arg);
        }

        return sb.append(')')
                .toString();
    }

}
