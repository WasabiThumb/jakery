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
package io.github.wasabithumb.jakery.agent.set;

import io.github.wasabithumb.jakery.agent.patterns.ClassName;
import io.github.wasabithumb.jakery.agent.patterns.Descriptor;
import io.github.wasabithumb.jakery.descriptor.member.ConstructorDescriptor;
import io.github.wasabithumb.jakery.descriptor.member.FieldDescriptor;
import io.github.wasabithumb.jakery.descriptor.member.MethodDescriptor;
import io.github.wasabithumb.jakery.descriptor.type.TypeDescriptor;
import io.github.wasabithumb.jakery.util.ms.MultiString;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.*;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@NullMarked
@ApiStatus.Internal
final class SetUtil {

    private static final int ACCESS_FLAG_MODIFIER_MASK;
    static {
        int accessFlagModifierMask = 0;
        for (AccessFlag flag : AccessFlag.values()) {
            if (!flag.sourceModifier()) continue;
            accessFlagModifierMask |= flag.mask();
        }
        ACCESS_FLAG_MODIFIER_MASK = accessFlagModifierMask;
    }

    //

    @SuppressWarnings("PatternValidation")
    static TypeDescriptor stringToDescriptor(@Descriptor String descriptor) {
        return TypeDescriptor.of(descriptor);
    }

    @SuppressWarnings("PatternValidation")
    static TypeDescriptor classNameToDescriptor(@ClassName String className) {
        return TypeDescriptor.of("L" + MultiString.parse(className, '.').toString('/') + ";");
    }

    @SuppressWarnings("PatternValidation")
    static TypeDescriptor classDescToDescriptor(ClassDesc desc) {
        return TypeDescriptor.of(desc.descriptorString());
    }

    @SuppressWarnings("PatternValidation")
    static TypeDescriptor classEntryToDescriptor(ClassEntry entry) {
        String name = entry.asInternalName();
        if (!name.startsWith("[")) name = "L" + name + ";";
        return TypeDescriptor.of(name);
    }

    @SuppressWarnings("PatternValidation")
    static FieldDescriptor fieldModelToDescriptor(FieldModel model) {
        Optional<ClassModel> parent = model.parent();
        if (parent.isEmpty()) throw new IllegalStateException("Field has no known parent");
        return FieldDescriptor.of(
                classEntryToDescriptor(parent.get().thisClass()),
                model.fieldName().stringValue()
        );
    }

    @SuppressWarnings("PatternValidation")
    static MethodDescriptor methodModelToMethodDescriptor(MethodModel model) {
        Optional<ClassModel> parent = model.parent();
        if (parent.isEmpty()) throw new IllegalStateException("Method has no known parent");
        return MethodDescriptor.of(
                classEntryToDescriptor(parent.get().thisClass()),
                model.methodName().stringValue(),
                methodTypeDescToDescriptors(model.methodTypeSymbol())
        );
    }

    static ConstructorDescriptor methodModelToConstructorDescriptor(MethodModel model) {
        Optional<ClassModel> parent = model.parent();
        if (parent.isEmpty()) throw new IllegalStateException("Constructor has no known parent");
        return ConstructorDescriptor.of(
                classEntryToDescriptor(parent.get().thisClass()),
                methodTypeDescToDescriptors(model.methodTypeSymbol())
        );
    }

    static List<TypeDescriptor> methodTypeDescToDescriptors(MethodTypeDesc desc) {
        int count = desc.parameterCount();
        TypeDescriptor[] ret = new TypeDescriptor[count];
        for (int i = 0; i < count; i++) {
            ClassDesc cls = desc.parameterType(i);
            ret[i] = classDescToDescriptor(cls);
        }
        return List.of(ret);
    }

    static @MagicConstant(flagsFromClass = Modifier.class) int extractModifiers(AccessFlags flags) {
        //noinspection MagicConstant
        return flags.flagsMask() & ACCESS_FLAG_MODIFIER_MASK;
    }

    static int accessFlagArrayToMask(AccessFlag[] array) {
        int ret = 0;
        for (AccessFlag flag : array) ret |= flag.mask();
        return ret;
    }

    static Stream<Annotation> allAnnotations(AttributedElement element) {
        return Stream.concat(
                element.findAttributes(Attributes.runtimeVisibleAnnotations())
                        .stream()
                        .flatMap((attr) -> attr.annotations().stream()),
                element.findAttributes(Attributes.runtimeInvisibleAnnotations())
                        .stream()
                        .flatMap((attr) -> attr .annotations().stream())
        );
    }

    static Stream<ClassEntry> superTypes(ClassModel cm) {
        Optional<ClassEntry> cls = cm.superclass();
        List<ClassEntry> ifaces = cm.interfaces();

        int state = 0;
        if (cls.isPresent()) state |= 1;
        if (!ifaces.isEmpty()) state |= 2;

        return switch (state) {
            case 0 -> Stream.empty();
            case 1 -> Stream.of(cls.get());
            case 2 -> ifaces.stream();
            case 3 -> Stream.concat(Stream.of(cls.get()), ifaces.stream());
            default -> throw new AssertionError("Unreachable code");
        };
    }

    @SuppressWarnings("PatternValidation")
    static ClassModel classToModel(Class<?> cls) {
        String descriptorString = cls.descriptorString();
        TypeDescriptor descriptor;
        try {
            descriptor = TypeDescriptor.of(descriptorString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Class " + cls.getName() + " has non-nominal descriptor string " + descriptorString,
                    e
            );
        }
        if (!descriptor.isClassName()) {
            throw new IllegalArgumentException(
                    "Descriptor " + descriptorString + " does not unambiguously refer to a class file"
            );
        }

        ClassLoader loader = cls.getClassLoader();
        String path = descriptor.asClassName() + ".class";
        byte[] bytes;

        try (InputStream in = loader.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Class " + cls.getName() + " cannot be found as resource in own class loader (reading " +
                                path + ")"
                );
            }
            bytes = in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read data for class " + cls.getName(), e);
        }

        ClassFile cf = ClassFile.of();
        try {
            return cf.parse(bytes);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Data for class " + cls.getName() + " could not be parsed", e);
        }
    }

    private static <T> List<TypeDescriptor> makeTypeDescriptorList(Function<T, TypeDescriptor> map, T first, T[] nth) {
        final int count = nth.length + 1;
        TypeDescriptor[] ret = new TypeDescriptor[count];
        ret[0] = map.apply(first);
        for (int i = 0; i < nth.length; i++) {
            ret[i + 1] = map.apply(nth[i]);
        }
        return List.of(ret);
    }

    static List<TypeDescriptor> makeTypeDescriptorList(@Descriptor String first, String[] nth) {
        return makeTypeDescriptorList(TypeDescriptor::of, first, nth);
    }

    static List<TypeDescriptor> makeTypeDescriptorList(Class<?> first, Class<?>[] nth) {
        return makeTypeDescriptorList(TypeDescriptor::of, first, nth);
    }

    static boolean isMethodApplicable(MethodModel model, boolean ctor) {
        Utf8Entry entry = model.methodName();
        if (CharSequence.compare(entry, "<clinit>") == 0) return false;
        return ctor == (CharSequence.compare(entry, "<init>") == 0);
    }

    static boolean isMethodConstructor(MethodModel model) {
        return isMethodApplicable(model, true);
    }

    static boolean isMethodNonInitializer(MethodModel model) {
        return isMethodApplicable(model, false);
    }

    //

    private SetUtil() { }

}
