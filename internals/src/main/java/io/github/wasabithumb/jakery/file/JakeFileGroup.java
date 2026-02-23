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
package io.github.wasabithumb.jakery.file;

import io.github.wasabithumb.jakery.descriptor.Descriptor;
import io.github.wasabithumb.jakery.descriptor.member.ConstructorDescriptor;
import io.github.wasabithumb.jakery.descriptor.member.FieldDescriptor;
import io.github.wasabithumb.jakery.descriptor.member.MethodDescriptor;
import io.github.wasabithumb.jakery.descriptor.type.TypeDescriptor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

import java.util.Set;

@NullMarked
@ApiStatus.NonExtendable
public interface JakeFileGroup<T extends Descriptor> {

    Type type();

    String name();

    Set<T> elements();

    @Contract("-> this")
    default JakeFileGroup<TypeDescriptor> asTypeGroup() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Contract("-> this")
    default JakeFileGroup<FieldDescriptor> asFieldGroup() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Contract("-> this")
    default JakeFileGroup<MethodDescriptor> asMethodGroup() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Contract("-> this")
    default JakeFileGroup<ConstructorDescriptor> asConstructorGroup() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    //

    enum Type {
        TYPE,
        FIELD,
        METHOD,
        CONSTRUCTOR
    }

    interface Builder<T extends Descriptor> {

        @Contract("_ -> this")
        Builder<T> add(T descriptor);

        @Contract("-> new")
        JakeFileGroup<T> build();

        //

        interface OfTypes extends Builder<TypeDescriptor> {

            @Override
            @Contract("_ -> this")
            OfTypes add(TypeDescriptor descriptor);

        }

        interface OfFields extends Builder<FieldDescriptor> {

            @Override
            @Contract("_ -> this")
            OfFields add(FieldDescriptor descriptor);

        }

        interface OfMethods extends Builder<MethodDescriptor> {

            @Override
            @Contract("_ -> this")
            OfMethods add(MethodDescriptor descriptor);

        }

        interface OfConstructors extends Builder<ConstructorDescriptor> {

            @Override
            @Contract("_ -> this")
            OfConstructors add(ConstructorDescriptor descriptor);

        }

    }

}
