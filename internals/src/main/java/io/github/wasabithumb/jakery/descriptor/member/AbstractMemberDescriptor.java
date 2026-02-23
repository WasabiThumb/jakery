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
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.Member;

@NullMarked
@ApiStatus.Internal
abstract class AbstractMemberDescriptor<M extends Member, X extends ReflectiveOperationException>
        implements MemberDescriptor<M, X>
{

    protected final TypeDescriptor declaringClass;

    protected AbstractMemberDescriptor(
            TypeDescriptor declaringClass
    ) {
        if (!declaringClass.isClassName())
            throw new IllegalArgumentException("Descriptor " + declaringClass + " does not refer to a class");
        this.declaringClass = declaringClass;
    }

    //

    @Override
    public TypeDescriptor declaringClass() {
        return this.declaringClass;
    }

}
