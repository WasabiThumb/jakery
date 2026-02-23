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
package io.github.wasabithumb.jakery.util;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

@NullMarked
@ApiStatus.Internal
public final class Buffers {

    public static int doubleCapacity(int n) {
        if (n == 0x7FFFFFFF) throw new OutOfMemoryError("Cannot grow buffer past " + n + " elements");
        if (n >= 0x40000000) return 0x7FFFFFFF;
        if (n <= 0) return 1;
        return n << 1;
    }

    public static int hashCapacity(int n) {
        return (int) Math.ceil((double) n / 0.75d);
    }

    //

    private Buffers() { }

}
