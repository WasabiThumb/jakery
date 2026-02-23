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
package io.github.wasabithumb.jakery.find;

import io.github.wasabithumb.jakery.descriptor.type.TypeDescriptor;
import io.github.wasabithumb.jakery.find.except.ClassFinderException;
import io.github.wasabithumb.jakery.find.except.ClassFinderIOException;
import io.github.wasabithumb.jakery.util.ms.MultiString;
import org.intellij.lang.annotations.MagicConstant;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

@NullMarked
public interface ClassFinder {

    int FIND_DEEP = 1;
    int FIND_INNER = 2;

    static ClassFinder forContextClass(Class<?> contextClass, boolean searchJrt) throws ClassFinderException {
        return ClassFinderFactory.forContextClass(contextClass, searchJrt);
    }

    static ClassFinder forClassLoader(ClassLoader classLoader, boolean searchJrt) throws ClassFinderException {
        return ClassFinderFactory.forClassLoader(classLoader, searchJrt);
    }

    //

    Stream<Entry> find(
            @Nullable MultiString packageName,
            @MagicConstant(flagsFromClass = ClassFinder.class) int flags
    ) throws ClassFinderException;

    @Nullable InputStream getResource(MultiString path) throws ClassFinderException;

    //

    interface Entry {

        TypeDescriptor descriptor();

        InputStream read() throws ClassFinderException;

        default <T> T read(ClassParser<T> parser) throws ClassFinderException {
            InputStream in = this.read();
            try {
                return parser.parse(in);
            } catch (IOException e) {
                throw new ClassFinderIOException("Failed to parse class data for type " + this.descriptor(), e);
            }
        }

    }

}
