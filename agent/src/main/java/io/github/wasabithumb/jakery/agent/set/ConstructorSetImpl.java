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
record ConstructorSetImpl(
        Stream<MethodModel> stream
) implements ConstructorSet, JakeFileApplicable {

    @Override
    public ConstructorSet withModifiers(int modifiers) {
        return this.filter((m) -> (extractModifiers(m.flags()) & modifiers) == modifiers);
    }

    @Override
    public ConstructorSet withoutModifiers(int modifiers) {
        return this.filter((m) -> (extractModifiers(m.flags()) & modifiers) == 0);
    }

    @Override
    public ConstructorSet withAccessFlags(AccessFlag... flags) {
        final int mask = accessFlagArrayToMask(flags);
        return this.filter((m) -> (m.flags().flagsMask() & mask) == mask);
    }

    @Override
    public ConstructorSet withoutAccessFlags(AccessFlag... flags) {
        final int mask = accessFlagArrayToMask(flags);
        return this.filter((m) -> (m.flags().flagsMask() & mask) == 0);
    }

    @Override
    public ConstructorSet withAnnotation(String annotationClass) {
        final TypeDescriptor desc = classNameToDescriptor(annotationClass);
        return this.filter((m) ->
                allAnnotations(m)
                        .map((a) -> classDescToDescriptor(a.classSymbol()))
                        .anyMatch(desc::equals)
        );
    }

    @Override
    public ConstructorSet withAnnotation(Class<? extends Annotation> annotationClass) {
        final TypeDescriptor desc = TypeDescriptor.of(annotationClass);
        return this.filter((m) ->
                allAnnotations(m)
                        .map((a) -> classDescToDescriptor(a.classSymbol()))
                        .anyMatch(desc::equals)
        );
    }

    @Override
    public ConstructorSet withoutAnnotation(String annotationClass) {
        final TypeDescriptor desc = classNameToDescriptor(annotationClass);
        return this.filter((m) ->
                allAnnotations(m)
                        .map((a) -> classDescToDescriptor(a.classSymbol()))
                        .noneMatch(desc::equals)
        );
    }

    @Override
    public ConstructorSet withoutAnnotation(Class<? extends Annotation> annotationClass) {
        final TypeDescriptor desc = TypeDescriptor.of(annotationClass);
        return this.filter((m) ->
                allAnnotations(m)
                        .map((a) -> classDescToDescriptor(a.classSymbol()))
                        .noneMatch(desc::equals)
        );
    }

    @Override
    public ConstructorSet withArgumentCount(int count) {
        return this.filter((m) -> count == m.methodTypeSymbol().parameterCount());
    }

    @Override
    public ConstructorSet withoutArgumentCount(int count) {
        return this.filter((m) -> count != m.methodTypeSymbol().parameterCount());
    }

    @Override
    public ConstructorSet withArgumentTypes(String type0, @NonNls String... typeN) {
        List<TypeDescriptor> args = makeTypeDescriptorList(type0, typeN);
        return this.filter((m) -> args.equals(methodTypeDescToDescriptors(m.methodTypeSymbol())));
    }

    @Override
    public ConstructorSet withoutArgumentTypes(String type0, @NonNls String... typeN) {
        List<TypeDescriptor> args = makeTypeDescriptorList(type0, typeN);
        return this.filter((m) -> !args.equals(methodTypeDescToDescriptors(m.methodTypeSymbol())));
    }

    @Override
    public ConstructorSet withArgumentTypes(Class<?> type0, Class<?>... typeN) {
        List<TypeDescriptor> args = makeTypeDescriptorList(type0, typeN);
        return this.filter((m) -> args.equals(methodTypeDescToDescriptors(m.methodTypeSymbol())));
    }

    @Override
    public ConstructorSet withoutArgumentTypes(Class<?> type0, Class<?>... typeN) {
        List<TypeDescriptor> args = makeTypeDescriptorList(type0, typeN);
        return this.filter((m) -> !args.equals(methodTypeDescToDescriptors(m.methodTypeSymbol())));
    }

    @Override
    public void apply(String group, JakeFile.Builder builder) {
        final Iterator<MethodModel> iter = this.stream.iterator();
        builder.constructorGroup(group, (b) -> {
            MethodModel next;
            while (iter.hasNext()) {
                next = iter.next();
                b.add(methodModelToConstructorDescriptor(next));
            }
        });
    }

    private ConstructorSet filter(Predicate<? super MethodModel> predicate) {
        return new ConstructorSetImpl(this.stream.filter(predicate));
    }

}
