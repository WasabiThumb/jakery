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
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NonNls;
import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;

@NullMarked
public sealed interface ClassSet permits ClassSetImpl {

    static ClassSet of(Class<?>... classes) {
        return new ClassSetImpl(Arrays.asList(classes));
    }

    static ClassSet of(Collection<? extends Class<?>> classes) {
        return new ClassSetImpl(classes);
    }

    //

    ClassSet withModifiers(@MagicConstant(flagsFromClass = Modifier.class) int modifiers);

    ClassSet withoutModifiers(@MagicConstant(flagsFromClass = Modifier.class) int modifiers);

    ClassSet withAccessFlags(AccessFlag... flags);

    ClassSet withoutAccessFlags(AccessFlag... flags);

    ClassSet withAnnotation(@ClassName @NonNls String annotationClass);

    ClassSet withAnnotation(Class<? extends Annotation> annotationClass);

    ClassSet withoutAnnotation(@ClassName @NonNls String annotationClass);

    ClassSet withoutAnnotation(Class<? extends Annotation> annotationClass);

    ClassSet withSupertype(@ClassName @NonNls String superType);

    ClassSet withSupertype(Class<?> superType);

    FieldSet fields();

    MethodSet methods();

    ConstructorSet constructors();

}
