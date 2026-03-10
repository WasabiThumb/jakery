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
package io.github.wasabithumb.jakery.gradle.shadow

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

/**
 * Applies the JakeryFileTransformer to transform
 * Jakery files. This allows class names to be relocated
 * and groups from distinct agent sets to be merged.
 */
fun ShadowJar.mergeJakeryFiles() {
    transform(JakeryFileTransformer::class.java)
}