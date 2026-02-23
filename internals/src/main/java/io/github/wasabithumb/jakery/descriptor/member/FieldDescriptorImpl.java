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

import java.lang.reflect.Field;

@NullMarked
@ApiStatus.Internal
final class FieldDescriptorImpl
        extends AbstractMemberDescriptor<Field, NoSuchFieldException>
        implements FieldDescriptor
{

    private final String name;

    FieldDescriptorImpl(
            TypeDescriptor declaringClass,
            @Pattern(NAME_PATTERN) String name
    ) {
        super(declaringClass);
        this.name = name;
    }

    //

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Field resolve(ClassLoader classLoader) throws ClassNotFoundException, NoSuchFieldException {
        Class<?> cls = this.declaringClass.resolve(classLoader);
        return cls.getDeclaredField(this.name);
    }

    @Override
    public int hashCode() {
        return 31 * this.declaringClass.hashCode() + this.name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FieldDescriptor)) return false;
        FieldDescriptor other = (FieldDescriptor) obj;
        return this.declaringClass.equals(other.declaringClass()) &&
                this.name.equals(other.name());
    }

    @Override
    @SuppressWarnings("PatternValidation")
    public String toString() {
        return this.declaringClass.asClassName() + "#" + this.name;
    }

}
