/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.tooling.migration.profile;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class FabricProfileFileSystem extends FileSystem {

    private final FabricProfileFileSystemProvider fileSystemProvider;

    private final Map<String, Object> contents;

    public FabricProfileFileSystem(FabricProfileFileSystemProvider fileSystemProvider, Map<String, Object> contents) {
        this.fileSystemProvider = fileSystemProvider;
        this.contents = contents;
    }

    @Override
    public FileSystemProvider provider() {
        return fileSystemProvider;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public String getSeparator() {
        return "/";
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.<Path>singleton(new FabricProfilePath(this, new byte[]{'/'}));
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return null;
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return null;
    }

    @Override
    public Path getPath(String first, String... more) {
        String path;
        if (more.length == 0) {
            path = first;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(first);
            for (String segment : more) {
                if (segment.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append('/');
                    }
                    sb.append(segment);
                }
            }
            path = sb.toString();
        }
        return new FabricProfilePath(this, path.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        int colonIndex = syntaxAndPattern.indexOf(':');
        if (colonIndex <= 0 || colonIndex == syntaxAndPattern.length() - 1) {
            throw new IllegalArgumentException("syntaxAndPattern must have form \"syntax:pattern\" but was \"" + syntaxAndPattern + "\"");
        }

        String syntax = syntaxAndPattern.substring(0, colonIndex);
        String pattern = syntaxAndPattern.substring(colonIndex + 1);
        String expr;
        switch (syntax) {
        case "glob":
            expr = globToRegex(pattern);
            break;
        case "regex":
            expr = pattern;
            break;
        default:
            throw new UnsupportedOperationException("Unsupported syntax \'" + syntax + "\'");
        }
        final Pattern regex = Pattern.compile(expr);
        return new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                return regex.matcher(path.toString()).matches();
            }
        };
    }

    private String globToRegex(String pattern) {
        StringBuilder sb = new StringBuilder(pattern.length());
        int inGroup = 0;
        int inClass = 0;
        int firstIndexInClass = -1;
        char[] arr = pattern.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            switch (ch) {
            case '\\':
                if (++i >= arr.length) {
                    sb.append('\\');
                } else {
                    char next = arr[i];
                    switch (next) {
                    case ',':
                        // escape not needed
                        break;
                    case 'Q':
                    case 'E':
                        // extra escape needed
                        sb.append('\\');
                    default:
                        sb.append('\\');
                    }
                    sb.append(next);
                }
                break;
            case '*':
                if (inClass == 0)
                    sb.append(".*");
                else
                    sb.append('*');
                break;
            case '?':
                if (inClass == 0)
                    sb.append('.');
                else
                    sb.append('?');
                break;
            case '[':
                inClass++;
                firstIndexInClass = i + 1;
                sb.append('[');
                break;
            case ']':
                inClass--;
                sb.append(']');
                break;
            case '.':
            case '(':
            case ')':
            case '+':
            case '|':
            case '^':
            case '$':
            case '@':
            case '%':
                if (inClass == 0 || (firstIndexInClass == i && ch == '^'))
                    sb.append('\\');
                sb.append(ch);
                break;
            case '!':
                if (firstIndexInClass == i)
                    sb.append('^');
                else
                    sb.append('!');
                break;
            case '{':
                inGroup++;
                sb.append('(');
                break;
            case '}':
                inGroup--;
                sb.append(')');
                break;
            case ',':
                if (inGroup > 0)
                    sb.append('|');
                else
                    sb.append(',');
                break;
            default:
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }

    public InputStream newInputStream(Path path, OpenOption[] options) throws IOException {
        byte[] data = getFile(path);
        return new ByteArrayInputStream(data);
    }

    @SuppressWarnings("unchecked")
    private List<String> getDirectory(Path dir) throws IOException {
        final Object content = loadContent(dir.toAbsolutePath().toString());
        if (!(content instanceof List)) {
            throw new IOException("Is not a directory");
        }
        return (List<String>) content;
    }

    @SuppressWarnings("unchecked")
    private byte[] getFile(Path dir) throws IOException {
        final Object content = loadContent(dir.toAbsolutePath().toString());
        if (!(content instanceof byte[])) {
            throw new IOException("Is not a file");
        }
        return (byte[]) content;
    }

    public DirectoryStream<Path> newDirectoryStream(final Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        final List<String> directory = getDirectory(dir);
        return new DirectoryStream<Path>() {
            @Override
            public Iterator<Path> iterator() {
                return new Iterator<Path>() {
                    final Iterator<String> delegate = directory.iterator();

                    @Override
                    public boolean hasNext() {
                        return delegate.hasNext();
                    }

                    @Override
                    public Path next() {
                        String val = delegate.next();
                        return new FabricProfilePath(FabricProfileFileSystem.this, val.getBytes(StandardCharsets.UTF_8));
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    public <A extends BasicFileAttributes> SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>[] attrs) throws IOException {
        final byte[] data = getFile(path);
        return new SeekableByteChannel() {
            long position;

            @Override
            public int read(ByteBuffer dst) throws IOException {
                int l = (int) Math.min(dst.remaining(), size() - position);
                dst.put(data, (int) position, l);
                position += l;
                return l;
            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public long position() throws IOException {
                return position;
            }

            @Override
            public SeekableByteChannel position(long newPosition) throws IOException {
                position = newPosition;
                return this;
            }

            @Override
            public long size() throws IOException {
                return data.length;
            }

            @Override
            public SeekableByteChannel truncate(long size) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> clazz, LinkOption... options) throws IOException {
        if (clazz != BasicFileAttributes.class) {
            throw new UnsupportedOperationException();
        }
        Path absolute = path.toAbsolutePath();
        Object desc = contents.get(absolute.toString());
        if (desc == null) {
            throw new FileNotFoundException(path.toString());
        }
        String type;
        long size;
        if (desc instanceof List) {
            type = "directory";
            size = 0;
        } else if (desc instanceof byte[]) {
            type = "file";
            size = ((byte[]) desc).length;
        } else {
            throw new IllegalStateException();
        }
        return (A) new FabricGitProfileFileAttributes(type, size);
    }

    private Object loadContent(String path) throws IOException {
        return contents.get(path);
    }

    private static class FabricGitProfileFileAttributes implements BasicFileAttributes {

        private final String type;
        private final long size;

        private FabricGitProfileFileAttributes(String type, long size) {
            this.type = type;
            this.size = size;
        }

        @Override
        public FileTime lastModifiedTime() {
            return null;
        }

        @Override
        public FileTime lastAccessTime() {
            return null;
        }

        @Override
        public FileTime creationTime() {
            return null;
        }

        @Override
        public boolean isRegularFile() {
            return "file".equals(type);
        }

        @Override
        public boolean isDirectory() {
            return "directory".equals(type);
        }

        @Override
        public boolean isSymbolicLink() {
            return false;
        }

        @Override
        public boolean isOther() {
            return false;
        }

        @Override
        public long size() {
            return size;
        }

        @Override
        public Object fileKey() {
            return null;
        }
    }
}
