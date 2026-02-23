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
package io.github.wasabithumb.jakery.agent;

import io.github.wasabithumb.jakery.find.ClassParser;
import io.github.wasabithumb.jakery.find.except.ClassFinderParseException;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;

@NullMarked
@ApiStatus.Internal
final class ModelClassParser implements ClassParser<ClassModel> {

    private static final ModelClassParser INSTANCE = new ModelClassParser();

    public static ModelClassParser modelClassParser() {
        return INSTANCE;
    }

    //

    private ModelClassParser() { }

    //

    @Override
    public ClassModel parse(InputStream in) throws IOException {
        try {
            return ClassFile.of().parse(in.readAllBytes());
        } catch (IllegalArgumentException e) {
            throw new ClassFinderParseException("Failed to parse class file", e);
        }
    }

}
