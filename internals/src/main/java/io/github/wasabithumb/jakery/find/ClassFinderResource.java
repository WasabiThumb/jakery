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

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@NullMarked
@ApiStatus.Internal
abstract class ClassFinderResource {

    public static ClassFinderResource ambiguous() {
        return Ambiguous.INSTANCE;
    }

    public static ClassFinderResource simple(Path path) {
        return new Simple(path);
    }

    public static ClassFinderResource zip(Path archive, String entry) {
        return new Zip(archive, entry);
    }

    //

    public abstract boolean isAmbiguous();

    public abstract InputStream open() throws IOException;

    //

    private static final class Ambiguous extends ClassFinderResource {

        private static final Ambiguous INSTANCE = new Ambiguous();

        //

        @Override
        public boolean isAmbiguous() {
            return true;
        }

        @Override
        public InputStream open() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            return 560302898;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Ambiguous;
        }

        @Override
        public String toString() {
            return "Ambiguous";
        }

    }

    private static final class Simple extends ClassFinderResource {

        private final Path path;

        Simple(Path path) {
            this.path = path;
        }

        //

        @Override
        public boolean isAmbiguous() {
            return false;
        }

        @Override
        public InputStream open() throws IOException {
            return Files.newInputStream(this.path);
        }

        @Override
        public int hashCode() {
            return this.path.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Simple && this.path.equals(((Simple) obj).path);
        }

        @Override
        public String toString() {
            return "Simple{path=" + this.path + "}";
        }

    }

    private static final class Zip extends ClassFinderResource {

        private final Path archive;
        private final String entry;

        Zip(Path archive, String entry) {
            this.archive = archive;
            this.entry = entry;
        }

        //

        @Override
        public boolean isAmbiguous() {
            return false;
        }

        @Override
        public InputStream open() throws IOException {
            final ZipFile zf = new ZipFile(this.archive.toFile());
            boolean close = true;

            try {
                ZipEntry entry = zf.getEntry(this.entry);
                InputStream in;
                if (entry == null || (in = zf.getInputStream(entry)) == null) {
                    throw new IOException("Entry \"" + this.entry + "\" not found in archive " + this.archive);
                }

                close = false;
                return new FilterInputStream(in) {
                    @Override
                    public void close() throws IOException {
                        try {
                            super.close();
                        } finally {
                            zf.close();
                        }
                    }
                };
            } finally {
                if (close) zf.close();
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.archive, this.entry);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Zip)) return false;
            Zip other = (Zip) obj;
            return this.archive.equals(other.archive) &&
                    this.entry.equals(other.entry);
        }

        @Override
        public String toString() {
            return "Zip{archive=" + this.archive +
                    ", entry=" + this.entry +
                    "}";
        }
    }

}
