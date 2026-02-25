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
package io.github.wasabithumb.jakery.agent;

import io.github.wasabithumb.jakery.file.JakeFile;
import io.github.wasabithumb.jakery.find.ClassFinder;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Executed by Gradle to generate the
 * Jakery index file for a project.
 */
@NullMarked
@ApiStatus.Internal
public final class JakeryAgentLauncher {

    static void main(String[] args) throws IOException {
        Path out = prepareOutputFile(args);
        JakeFile.Builder builder = JakeFile.builder();

        try (Stream<? extends Class<?>> stream = listAgentClasses()) {
            Iterator<? extends Class<?>> iter = stream.iterator();
            Class<?> next;

            while (iter.hasNext()) {
                next = iter.next();
                JakeryAgent agent = initAgent(next.asSubclass(JakeryAgent.class));
                agent.apply(builder);
            }
        }

        JakeFile file = builder.build();
        try (OutputStream os = Files.newOutputStream(
                out,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)
        ) {
            file.write(os);
        }
    }

    private static JakeryAgent initAgent(Class<?> type) {
        Class<? extends JakeryAgent> qual = type.asSubclass(JakeryAgent.class);
        Constructor<? extends JakeryAgent> con;
        try {
            con = qual.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Agent class " + type.getName() + " has no primary constructor");
        }

        try {
            con.setAccessible(true);
        } catch (Exception ignored) { }

        JakeryAgent instance;
        try {
            instance = con.newInstance();
        } catch (Throwable t) {
            Throwable cause = t.getCause();
            if (cause == null) cause = t;
            if (cause instanceof RuntimeException re) throw re;
            throw new IllegalStateException("Unexpected error while initializing agent class " + type.getName(), cause);
        }

        return instance;
    }

    private static Path prepareOutputFile(String[] args) throws IOException {
        String src;
        switch (args.length) {
            case 0:
                System.err.println("No output file specified");
                System.exit(1);
                return null;
            case 1:
                src = args[0];
                break;
            default:
                src = String.join(" ", args);
                break;
        }

        Path path = FileSystems.getDefault().getPath(src);
        Path parent = path.getParent();
        if (parent != null) Files.createDirectories(parent);

        return path;
    }

    private static Stream<? extends Class<?>> listAgentClasses() {
        ClassFinder finder = ClassFinder.forContextClass(JakeryAgentLauncher.class, false);
        return finder.find(null, ClassFinder.FIND_DEEP)
                .map((ClassFinder.Entry entry) -> entry.descriptor().asClassName().replaceAll("/", "."))
                .map((String name) -> {
                    try {
                        return Class.forName(name);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("Discovered class " + name + " could not be found", e);
                    }
                })
                .filter((Class<?> cls) -> {
                    int mod = cls.getModifiers();
                    if (Modifier.isAbstract(mod)) return false;
                    if (Modifier.isInterface(mod)) return false;
                    return JakeryAgent.class.isAssignableFrom(cls);
                });
    }

}
