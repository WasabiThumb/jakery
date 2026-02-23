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

import io.github.wasabithumb.jakery.descriptor.Descriptor;
import io.github.wasabithumb.jakery.descriptor.type.TypeDescriptor;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.Member;

@NullMarked
@ApiStatus.NonExtendable
public interface MemberDescriptor<M extends Member, X extends ReflectiveOperationException> extends Descriptor {

    @Language("RegExp")
    String NAME_PATTERN = "[A-Za-z_$][\\dA-Za-z_$]*";

    //

    M resolve(ClassLoader classLoader) throws ClassNotFoundException, X;

    TypeDescriptor declaringClass();

}
