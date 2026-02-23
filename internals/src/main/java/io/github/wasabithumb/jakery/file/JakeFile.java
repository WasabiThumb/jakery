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

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.function.Consumer;

@NullMarked
@ApiStatus.NonExtendable
public interface JakeFile {

    @Contract("-> new")
    static Builder builder() {
        return new JakeFileImpl.Builder();
    }

    @Contract("_ -> new")
    static JakeFile read(InputStream in) throws IOException {
        return JakeFileIO.read(in);
    }

    //

    Set<String> keys();

    @Nullable JakeFileGroup<?> group(String key);

    void write(OutputStream out) throws IOException;

    //

    interface Builder {

        @Contract("_, _ -> this")
        Builder typeGroup(String name, Consumer<JakeFileGroup.Builder.OfTypes> configure);

        @Contract("_, _ -> this")
        Builder fieldGroup(String name, Consumer<JakeFileGroup.Builder.OfFields> configure);

        @Contract("_, _ -> this")
        Builder methodGroup(String name, Consumer<JakeFileGroup.Builder.OfMethods> configure);

        @Contract("_, _ -> this")
        Builder constructorGroup(String name, Consumer<JakeFileGroup.Builder.OfConstructors> configure);

        @Contract("-> new")
        JakeFile build();

    }

}
