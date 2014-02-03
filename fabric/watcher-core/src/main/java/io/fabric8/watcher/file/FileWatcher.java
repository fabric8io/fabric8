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
package io.fabric8.watcher.file;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.FileSystem;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import io.fabric8.watcher.Processor;
import io.fabric8.watcher.matchers.Matchers;
import io.fabric8.watcher.support.WatcherSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * A File watching service
 */
public class FileWatcher extends WatcherSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileWatcher.class);

    private Path root;
    private boolean watch = true;
    private WatchService watcher;
    private PathMatcher dirMatcher;
    private PathMatcher fileMatcher;
    private ExecutorService executor;
    private final AtomicInteger processing = new AtomicInteger();
    private final Map<WatchKey, Path> keys = new ConcurrentHashMap<WatchKey, Path>();
    private volatile long lastModified;
    private final Map<Path, Boolean> processedMap = new ConcurrentHashMap<Path, Boolean>();

    public void init() throws IOException {
        if (root == null) {
            Iterable<Path> rootDirectories = getFileSystem().getRootDirectories();
            for (Path rootDirectory : rootDirectories) {
                if (rootDirectory != null) {
                    root = rootDirectory;
                    break;
                }
            }
        }
        if (!Files.exists(root)) {
            fail(LOGGER, "Root path does not exist: " + root);
        } else if (!Files.isDirectory(root)) {
            fail(LOGGER, "Root path is not a directory: " + root);
        }
        if (executor == null) {
            this.executor = Executors.newFixedThreadPool(4);
        }
        if (watcher == null) {
            watcher = watch ? getFileSystem().newWatchService() : null;
        }

        // use thread pool for work to as we should not
        // lock the thread that initializes this watcher
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    rescan();
                } catch (IOException e) {
                    LOGGER.warn("Caught: " + e, e);
                }
                LOGGER.debug("Completed rescan file watcher");
            }
        });

        if (watch) {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        processEvents();
                    } catch (Throwable e) {
                        LOGGER.warn("Caught: " + e, e);
                    }
                    LOGGER.debug("Completed processing file watcher events");
                }
            });
        }
    }

    public void destroy() {
        executor.shutdownNow();
    }

    public long getLastModified() {
        return lastModified;
    }

    // Properties
    //-------------------------------------------------------------------------

    public void setRootPath(String rootPath) {
        Path path = new File(rootPath).getAbsoluteFile().toPath();
        setRoot(path);
    }

    public void setRootDirectory(File directory) {
        setRoot(directory.toPath());
    }

    /**
     * Sets the directory matching pattern using the
     * <a href="http://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String)">java 7 pattern matching syntax</a>
     * such as <code>glob:/foo/*</code>
     */
    public void setDirMatchPattern(String pattern) {
        setDirMatcher(Matchers.parse(pattern, getFileSystem()));
    }

    /**
     * Sets the file matching pattern using the
     * <a href="http://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String)">java 7 pattern matching syntax</a>
     * such as <code>glob:*.xml</code>
     */
    public void setFileMatchPattern(String pattern) {
        setFileMatcher(Matchers.parse(pattern, getFileSystem()));
    }

    public Path getRoot() {
        return root;
    }

    public void setRoot(Path root) {
        this.root = root;
    }

    public boolean isWatch() {
        return watch;
    }

    public void setWatch(boolean watch) {
        this.watch = watch;
    }

    public WatchService getWatcher() {
        return watcher;
    }

    public void setWatcher(WatchService watcher) {
        this.watcher = watcher;
    }

    public PathMatcher getDirMatcher() {
        return dirMatcher;
    }

    public void setDirMatcher(PathMatcher dirMatcher) {
        this.dirMatcher = dirMatcher;
    }

    public PathMatcher getFileMatcher() {
        return fileMatcher;
    }

    public void setFileMatcher(PathMatcher fileMatcher) {
        this.fileMatcher = fileMatcher;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }


    // Implementation methods
    //-------------------------------------------------------------------------

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
            Files.walkFileTree(root, new FilteringFileVisitor());
            synchronized (processing) {
                while (processing.get() > 0) {
                    processing.wait();
                }
            }
        } catch (InterruptedException e) {
            throw (IOException)new InterruptedIOException().initCause(e);
        }
    }

    private void processEvents() {
        while (true) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException e) {
                return;
            }
            Path dir = keys.get(key);
            if (dir == null) {
                LOGGER.warn("Could not find key for " + key);
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();
                WatchEvent<Path> ev = (WatchEvent<Path>)event;

                // Context for directory entry event is the file name of entry
                Path name = ev.context();
                Path child = dir.resolve(name);

                LOGGER.debug("Processing event {} on path {}", kind, child);

                if (kind == OVERFLOW) {
//                    rescan();
                    continue;
                }

                try {
                    if (kind == ENTRY_CREATE) {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {

                            // if directory is created, and watching recursively, then
                            // register it and its sub-directories
                            Files.walkFileTree(child, new FilteringFileVisitor());
                        } else if (Files.isRegularFile(child, NOFOLLOW_LINKS)) {
                            scan(child);
                        }
                    } else if (kind == ENTRY_MODIFY) {
                        if (Files.isRegularFile(child, NOFOLLOW_LINKS)) {
                            scan(child);
                        }
                    } else if (kind == ENTRY_DELETE) {
                        unscan(child);
                    }
                } catch (IOException x) {
                    // ignore to keep sample readbale
                    x.printStackTrace();
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                LOGGER.debug("Removing key " + key + " and dir " + dir + " from keys");
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    private void scan(final Path file) throws IOException {
        if (isMatchesFile(file)) {
            fireListeners(file, ENTRY_MODIFY);
            process(file);
            processedMap.put(file, Boolean.TRUE);
        }
    }

    protected boolean isMatchesFile(Path file) {
        boolean matches = true;
        if (fileMatcher != null) {
            Path rel = root.relativize(file);
            matches = fileMatcher.matches(rel);
        }
        return matches;
    }

    private void unscan(final Path file) throws IOException {
        if (isMatchesFile(file)) {
            Processor processor = getProcessor();
            if (processor != null) {
                processor.onRemove(file);
            }
            lastModified = System.currentTimeMillis();
        } else {
            // lets find all the files that now no longer exist
            List<Path> files = new ArrayList<Path>(processedMap.keySet());
            for (Path path : files) {
                if (!Files.exists(path)) {
                    LOGGER.debug("File has been deleted: " + path);
                    processedMap.remove(path);
                    if (isMatchesFile(path)) {
                        Processor processor = getProcessor();
                        if (processor != null) {
                            processor.onRemove(path);
                        }
                        fireListeners(path, ENTRY_DELETE);
                        lastModified = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    private void process(final Path path) throws IOException {
        final Processor processor = getProcessor();
        if (processor != null) {
            processing.incrementAndGet();
            try {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            processor.process(path);
                            lastModified = System.currentTimeMillis();
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
    }

    private void watch(final Path path) throws IOException {
        if (watcher != null) {
            WatchKey key = path.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
            keys.put(key, path);
            LOGGER.debug("Watched path " + path + " key " + key);
        } else {
            LOGGER.warn("No watcher yet for path " + path);
        }
    }

    protected FileSystem getFileSystem() {
        return FileSystems.getDefault();
    }

    public class FilteringFileVisitor implements FileVisitor<Path> {

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
            scan(file);
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


    /**
     * Throws an invalid argument exception after logging a warning
     * just in case the stack trace gets gobbled up by application containers
     * like spring or blueprint, at least the error message will be clearly shown in the log
     *
     */
    public static void fail(Logger logger, String message) {
        logger.warn(message);
        throw new IllegalArgumentException(message);
    }
}