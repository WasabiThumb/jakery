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
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

@NullMarked
@ApiStatus.Internal
final class ArrayTypeDescriptor implements TypeDescriptor {

    private static final boolean HAS_CLASS_ARRAY_TYPE;
    private static final @UnknownNullability MethodHandle CLASS_ARRAY_TYPE;
    static {
        boolean hasClassArrayType = false;
        MethodHandle classArrayType = null;
        try {
            classArrayType = MethodHandles.publicLookup()
                    .findVirtual(Class.class, "arrayType", MethodType.methodType(Class.class));
            hasClassArrayType = true;
        } catch (Exception ignored) { }
        HAS_CLASS_ARRAY_TYPE = hasClassArrayType;
        CLASS_ARRAY_TYPE = classArrayType;
    }

    //

    private final TypeDescriptor componentType;
    private final int depth;

    ArrayTypeDescriptor(
            TypeDescriptor componentType,
            @Range(from = 1, to = 0xFFFF) int depth
    ) {
        this.componentType = componentType;
        this.depth = depth;
    }

    //

    @Override
    public int length() {
        return this.depth + this.componentType.length();
    }

    @Override
    public Class<?> resolve(ClassLoader classLoader) throws ClassNotFoundException {
        if (HAS_CLASS_ARRAY_TYPE) {
            Class<?> ret = this.componentType.resolve(classLoader);
            for (int i = 0; i < this.depth; i++) {
                try {
                    ret = (Class<?>) CLASS_ARRAY_TYPE.invokeExact(ret);
                } catch (RuntimeException re) {
                    throw re;
                } catch (Throwable t) {
                    throw new AssertionError("Class#arrayType raised an unexpected error", t);
                }
            }
            return ret;
        } else {
            String base = this.componentType.toString();
            int baseLength = base.length();
            char[] buf = new char[baseLength + this.depth];
            Arrays.fill(buf, 0, this.depth, '[');
            for (int i = 0; i < baseLength; i++) {
                char c = base.charAt(i);
                if (c == '/') c = '.';
                buf[this.depth + i] = c;
            }
            String padded = new String(buf);
            return Class.forName(padded, false, classLoader);
        }
    }

    @Override
    public TypeDescriptor arrayType() {
        return new ArrayTypeDescriptor(this, this.depth + 1);
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public TypeDescriptor componentType() {
        int nd = this.depth - 1;
        if (nd <= 0) return this.componentType;
        return new ArrayTypeDescriptor(this.componentType, nd);
    }

    @Override
    public int hashCode() {
        return 31 * this.componentType.hashCode() + this.depth;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ArrayTypeDescriptor)) return false;
        ArrayTypeDescriptor other = (ArrayTypeDescriptor) obj;
        return this.componentType.equals(other.componentType) &&
                this.depth == other.depth;
    }

    @Override
    @SuppressWarnings("PatternValidation")
    public String toString() {
        char[] s = this.componentType.toString().toCharArray();
        int sl = s.length;

        char[] buf = new char[this.depth + sl];
        Arrays.fill(buf, 0, this.depth, '[');
        System.arraycopy(s, 0, buf, this.depth, sl);

        return new String(buf);
    }

}
