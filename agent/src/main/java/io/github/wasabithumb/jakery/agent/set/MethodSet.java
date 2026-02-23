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
public sealed interface MethodSet permits MethodSetImpl {

    MethodSet withModifiers(@MagicConstant(flagsFromClass = Modifier.class) int modifiers);

    MethodSet withoutModifiers(@MagicConstant(flagsFromClass = Modifier.class) int modifiers);

    MethodSet withAccessFlags(AccessFlag... flags);

    MethodSet withoutAccessFlags(AccessFlag... flags);

    MethodSet withAnnotation(@ClassName @NonNls String annotationClass);

    MethodSet withAnnotation(Class<? extends Annotation> annotationClass);

    MethodSet withoutAnnotation(@ClassName @NonNls String annotationClass);

    MethodSet withoutAnnotation(Class<? extends Annotation> annotationClass);

    MethodSet withName(@NonNls String name);

    MethodSet withReturnType(@Descriptor @NonNls String descriptor);

    MethodSet withReturnType(Class<?> clazz);

    MethodSet withArgumentCount(int count);

    MethodSet withoutArgumentCount(int count);

    MethodSet withArgumentTypes(@Descriptor @NonNls String type0, @NonNls String... typeN);

    MethodSet withoutArgumentTypes(@Descriptor @NonNls String type0, @NonNls String... typeN);

    MethodSet withArgumentTypes(Class<?> type0, Class<?>... typeN);

    MethodSet withoutArgumentTypes(Class<?> type0, Class<?>... typeN);

}
