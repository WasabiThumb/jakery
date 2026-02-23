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

import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

@NullMarked
@ApiStatus.Internal
final class ObjectTypeDescriptor implements TypeDescriptor {

    private final String className;

    ObjectTypeDescriptor(
            @Pattern(CLASS_NAME_PATTERN) String className
    ) {
        this.className = className;
    }

    //


    @Override
    public int length() {
        return 2 + this.className.length();
    }

    @Override
    public Class<?> resolve(ClassLoader classLoader) throws ClassNotFoundException {
        int len = this.className.length();
        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            char c = this.className.charAt(i);
            if (c == '/') c = '.';
            buf[i] = c;
        }
        String name = new String(buf);
        return Class.forName(name, false, classLoader);
    }

    @Override
    public boolean isClassName() {
        return true;
    }

    @Override
    public String asClassName() {
        return this.className;
    }

    @Override
    public int hashCode() {
        return this.className.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ObjectTypeDescriptor)) return false;
        return this.className.equals(((ObjectTypeDescriptor) obj).className);
    }

    @Override
    public String toString() {
        return "L" + this.className + ";";
    }

}
