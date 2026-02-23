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
import org.intellij.lang.annotations.Language;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.Constructor;
import java.util.List;

@NullMarked
@ApiStatus.NonExtendable
public interface ConstructorDescriptor extends ExecutableDescriptor<Constructor<?>> {

    @Language("RegExp")
    String PATTERN = TypeDescriptor.CLASS_NAME_PATTERN + "\\((?:" + TypeDescriptor.PATTERN + ")*\\)";

    @Contract("_, _ -> new")
    static ConstructorDescriptor of(
            TypeDescriptor declaringClass,
            List<TypeDescriptor> arguments
    ) {
        return MemberDescriptorOps.newConstructorDescriptor(declaringClass, arguments);
    }

    //

    @Override
    @Pattern(PATTERN) String toString();

}
