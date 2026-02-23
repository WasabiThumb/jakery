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
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

@NullMarked
public sealed interface ConstructorSet permits ConstructorSetImpl {

    ConstructorSet withModifiers(@MagicConstant(flagsFromClass = Modifier.class) int modifiers);

    ConstructorSet withoutModifiers(@MagicConstant(flagsFromClass = Modifier.class) int modifiers);

    ConstructorSet withAccessFlags(AccessFlag... flags);

    ConstructorSet withoutAccessFlags(AccessFlag... flags);

    ConstructorSet withAnnotation(@ClassName @NonNls String annotationClass);

    ConstructorSet withAnnotation(Class<? extends Annotation> annotationClass);

    ConstructorSet withoutAnnotation(@ClassName @NonNls String annotationClass);

    ConstructorSet withoutAnnotation(Class<? extends Annotation> annotationClass);

    ConstructorSet withArgumentCount(int count);

    ConstructorSet withoutArgumentCount(int count);

    ConstructorSet withArgumentTypes(@Descriptor @NonNls String type0, @NonNls String... typeN);

    ConstructorSet withoutArgumentTypes(@Descriptor @NonNls String type0, @NonNls String... typeN);

    ConstructorSet withArgumentTypes(Class<?> type0, Class<?>... typeN);

    ConstructorSet withoutArgumentTypes(Class<?> type0, Class<?>... typeN);

}
