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
package io.github.wasabithumb.jakery.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.language.jvm.tasks.ProcessResources

class JakeryPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val ext = target.extensions.create("jakery", JakeryExtension::class)

        // Create the "jakery" source set
        val java = target.extensions.getByType(JavaPluginExtension::class)
        val jakeryAgentSourceSet = java.sourceSets.register("jakery").get()

        // Create the "generateJakeryIndex" task to write the .jakery file
        val generateJakeryIndexTask = target.tasks.register("generateJakeryIndex", JavaExec::class) {
            dependsOn(jakeryAgentSourceSet.compileJavaTaskName)

            val outFile = temporaryDir.toPath().resolve(".jakery")
            outputs.file(outFile)

            args(outFile.toAbsolutePath().toString())
            classpath(jakeryAgentSourceSet.runtimeClasspath)
            mainClass.set("io.github.wasabithumb.jakery.agent.JakeryAgentLauncher")
        }

        // Modify the "processResources" task to inject the .jakery file
        target.tasks.named("processResources", ProcessResources::class.java) {
            dependsOn(generateJakeryIndexTask)
            val index = generateJakeryIndexTask.map { it.outputs.files.singleFile }
            into("META-INF") {
                from(index)
            }
        }

        // Setup "jakery" source set to inherit classes of "main"
        target.configurations.getByName(jakeryAgentSourceSet.implementationConfigurationName) {
            // We can't simply extendsFrom "runtimeClasspath" since that would include resources
            extendsFrom(target.configurations.getByName("implementation"))
            dependencies.addLater(target.tasks.named("compileJava").map { target.dependencyFactory.create(it.outputs.files) })
        }

        // Add the jakery-runtime and jakery-agent dependencies
        target.afterEvaluate {
            val version = ext.libraryVersion.get()

            target.configurations.getByName("implementation") {
                if (ext.autoRuntimeDependency.get()) {
                    dependencies.add(target.dependencyFactory.create(
                        "io.github.wasabithumb",
                        "jakery-runtime",
                        version
                    ))
                }
            }
            target.configurations.getByName(jakeryAgentSourceSet.implementationConfigurationName) {
                dependencies.add(target.dependencyFactory.create(
                    "io.github.wasabithumb",
                    "jakery-agent",
                    version
                ))
            }
        }
    }

}