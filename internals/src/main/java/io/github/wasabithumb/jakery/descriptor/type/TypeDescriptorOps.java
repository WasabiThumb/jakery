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

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@ApiStatus.Internal
final class TypeDescriptorOps {

    // State flags
    private static final int S_VALIDATE = 1;
    private static final int S_CONVERT = 2;
    private static final int S_MUST_BE_CLASS = 4;
    private static final int S_MUST_BE_PACKAGE = 8;
    private static final int S_CONTINUING = 16;

    // C2PD: Lookup primitive descriptors by character (J, I, C, S, B, D, F, Z, V)
    private static final int C2PD_MIN;
    private static final int C2PD_MAX;
    private static final @Nullable PrimitiveTypeDescriptor[] C2PD;

    static {
        final PrimitiveTypeDescriptor[] primitiveDescriptors = PrimitiveTypeDescriptor.values();

        // C2PD
        int c2pdMin = 65536;
        int c2pdMax = -1;

        for (PrimitiveTypeDescriptor descriptor : primitiveDescriptors) {
            int value = descriptor.value();
            c2pdMin = Math.min(c2pdMin, value);
            c2pdMax = Math.max(c2pdMax, value);
        }

        PrimitiveTypeDescriptor[] c2pd = new PrimitiveTypeDescriptor[c2pdMax - c2pdMin + 1];
        for (PrimitiveTypeDescriptor descriptor : primitiveDescriptors) {
            int value = descriptor.value();
            c2pd[value - c2pdMin] = descriptor;
        }

        C2PD_MIN = c2pdMin;
        C2PD_MAX = c2pdMax;
        C2PD = c2pd;
    }

    //

    static TypeDescriptor of(CharSequence classifier) {
        int d = arrayDepth(classifier);
        char c0 = classifier.charAt(d);

        PrimitiveTypeDescriptor primitive = primitiveOf(c0);
        if (primitive != null) return withArrayDepth(primitive, d);
        if (c0 != 'L') {
            throw new IllegalArgumentException(
                    "Invalid classifier \"" + classifier +
                            "\" (expected signifying character)"
            );
        }

        int end = classifier.length() - 1;
        if (end <= d || classifier.charAt(end) != ';') {
            throw new IllegalArgumentException(
                    "Invalid classifier \"" + classifier +
                            "\" (expected end of class name)"
            );
        }

        return withArrayDepth(objectOf(classifier.subSequence(d + 1, end).toString(), S_VALIDATE), d);
    }

    static TypeDescriptor of(Class<?> cls) {
        int d = 0;
        while (cls.isArray()) {
            cls = cls.getComponentType();
            d++;
        }

        if (cls.isPrimitive()) {
            return withArrayDepth(primitiveOf(cls), d);
        }

        String name = cls.getCanonicalName();
        if (name == null) {
            throw new IllegalArgumentException(cls + " has no canonical name");
        }

        return withArrayDepth(objectOf(name, S_CONVERT), d);
    }

    @SuppressWarnings("PatternValidation")
    private static ObjectTypeDescriptor objectOf(
            String classNameSource,
            @MagicConstant(flagsFromClass = TypeDescriptorOps.class) int state
    ) {
        int len = classNameSource.length();
        char[] buf = checkFlags(state, S_CONVERT) ? new char[len] : null;

        char c;
        for (int i = 0; i < len; i++) {
            c = classNameSource.charAt(i);
            if (checkFlags(state, S_CONVERT)) {
                if (c == '.') c = '/';
                assert buf != null;
                buf[i] = c;
            }
            if (!checkFlags(state, S_VALIDATE)) continue;
            if (c == '/') {
                if (checkFlags(state, S_MUST_BE_CLASS)) {
                    throw invalidClassName(classNameSource, "uppercase letters or underscores found in package name");
                }
                if (!checkFlags(state, S_CONTINUING)) {
                    throw invalidClassName(classNameSource, "empty package name");
                }
                state &= ~(S_CONTINUING | S_MUST_BE_PACKAGE);
                continue;
            } else if (c == '+' || c == '-') {
                if (checkFlags(state, S_MUST_BE_CLASS)) {
                    throw invalidClassName(classNameSource, "plus/minus not allowed in class name");
                }
                if (!checkFlags(state, S_CONTINUING)) {
                    throw invalidClassName(classNameSource, "plus/minus cannot be first character in package name");
                }
                state |= S_MUST_BE_PACKAGE;
            } else if (c == '$') {
                if (checkFlags(state, S_MUST_BE_PACKAGE)) {
                    throw invalidClassName(classNameSource, "dollar sign not allowed in package name");
                }
                if (!checkFlags(state, S_CONTINUING)) {
                    throw invalidClassName(classNameSource, "empty declaring class name");
                }
                state |= S_MUST_BE_CLASS;
                state &= ~(S_CONTINUING);
                continue;
            } else if (c == '_') {
                if (checkFlags(state, S_MUST_BE_CLASS) && !checkFlags(state, S_CONTINUING)) {
                    throw invalidClassName(classNameSource, "underscore cannot be first character in class name");
                }
            } else if (c < 0x30 || (0x3A <= c && c <= 0x40) || (0x5B <= c && c <= 0x60) || c > 0x7A) {
                throw invalidClassName(classNameSource, "illegal character at index " + i);
            } else if (c <= '9') {
                if (!checkFlags(state, S_CONTINUING)) {
                    throw invalidClassName(classNameSource, "digit cannot be first character in package or class name");
                }
            } else if (c <= 'Z') {
                if (checkFlags(state, S_MUST_BE_PACKAGE)) {
                    throw invalidClassName(classNameSource, "uppercase letters not allowed in package name");
                }
                state |= S_MUST_BE_CLASS;
            }
            state |= S_CONTINUING;
        }

        if (checkFlags(state, S_VALIDATE)) {
            if (checkFlags(state, S_MUST_BE_PACKAGE)) {
                throw invalidClassName(classNameSource, "plus/minus found in class name");
            }
            if (!checkFlags(state, S_CONTINUING)) {
                throw invalidClassName(classNameSource, "empty class name");
            }
        }

        String className;
        if (checkFlags(state, S_CONVERT)) {
            assert buf != null;
            className = new String(buf);
        } else {
            className = classNameSource;
        }
        return new ObjectTypeDescriptor(className);
    }

    static @Nullable PrimitiveTypeDescriptor primitiveOf(char c) {
        return primitiveOf((int) c);
    }

    private static @Nullable PrimitiveTypeDescriptor primitiveOf(int c) {
        if (c < C2PD_MIN || c > C2PD_MAX) return null;
        return C2PD[c - C2PD_MIN];
    }

    private static PrimitiveTypeDescriptor primitiveOf(Class<?> cls) {
        String name = cls.getName();
        switch (name.charAt(0)) {
            case 'v': // void
                return PrimitiveTypeDescriptor.VOID;
            case 'b': // boolean, byte
                return name.charAt(1) == 'y' ?
                        PrimitiveTypeDescriptor.BYTE :
                        PrimitiveTypeDescriptor.BOOLEAN;
            case 's': // short
                return PrimitiveTypeDescriptor.SHORT;
            case 'c': // char
                return PrimitiveTypeDescriptor.CHAR;
            case 'i': // int
                return PrimitiveTypeDescriptor.INT;
            case 'l': // long
                return PrimitiveTypeDescriptor.LONG;
            case 'f': // float
                return PrimitiveTypeDescriptor.FLOAT;
            case 'd': // double
                return PrimitiveTypeDescriptor.DOUBLE;
            default:
                throw new IllegalStateException("Unable to match primitive type with name \"" + name + "\"");
        }
    }

    private static int arrayDepth(CharSequence classifier) {
        int length = classifier.length();
        if (length > 255) {
            for (int i = 0; i <= 255; i++) {
                if (classifier.charAt(i) != '[') return i;
            }
            throw new IllegalArgumentException("Too many array dimensions (exceeds 255)");
        } else {
            for (int i = 0; i < length; i++) {
                if (classifier.charAt(i) != '[') return i;
            }
            throw new IllegalArgumentException("Empty descriptor");
        }
    }

    private static TypeDescriptor withArrayDepth(TypeDescriptor base, int depth) {
        return depth < 1 ? base : new ArrayTypeDescriptor(base, depth);
    }

    private static boolean checkFlags(
            @MagicConstant(flagsFromClass = TypeDescriptorOps.class) int haystack,
            @MagicConstant(flagsFromClass = TypeDescriptorOps.class) int needle
    ) {
        return (haystack & needle) == needle;
    }

    private static IllegalArgumentException invalidClassName(String className, String reason) {
        return new IllegalArgumentException("Invalid class name \"" + className + "\" (" + reason + ")");
    }

    //

    private TypeDescriptorOps() { }

}
