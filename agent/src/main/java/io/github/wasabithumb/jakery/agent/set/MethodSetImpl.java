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

import io.github.wasabithumb.jakery.descriptor.type.TypeDescriptor;
import io.github.wasabithumb.jakery.file.JakeFile;
import io.github.wasabithumb.jakery.file.JakeFileApplicable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Annotation;
import java.lang.classfile.MethodModel;
import java.lang.reflect.AccessFlag;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.github.wasabithumb.jakery.agent.set.SetUtil.*;

@NullMarked
@ApiStatus.Internal
record MethodSetImpl(
        Stream<MethodModel> stream
) implements MethodSet, JakeFileApplicable {

    @Override
    public MethodSet withModifiers(int modifiers) {
        return this.filter((m) -> (extractModifiers(m.flags()) & modifiers) == modifiers);
    }

    @Override
    public MethodSet withoutModifiers(int modifiers) {
        return this.filter((m) -> (extractModifiers(m.flags()) & modifiers) == 0);
    }

    @Override
    public MethodSet withAccessFlags(AccessFlag... flags) {
        final int mask = accessFlagArrayToMask(flags);
        return this.filter((m) -> (m.flags().flagsMask() & mask) == mask);
    }

    @Override
    public MethodSet withoutAccessFlags(AccessFlag... flags) {
        final int mask = accessFlagArrayToMask(flags);
        return this.filter((m) -> (m.flags().flagsMask() & mask) == 0);
    }

    @Override
    public MethodSet withAnnotation(String annotationClass) {
        final TypeDescriptor desc = classNameToDescriptor(annotationClass);
        return this.filter((m) ->
                allAnnotations(m)
                        .map((a) -> classDescToDescriptor(a.classSymbol()))
                        .anyMatch(desc::equals)
        );
    }

    @Override
    public MethodSet withAnnotation(Class<? extends Annotation> annotationClass) {
        final TypeDescriptor desc = TypeDescriptor.of(annotationClass);
        return this.filter((m) ->
                allAnnotations(m)
                        .map((a) -> classDescToDescriptor(a.classSymbol()))
                        .anyMatch(desc::equals)
        );
    }

    @Override
    public MethodSet withoutAnnotation(String annotationClass) {
        final TypeDescriptor desc = classNameToDescriptor(annotationClass);
        return this.filter((m) ->
                allAnnotations(m)
                        .map((a) -> classDescToDescriptor(a.classSymbol()))
                        .noneMatch(desc::equals)
        );
    }

    @Override
    public MethodSet withoutAnnotation(Class<? extends Annotation> annotationClass) {
        final TypeDescriptor desc = TypeDescriptor.of(annotationClass);
        return this.filter((m) ->
                allAnnotations(m)
                        .map((a) -> classDescToDescriptor(a.classSymbol()))
                        .noneMatch(desc::equals)
        );
    }

    @Override
    public MethodSet withName(String name) {
        return this.filter((m) -> CharSequence.compare(name, m.methodName()) == 0);
    }

    @Override
    public MethodSet withReturnType(String descriptor) {
        final TypeDescriptor desc = stringToDescriptor(descriptor);
        return this.filter((m) -> desc.equals(classDescToDescriptor(m.methodTypeSymbol().returnType())));
    }

    @Override
    public MethodSet withReturnType(Class<?> clazz) {
        final TypeDescriptor desc = TypeDescriptor.of(clazz);
        return this.filter((m) -> desc.equals(classDescToDescriptor(m.methodTypeSymbol().returnType())));
    }

    @Override
    public MethodSet withArgumentCount(int count) {
        return this.filter((m) -> count == m.methodTypeSymbol().parameterCount());
    }

    @Override
    public MethodSet withoutArgumentCount(int count) {
        return this.filter((m) -> count != m.methodTypeSymbol().parameterCount());
    }

    @Override
    public MethodSet withArgumentTypes(String type0, @NonNls String... typeN) {
        List<TypeDescriptor> args = makeTypeDescriptorList(type0, typeN);
        return this.filter((m) -> args.equals(methodTypeDescToDescriptors(m.methodTypeSymbol())));
    }

    @Override
    public MethodSet withoutArgumentTypes(String type0, @NonNls String... typeN) {
        List<TypeDescriptor> args = makeTypeDescriptorList(type0, typeN);
        return this.filter((m) -> !args.equals(methodTypeDescToDescriptors(m.methodTypeSymbol())));
    }

    @Override
    public MethodSet withArgumentTypes(Class<?> type0, Class<?>... typeN) {
        List<TypeDescriptor> args = makeTypeDescriptorList(type0, typeN);
        return this.filter((m) -> args.equals(methodTypeDescToDescriptors(m.methodTypeSymbol())));
    }

    @Override
    public MethodSet withoutArgumentTypes(Class<?> type0, Class<?>... typeN) {
        List<TypeDescriptor> args = makeTypeDescriptorList(type0, typeN);
        return this.filter((m) -> !args.equals(methodTypeDescToDescriptors(m.methodTypeSymbol())));
    }

    @Override
    public void apply(String group, JakeFile.Builder builder) {
        final Iterator<MethodModel> iter = this.stream.iterator();
        builder.methodGroup(group, (b) -> {
            MethodModel next;
            while (iter.hasNext()) {
                next = iter.next();
                b.add(methodModelToMethodDescriptor(next));
            }
        });
    }

    private MethodSet filter(Predicate<? super MethodModel> predicate) {
        return new MethodSetImpl(this.stream.filter(predicate));
    }

}
