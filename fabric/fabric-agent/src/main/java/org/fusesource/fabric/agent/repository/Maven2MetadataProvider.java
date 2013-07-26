/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.agent.repository;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Manifest;

import org.fusesource.common.util.Manifests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

/**
 */
public class Maven2MetadataProvider implements MetadataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(Maven2MetadataProvider.class);

    private final Path root;
    private final PathMatcher dirMatcher;
    private final PathMatcher fileMatcher;
    private final ExecutorService executor;
    private final AtomicInteger processing;
    private final WatchService watcher;
    private final Map<WatchKey,Path> keys;
    private final Map<String, Map<String, String>> metadatas;

    public Maven2MetadataProvider(Path root, Collection<String> groupIds) throws IOException {
        this(root,
             new GroupIdPathMatcher(groupIds),
             FileSystems.getDefault().getPathMatcher("glob:**/*.jar"),
             false);
    }

    public Maven2MetadataProvider(Path root, PathMatcher dirMatcher, PathMatcher fileMatcher, boolean watch) throws IOException {
        this.root = root;
        this.dirMatcher = dirMatcher;
        this.fileMatcher = fileMatcher;
        this.watcher = watch ? FileSystems.getDefault().newWatchService() : null;
        this.executor = Executors.newFixedThreadPool(4);
        this.processing = new AtomicInteger();
        this.keys = new ConcurrentHashMap<WatchKey, Path>();
        this.metadatas = new ConcurrentHashMap<String, Map<String, String>>();
        if (watch) {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processEvents();
                }
            });
        }
        rescan();
    }

    private void rescan() throws IOException {
        try {
            synchronized (processing) {
                while (processing.get() > 0) {
                    processing.wait();
                }
            }
            for (WatchKey key : keys.keySet()) {
                key.cancel();
            }
            keys.clear();
            metadatas.clear();
            Files.walkFileTree(root, new MavenVisitor());
            synchronized (processing) {
                while (processing.get() > 0) {
                    processing.wait();
                }
            }
        } catch (InterruptedException e) {
            throw (IOException) new InterruptedIOException().initCause(e);
        }
    }

    public void destroy() {
        executor.shutdownNow();
    }

    @Override
    public Map<String, Map<String, String>> getMetadatas() {
        return metadatas;
    }

    private void processEvents() {
        for (;;) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException e) {
                return;
            }
            Path dir = keys.get(key);
            if (dir == null) {
                continue;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();
                if (kind == OVERFLOW) {
//                    rescan();
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path name = ev.context();
                Path child = dir.resolve(name);

                // if directory is created, and watching recursively, then
                // register it and its sub-directories
                if (kind == ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            Files.walkFileTree(child, new MavenVisitor());
                        }
                    } catch (IOException x) {
                        // ignore to keep sample readbale
                    }
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    private void scan(final Path path) throws IOException {
        processing.incrementAndGet();
        try {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        doScan(path);
                    } finally {
                        processing.decrementAndGet();
                        synchronized (processing) {
                            processing.notifyAll();
                        }
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            processing.decrementAndGet();
            synchronized (processing) {
                processing.notifyAll();
            }
            throw e;
        }
    }

    private void doScan(Path path) {
        try {
            Manifest man = Manifests.getManifest(path.toFile());
            Map<String, String> headers = new HashMap<String, String>();
            for (Map.Entry attr : man.getMainAttributes().entrySet()) {
                headers.put(attr.getKey().toString(), attr.getValue().toString());
            }
            String uri = getResourceUri(path);
            metadatas.put(uri, headers);
        } catch (Exception e) {
            LOGGER.info("Unable to scan resource " + path, e);
        }
    }

    private String getResourceUri(Path path) {
        return convertToMavenUrl(root.relativize(path).toString());
    }

    private void watch(final Path path) throws IOException {
        if (watcher != null) {
            WatchKey key = path.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
            keys.put(key, path);
        }
    }

    private static String convertToMavenUrl(String location) {
        String[] p = location.split("/");
        if (p.length >= 4 && p[p.length-1].startsWith(p[p.length-3] + "-" + p[p.length-2])) {
            String artifactId = p[p.length-3];
            String version = p[p.length-2];
            String classifier;
            String type;
            String artifactIdVersion = artifactId + "-" + version;
            StringBuilder sb = new StringBuilder();
            if (p[p.length-1].charAt(artifactIdVersion.length()) == '-') {
                classifier = p[p.length-1].substring(artifactIdVersion.length() + 1, p[p.length-1].lastIndexOf('.'));
            } else {
                classifier = null;
            }
            type = p[p.length-1].substring(p[p.length-1].lastIndexOf('.') + 1);
            sb.append("mvn:");
            for (int j = 0; j < p.length - 3; j++) {
                if (j > 0) {
                    sb.append('.');
                }
                sb.append(p[j]);
            }
            sb.append('/').append(artifactId).append('/').append(version);
            if (!"jar".equals(type) || classifier != null) {
                sb.append('/');
                if (!"jar".equals(type)) {
                    sb.append(type);
                }
                if (classifier != null) {
                    sb.append('/').append(classifier);
                }
            }
            return sb.toString();
        } else {
            return location;
        }
    }


    public class MavenVisitor implements FileVisitor<Path> {

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (Thread.interrupted()) {
                throw new InterruptedIOException();
            }
            if (dirMatcher != null) {
                Path rel = root.relativize(dir);
                if (!"".equals(rel.toString()) && !dirMatcher.matches(rel)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }
            watch(dir);
            return FileVisitResult.CONTINUE;
        }
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (Thread.interrupted()) {
                throw new InterruptedIOException();
            }
            if (fileMatcher != null) {
                Path rel = root.relativize(file);
                if (fileMatcher.matches(rel)) {
                    scan(file);
                }
            }
            return FileVisitResult.CONTINUE;
        }
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

    }

    public static class GroupIdPathMatcher implements PathMatcher {

        Node root = new Node();

        public GroupIdPathMatcher(Collection<String> groupIds) {
            for (String groupId : groupIds) {
                addGroupId(groupId);
            }
        }

        private void addGroupId(String groupId) {
            Node node = root;
            for (String path : groupId.split("\\.")) {
                Node child = node.children.get(path);
                if (child == null) {
                    child = new Node();
                    node.children.put(path, child);
                }
                node = child;
            }
            node.valid = true;
        }

        @Override
        public boolean matches(Path path) {
            Node node = root;
            for (Path p : path) {
                node = node.children.get(p.toString());
                if (node == null) {
                    return false;
                } else if (node.valid) {
                    return true;
                }
            }
            return true;
        }

        static class Node {
            boolean valid;
            Map<String, Node> children = new HashMap<String, Node>();
        }
    }

}
