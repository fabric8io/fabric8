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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderMismatchException;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.utils.properties.Properties;

public class FabricProfileFileSystemProvider extends FileSystemProvider {

    final Map<Path, FabricProfileFileSystem> fileSystems = new HashMap<>();

    @Override
    public String getScheme() {
        return "fabric-profile";
    }

    @Override
    public FabricProfileFileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        Path path = uriToPath(uri);
        return newFileSystem(path, env);
    }

    public FabricProfileFileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
        if (!Files.isDirectory(path)) {
            throw new UnsupportedOperationException();
        }
        synchronized (fileSystems) {
            Path localPath = path.toRealPath();
            if (this.fileSystems.containsKey(localPath)) {
                throw new FileSystemAlreadyExistsException();
            }
            FabricProfileFileSystem fileSystem = buildFileSystem(path);
            fileSystems.put(localPath, fileSystem);
            return fileSystem;
        }
    }

    private FabricProfileFileSystem buildFileSystem(final Path path) throws IOException {
        final Map<String, Object> contents = new HashMap<>();
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                addChild(dir, new ArrayList<String>());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                byte[] content = Files.readAllBytes(file);
                if (file.getFileName().toString().equals("io.fabric8.agent.properties")) {
                    file = file.resolveSibling("profile.cfg");
                } else if (file.getFileName().toString().contains(".properties")) {
                    file = file.resolveSibling(file.getFileName().toString().replace(".properties", ".cfg"));
                }
                if (file.getFileName().toString().contains(".cfg")) {
                    Properties props = new Properties(false);
                    props.load(new ByteArrayInputStream(content));
                    for (Map.Entry<String, String> entry : props.entrySet()) {
                        String val = entry.getValue();
                        val = val.replace("${profile:io.fabric8.agent/", "${profile:profile/");
                        val = val.replace("${version:", "${profile:io.fabric8.version/");
                        val = val.replace("${runtime.", "${karaf.");
                        Matcher matcher = Pattern.compile(".*\\$\\{(.*?):.*?\\}.*").matcher(val);
                        if (matcher.matches()) {
                            String scheme = matcher.group(1);
                            if (!"profile".equals(scheme)) {
                                System.out.println("Unsupported scheme: " + entry.getKey() + " = " + val + " in " + path.relativize(file));
                            }
                        }
                        entry.setValue(val);
                    }
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    props.save(baos);
                    baos.flush();
                    content = baos.toByteArray();
                }
                addChild(file, content);
                return FileVisitResult.CONTINUE;
            }

            private void addChild(Path child, Object content) {
                String par = "/" + path.relativize(child.getParent()).toString();
                String str = "/" + path.relativize(child).toString();
                if (!"/".equals(str)) {
                    if (str.endsWith("/")) {
                        str = str.substring(0, str.length() - 1);
                    }
                    ((List<String>) contents.get(par)).add(str);
                }
                contents.put(str, content);
            }

        });
        return new FabricProfileFileSystem(this, contents);
    }

    @Override
    public FabricProfileFileSystem getFileSystem(URI uri) {
        synchronized (fileSystems) {
            FabricProfileFileSystem fileSystem = null;
            try {
                fileSystem = fileSystems.get(uriToPath(uri).toRealPath());
            } catch (IOException ignore) {
            }
            if (fileSystem == null) {
                throw new FileSystemNotFoundException();
            }
            return fileSystem;
        }
    }

    protected Path uriToPath(URI uri) {
        String scheme = uri.getScheme();
        if ((scheme == null) || (!scheme.equalsIgnoreCase(getScheme()))) {
            throw new IllegalArgumentException("URI scheme is not '" + getScheme() + "'");
        }
        try {
            String root = uri.getRawSchemeSpecificPart();
            int i = root.lastIndexOf("!/");
            if (i != -1) {
                root = root.substring(0, i);
            }
            URI rootUri = new URI(root);
            try {
                return Paths.get(rootUri).toAbsolutePath();
            } catch (FileSystemNotFoundException e) {
                try {
                    FileSystem fs = FileSystems.newFileSystem(rootUri, new HashMap<String, Object>(), FabricProfileFileSystemProvider.class.getClassLoader());
                    return fs.provider().getPath(rootUri).toAbsolutePath();
                } catch (IOException e2) {
                    e.addSuppressed(e2);
                    throw e;
                }
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public Path getPath(URI uri) {
        String str = uri.getSchemeSpecificPart();
        int i = str.lastIndexOf("!/");
        if (i == -1) {
            throw new IllegalArgumentException("URI: " + uri + " does not contain path info ex. github:apache/karaf#master!/");
        }
        return getFileSystem(uri).getPath(str.substring(i + 1));
    }

    @Override
    public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        if (!(path instanceof FabricProfilePath)) {
            throw new ProviderMismatchException();
        }
        return ((FabricProfilePath) path).getFileSystem().newInputStream(path, options);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        if (!(dir instanceof FabricProfilePath)) {
            throw new ProviderMismatchException();
        }
        return ((FabricProfilePath) dir).getFileSystem().newDirectoryStream(dir, filter);
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        if (!(path instanceof FabricProfilePath)) {
            throw new ProviderMismatchException();
        }
        return ((FabricProfilePath) path).getFileSystem().newByteChannel(path, options, attrs);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void delete(Path path) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return path.toAbsolutePath().equals(path2.toAbsolutePath());
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return null;
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {

    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        if (!(path instanceof FabricProfilePath)) {
            throw new ProviderMismatchException();
        }
        return ((FabricProfilePath) path).getFileSystem().readAttributes(path, type, options);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

}
