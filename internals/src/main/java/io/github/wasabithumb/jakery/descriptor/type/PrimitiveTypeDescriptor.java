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

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.NullMarked;

@NullMarked
@ApiStatus.Internal
enum PrimitiveTypeDescriptor implements TypeDescriptor {
    BYTE('B', Byte.TYPE),
    SHORT('S', Short.TYPE),
    INT('I', Integer.TYPE),
    LONG('J', Long.TYPE),
    CHAR('C', Character.TYPE),
    FLOAT('F', Float.TYPE),
    DOUBLE('D', Double.TYPE),
    BOOLEAN('Z', Boolean.TYPE),
    VOID('V', Void.TYPE);

    //

    private final char value;
    private final Class<?> type;

    PrimitiveTypeDescriptor(
            @Range(from = 'B', to = 'Z') char value,
            Class<?> type
    ) {
        this.value = value;
        this.type = type;
    }

    //

    public @Range(from = 'B', to = 'Z') int value() {
        return this.value;
    }

    @Override
    public int length() {
        return 1;
    }

    @Override
    public Class<?> resolve(ClassLoader ignored) {
        return this.type;
    }

    @Override
    @SuppressWarnings("PatternValidation")
    public String toString() {
        return String.valueOf(this.value);
    }

}
