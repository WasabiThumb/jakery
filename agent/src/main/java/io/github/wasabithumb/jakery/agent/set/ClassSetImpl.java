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
import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Annotation;
import java.lang.classfile.ClassModel;
import java.lang.reflect.AccessFlag;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.github.wasabithumb.jakery.agent.set.SetUtil.*;

@NullMarked
@ApiStatus.Internal
record ClassSetImpl(
        Stream<ClassModel> stream
) implements ClassSet, JakeFileApplicable {

    ClassSetImpl(Collection<? extends Class<?>> classes) {
        this(Set.copyOf(classes).stream().map(SetUtil::classToModel));
    }

    //

    @Override
    public ClassSet withModifiers(int modifiers) {
        return this.filter((ClassModel cm) -> (extractModifiers(cm.flags()) & modifiers) == modifiers);
    }

    @Override
    public ClassSet withoutModifiers(int modifiers) {
        return this.filter((ClassModel cm) -> (extractModifiers(cm.flags()) & modifiers) == 0);
    }

    @Override
    public ClassSet withAccessFlags(AccessFlag... flags) {
        final int mask = accessFlagArrayToMask(flags);
        return this.filter((ClassModel cm) -> (cm.flags().flagsMask() & mask) == mask);
    }

    @Override
    public ClassSet withoutAccessFlags(AccessFlag... flags) {
        final int mask = accessFlagArrayToMask(flags);
        return this.filter((ClassModel cm) -> (cm.flags().flagsMask() & mask) == 0);
    }

    @Override
    public ClassSet withAnnotation(String annotationClass) {
        final TypeDescriptor desc = classNameToDescriptor(annotationClass);
        return this.filter((ClassModel cm) ->
                allAnnotations(cm)
                        .map((a) -> classDescToDescriptor(a.classSymbol()))
                        .anyMatch(desc::equals)
        );
    }

    @Override
    public ClassSet withAnnotation(Class<? extends Annotation> annotationClass) {
        final TypeDescriptor desc = TypeDescriptor.of(annotationClass);
        return this.filter((ClassModel cm) ->
                allAnnotations(cm)
                        .map((a) -> classDescToDescriptor(a.classSymbol()))
                        .anyMatch(desc::equals)
        );
    }

    @Override
    public ClassSet withoutAnnotation(String annotationClass) {
        final TypeDescriptor desc = classNameToDescriptor(annotationClass);
        return this.filter((ClassModel cm) ->
                allAnnotations(cm)
                        .map((a) -> classDescToDescriptor(a.classSymbol()))
                        .noneMatch(desc::equals)
        );
    }

    @Override
    public ClassSet withoutAnnotation(Class<? extends Annotation> annotationClass) {
        final TypeDescriptor desc = TypeDescriptor.of(annotationClass);
        return this.filter((ClassModel cm) ->
                allAnnotations(cm)
                        .map((a) -> classDescToDescriptor(a.classSymbol()))
                        .noneMatch(desc::equals)
        );
    }

    @Override
    public ClassSet withSupertype(String superType) {
        final TypeDescriptor desc = classNameToDescriptor(superType);
        return this.filter((ClassModel cm) ->
            superTypes(cm)
                    .map(SetUtil::classEntryToDescriptor)
                    .anyMatch(desc::equals)
        );
    }

    @Override
    public ClassSet withSupertype(Class<?> superType) {
        final TypeDescriptor desc = TypeDescriptor.of(superType);
        return this.filter((ClassModel cm) ->
                superTypes(cm)
                        .map(SetUtil::classEntryToDescriptor)
                        .anyMatch(desc::equals)
        );
    }

    @Override
    public FieldSet fields() {
        return this.flatMap(FieldSetImpl::new, (ClassModel cm) -> cm.fields().stream());
    }

    @Override
    public MethodSet methods() {
        return this.flatMap(
                MethodSetImpl::new,
                (ClassModel cm) -> cm.methods().stream().filter(SetUtil::isMethodNonInitializer)
        );
    }

    @Override
    public ConstructorSet constructors() {
        return this.flatMap(
                ConstructorSetImpl::new,
                (ClassModel cm) -> cm.methods().stream().filter(SetUtil::isMethodConstructor)
        );
    }

    @Override
    public void apply(String group, JakeFile.Builder builder) {
        final Iterator<ClassModel> iter = this.stream.iterator();
        builder.typeGroup(group, (b) -> {
            ClassModel next;
            while (iter.hasNext()) {
                next = iter.next();
                b.add(classEntryToDescriptor(next.thisClass()));
            }
        });
    }

    private ClassSet filter(Predicate<? super ClassModel> predicate) {
        return new ClassSetImpl(this.stream.filter(predicate));
    }

    private <E, T> E flatMap(Function<Stream<T>, E> box, Function<? super ClassModel, Stream<T>> mapper) {
        return box.apply(this.stream.flatMap(mapper));
    }

}
