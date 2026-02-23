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
package io.github.wasabithumb.jakery.find.except;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.IOException;

public final class ClassFinderIOException extends ClassFinderException {

    private static final long serialVersionUID = -5019378782961675953L;

    //

    public ClassFinderIOException(@NonNull String message, @NotNull IOException cause) {
        super(message, cause);
    }

    public ClassFinderIOException(@NotNull IOException cause) {
        this("Unexpected IO error while finding classes", cause);
    }

    //

    @Override
    public @NotNull IOException getCause() {
        return (IOException) super.getCause();
    }

}
