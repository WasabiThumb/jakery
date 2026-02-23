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
package io.github.wasabithumb.jakery.find;

import io.github.wasabithumb.jakery.find.except.ClassFinderIOException;
import io.github.wasabithumb.jakery.util.ms.MultiString;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@NullMarked
@ApiStatus.Internal
final class ClassFinderFactory {

    static ClassFinder forContextClass(Class<?> cls, boolean searchJrt) {
        ClassFinderImpl ret = new ClassFinderImpl();
        extractClassCodeSource(ret, cls);
        extractSystemClassPath(ret);
        if (searchJrt) extractJrt(ret);
        return ret;
    }

    static ClassFinder forClassLoader(ClassLoader classLoader, boolean searchJrt) {
        ClassFinderImpl ret = new ClassFinderImpl();
        extractClassLoader(ret, classLoader);
        extractSystemClassPath(ret);
        if (searchJrt) extractJrt(ret);
        return ret;
    }

    private static void extractClassLoader(ClassFinderImpl ret, ClassLoader loader) {
        if (!(loader instanceof URLClassLoader)) return;
        URL[] urls = ((URLClassLoader) loader).getURLs();
        for (URL url : urls) {
            File file;
            try {
                file = new File(url.toURI());
            } catch (URISyntaxException | IllegalArgumentException ignored) {
                continue;
            }
            try {
                Path path = file.toPath();
                if (Files.isDirectory(path)) {
                    registerSimpleTree(ret, path);
                } else {
                    registerArchiveTree(ret, path);
                }
            } catch (IOException e) {
                throw new ClassFinderIOException("Failed to inspect ClassLoader URL " + url, e);
            }
        }
    }

    private static void extractClassCodeSource(ClassFinderImpl ret, Class<?> cls) {
        Path cs;
        try {
            cs = codeSource(cls);
            if (cs != null && Files.exists(cs)) {
                if (Files.isDirectory(cs)) {
                    registerSimpleTree(ret, cs);
                } else {
                    registerArchiveTree(ret, cs);
                }
            }
        } catch (IOException e) {
            throw new ClassFinderIOException("Failed to inspect context code source", e);
        }
    }

    private static void extractSystemClassPath(ClassFinderImpl ret) {
        String cp = System.getProperty("java.class.path");
        for (String part : MultiString.parse(cp, File.pathSeparatorChar)) {
            Path path;
            try {
                path = FileSystems.getDefault().getPath(part);
            } catch (InvalidPathException ignored) {
                continue;
            }

            try {
                if (!Files.exists(path)) continue;
                if (Files.isDirectory(path)) {
                    registerSimpleTree(ret, path);
                } else {
                    registerArchiveTree(ret, path);
                }
            } catch (IOException e) {
                throw new ClassFinderIOException("Failed to inspect classpath entry " + part, e);
            }
        }
    }

    private static void extractJrt(ClassFinderImpl ret) {
        try {
            Path root = jrt();

            Path modules = root.resolve("modules");
            try (Stream<Path> stream = Files.list(modules)) {
                Iterator<Path> iter = stream.iterator();
                while (iter.hasNext()) registerSimpleTree(ret, iter.next());
            }

            Path packages = root.resolve("packages");
            try (Stream<Path> stream = nestedList(packages)) {
                Iterator<Path> iter = stream.iterator();
                while (iter.hasNext()) registerSimpleTree(ret, iter.next());
            }
        } catch (IOException e) {
            throw new ClassFinderIOException("Failed to inspect JRT filesystem", e);
        }
    }

    private static void registerSimpleTree(ClassFinderImpl dest, Path src) throws IOException {
        registerSimpleTree(MultiString.of(), dest, src);
    }

    private static void registerSimpleTree(MultiString prefix, ClassFinderImpl dest, Path src) throws IOException {
        try (Stream<Path> stream = Files.list(src)) {
            Iterator<Path> iter = stream.iterator();
            Path next;

            while (iter.hasNext()) {
                next = iter.next();

                Path fileName = next.getFileName();
                if (fileName == null) continue;

                MultiString path = MultiString.of(fileName.toString());
                if (!prefix.isEmpty()) path = MultiString.join(prefix, path);

                if (Files.isDirectory(next)) {
                    registerSimpleTree(path, dest, next);
                } else if (Files.isRegularFile(next)) {
                    dest.registerResource(path, ClassFinderResource.simple(next));
                }
            }
        }
    }

    private static void registerArchiveTree(ClassFinderImpl dest, Path archive) throws IOException {
        try (InputStream in = Files.newInputStream(archive);
             ZipInputStream zin = new ZipInputStream(in)
        ) {
            registerArchiveTree(zin, dest, archive);
        }
    }

    private static void registerArchiveTree(
            ZipInputStream in,
            ClassFinderImpl dest,
            Path archive
    ) throws IOException {
        ZipEntry next;
        while ((next = in.getNextEntry()) != null) {
            String name = next.getName();
            if (name.isEmpty()|| name.charAt(name.length() - 1) == '/') continue;
            MultiString path = MultiString.parse(name, '/');
            dest.registerResource(
                    path,
                    ClassFinderResource.zip(archive, name)
            );
        }
    }

    private static Stream<Path> nestedList(Path in) throws IOException {
        Stream<Path> ret = Files.list(in);
        ret = ret.flatMap((Path p) -> {
            try {
                if (!Files.isDirectory(p)) return Stream.empty();
                return Files.list(p);
            } catch (IOException e) {
                return Stream.empty();
            }
        });
        return ret;
    }

    private static Path jrt() throws IOException {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        return fs.getPath("");
    }

    private static @Nullable Path codeSource(Class<?> cls) throws IOException {
        URI uri;
        try {
            uri = cls.getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException ignored) {
            return null;
        }
        if (!"file".equals(uri.getScheme())) return null;
        return (new File(uri)).toPath();
    }

    //

    private ClassFinderFactory() { }

}
