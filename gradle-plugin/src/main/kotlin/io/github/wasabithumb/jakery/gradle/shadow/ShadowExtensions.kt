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