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
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NonNls;
import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Modifier;

@NullMarked
public sealed interface FieldSet permits FieldSetImpl {

    FieldSet withModifiers(@MagicConstant(flagsFromClass = Modifier.class) int modifiers);

    FieldSet withoutModifiers(@MagicConstant(flagsFromClass = Modifier.class) int modifiers);

    FieldSet withAccessFlags(AccessFlag... flags);

    FieldSet withoutAccessFlags(AccessFlag... flags);

    FieldSet withAnnotation(@ClassName @NonNls String annotationClass);

    FieldSet withAnnotation(Class<? extends Annotation> annotationClass);

    FieldSet withoutAnnotation(@ClassName @NonNls String annotationClass);

    FieldSet withoutAnnotation(Class<? extends Annotation> annotationClass);

    FieldSet withName(@NonNls String name);

    FieldSet withType(@Descriptor @NonNls String descriptor);

    FieldSet withType(Class<?> clazz);

}
