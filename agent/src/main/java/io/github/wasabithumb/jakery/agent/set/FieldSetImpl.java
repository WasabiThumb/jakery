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
import java.lang.classfile.FieldModel;
import java.lang.reflect.AccessFlag;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.github.wasabithumb.jakery.agent.set.SetUtil.*;

@NullMarked
@ApiStatus.Internal
record FieldSetImpl(
        Stream<FieldModel> stream
) implements FieldSet, JakeFileApplicable {

    @Override
    public FieldSet withModifiers(int modifiers) {
        return this.filter((FieldModel m) -> (extractModifiers(m.flags()) & modifiers) == modifiers);
    }

    @Override
    public FieldSet withoutModifiers(int modifiers) {
        return this.filter((FieldModel m) -> (extractModifiers(m.flags()) & modifiers) == 0);
    }

    @Override
    public FieldSet withAccessFlags(AccessFlag... flags) {
        final int mask = accessFlagArrayToMask(flags);
        return this.filter((FieldModel m) -> (m.flags().flagsMask() & mask) == mask);
    }

    @Override
    public FieldSet withoutAccessFlags(AccessFlag... flags) {
        final int mask = accessFlagArrayToMask(flags);
        return this.filter((FieldModel m) -> (m.flags().flagsMask() & mask) == 0);
    }

    @Override
    public FieldSet withAnnotation(String annotationClass) {
        final TypeDescriptor desc = classNameToDescriptor(annotationClass);
        return this.filter((FieldModel m) ->
                allAnnotations(m)
                        .map((a) -> classDescToDescriptor(a.classSymbol()))
                        .anyMatch(desc::equals)
        );
    }

    @Override
    public FieldSet withAnnotation(Class<? extends Annotation> annotationClass) {
        final TypeDescriptor desc = TypeDescriptor.of(annotationClass);
        return this.filter((FieldModel m) ->
                allAnnotations(m)
                        .map((a) -> classDescToDescriptor(a.classSymbol()))
                        .anyMatch(desc::equals)
        );
    }

    @Override
    public FieldSet withoutAnnotation(String annotationClass) {
        final TypeDescriptor desc = classNameToDescriptor(annotationClass);
        return this.filter((FieldModel m) ->
                allAnnotations(m)
                        .map((a) -> classDescToDescriptor(a.classSymbol()))
                        .noneMatch(desc::equals)
        );
    }

    @Override
    public FieldSet withoutAnnotation(Class<? extends Annotation> annotationClass) {
        final TypeDescriptor desc = TypeDescriptor.of(annotationClass);
        return this.filter((FieldModel m) ->
                allAnnotations(m)
                        .map((a) -> classDescToDescriptor(a.classSymbol()))
                        .noneMatch(desc::equals)
        );
    }

    @Override
    public FieldSet withName(String name) {
        return this.filter((FieldModel m) -> CharSequence.compare(name, m.fieldName()) == 0);
    }

    @Override
    public FieldSet withType(String descriptor) {
        final TypeDescriptor desc = stringToDescriptor(descriptor);
        return this.filter((FieldModel m) -> desc.equals(classDescToDescriptor(m.fieldTypeSymbol())));
    }

    @Override
    public FieldSet withType(Class<?> clazz) {
        final TypeDescriptor desc = TypeDescriptor.of(clazz);
        return this.filter((FieldModel m) -> desc.equals(classDescToDescriptor(m.fieldTypeSymbol())));
    }

    @Override
    public void apply(String group, JakeFile.Builder builder) {
        final Iterator<FieldModel> iter = this.stream.iterator();
        builder.fieldGroup(group, (b) -> {
            FieldModel next;
            while (iter.hasNext()) {
                next = iter.next();
                b.add(fieldModelToDescriptor(next));
            }
        });
    }

    private FieldSet filter(Predicate<? super FieldModel> predicate) {
        return new FieldSetImpl(this.stream.filter(predicate));
    }

}
