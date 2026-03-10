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

import com.github.jengelman.gradle.plugins.shadow.relocation.relocatePath
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowCopyAction
import com.github.jengelman.gradle.plugins.shadow.transformers.CacheableTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.PatternFilterableResourceTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import io.github.wasabithumb.jakery.descriptor.member.ConstructorDescriptor
import io.github.wasabithumb.jakery.descriptor.member.FieldDescriptor
import io.github.wasabithumb.jakery.descriptor.member.MethodDescriptor
import io.github.wasabithumb.jakery.descriptor.type.TypeDescriptor
import io.github.wasabithumb.jakery.file.JakeFile
import io.github.wasabithumb.jakery.file.JakeFileGroup
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipOutputStream
import org.gradle.api.tasks.util.PatternSet

@CacheableTransformer
class JakeryFileTransformer : PatternFilterableResourceTransformer(
    PatternSet().include(FILE_PATH)
) {

    private val builder: JakeFile.Builder = JakeFile.builder()
    private var any: Boolean = false

    //

    override fun transform(context: TransformerContext) {
        val file = context.inputStream.use { JakeFile.read(it) }
        this.any = true

        for (key in file.keys()) {
            val group = file.group(key) ?: throw ConcurrentModificationException()
            when (group.type()) {
                JakeFileGroup.Type.TYPE -> this.builder.typeGroup(group.name()) {
                    for (descriptor in group.asTypeGroup().elements()) {
                        it.add(context.relocate(descriptor))
                    }
                }
                JakeFileGroup.Type.FIELD -> this.builder.fieldGroup(group.name()) {
                    for (descriptor in group.asFieldGroup().elements()) {
                        it.add(context.relocate(descriptor))
                    }
                }
                JakeFileGroup.Type.METHOD -> this.builder.methodGroup(group.name()) {
                    for (descriptor in group.asMethodGroup().elements()) {
                        it.add(context.relocate(descriptor))
                    }
                }
                JakeFileGroup.Type.CONSTRUCTOR -> this.builder.constructorGroup(group.name()) {
                    for (descriptor in group.asConstructorGroup().elements()) {
                        it.add(context.relocate(descriptor))
                    }
                }
            }
        }
    }

    override fun hasTransformedResource(): Boolean {
        return this.any
    }

    override fun modifyOutputStream(os: ZipOutputStream, preserveFileTimestamps: Boolean) {
        val file = this.builder.build()
        val entry = ZipEntry(FILE_PATH)
        if (!preserveFileTimestamps) entry.time = ShadowCopyAction.CONSTANT_TIME_FOR_ZIP_ENTRIES

        os.putNextEntry(entry)
        file.write(os)
        os.closeEntry()
    }

    //

    @Suppress("ConvertToStringTemplate")
    private fun TransformerContext.relocate(descriptor: TypeDescriptor): TypeDescriptor {
        if (descriptor.isArray) return this.relocate(descriptor.componentType()).arrayType()
        if (!descriptor.isClassName) return descriptor
        var cn = descriptor.asClassName()
        cn = this.relocators.relocatePath(cn)
        return TypeDescriptor.of("L" + cn + ";")
    }

    private fun TransformerContext.relocate(descriptor: FieldDescriptor): FieldDescriptor {
        return FieldDescriptor.of(
            this.relocate(descriptor.declaringClass()),
            descriptor.name()
        )
    }

    private fun TransformerContext.relocate(descriptor: MethodDescriptor): MethodDescriptor {
        return MethodDescriptor.of(
            this.relocate(descriptor.declaringClass()),
            descriptor.name(),
            descriptor.arguments().map { this.relocate(it) }
        )
    }

    private fun TransformerContext.relocate(descriptor: ConstructorDescriptor): ConstructorDescriptor{
        return ConstructorDescriptor.of(
            this.relocate(descriptor.declaringClass()),
            descriptor.arguments().map { this.relocate(it) }
        )
    }

    //

    companion object {

        private const val FILE_PATH = "META-INF/.jakery"

    }

}