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
package io.fabric8.watcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.fusesource.common.util.Files;
import org.fusesource.common.util.IOHelpers;
import io.fabric8.watcher.file.FileWatcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Ignore("[FABRIC-904][7.4] Fix watcher-core FileWatcherTest")
public class FileWatcherTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(FileWatcherTest.class);

    protected static FileWatcher watcher = new FileWatcher();
    protected static File dataDir;
    protected static MockWatchListener listener = new MockWatchListener();
    protected static MockProcessor processor = new MockProcessor();
    private List<Expectation> expectations = new ArrayList<Expectation>();
    private long timeout = 10000;
    protected static File tmpDir;


    public static File getBaseDir() {
        String basedir = System.getProperty("basedir", ".");
        return new File(basedir);
    }

    @BeforeClass
    public static void init() throws IOException {
        dataDir = new File(getBaseDir(), "target/test-fileWatcherDir");
        Files.recursiveDelete(dataDir);
        dataDir.mkdirs();

        tmpDir = new File(getBaseDir(), "target/tmp-fileWatcherDir");
        Files.recursiveDelete(tmpDir);
        tmpDir.mkdirs();

        watcher.setRootDirectory(dataDir);
        watcher.setFileMatchPattern("glob:**.txt");
        watcher.addListener(listener);
        watcher.setProcessor(processor);
        watcher.init();
    }

    @AfterClass
    public static void destroy() {
        if (watcher != null) {
            watcher.destroy();
        }
    }

    @Test
    public void testFileWatcher() throws Exception {
        File file1 = assertProcessed("something.txt", "this is\nsome text\n");
        File file2 = assertProcessed("a1/thing2.txt", "1 level deep");
        File file3 = assertProcessed("b1/b2/thing3.txt", "2 levels deep");
        File file4 = assertProcessed("c1/c2/thing3.txt", "2 levels deep");

        assertNotProcessed("something.ignored", "ignored");
        assertNotProcessed("a1/something.ignored", "ignored");
        assertNotProcessed("b1/b2/something.ignored", "ignored");

        assertExpectations();
        expectations.clear();
        listener.clearEvents();

        // now lets delete some files and directories and ensure we get notified
        file1.delete();

        // lets try move the directories to avoid raising events directly for files
        moveFileToTempDir(file2.getParentFile());
        moveFileToTempDir(file3.getParentFile());
        moveFileToTempDir(file4.getParentFile().getParentFile());

        processor.expectRemoved(expectations, file1, file2, file3, file4);
        listener.expectCalledWith(expectations, file1, file2, file3, file4);

        LOG.info("Processed: " + processor.getProcessPaths());
        LOG.info("Removed: " + processor.getOnRemovePaths());

        assertExpectations();
    }

    protected void moveFileToTempDir(File file) {
        File dest = new File(tmpDir, file.getName());
        LOG.info("moving file " + file + " to " + dest);
        file.renameTo(dest);
        if (file.exists()) {
            LOG.warn("File still exists!" + file);
        }
    }

    protected void assertExpectations() throws Exception {
        AsyncTests.assertTrue(timeout, expectations);
    }


    protected File assertProcessed(String fileName, String content) throws Exception {
        File name = newFileName(fileName);
        Path expectedPath = name.toPath();
        IOHelpers.writeTo(name, content);

        listener.expectCalledWith(expectations, expectedPath);
        processor.expectProcessed(expectations, expectedPath);

        return name;
    }

    protected void assertNotProcessed(String fileName, String content) throws Exception {
        File name = newFileName(fileName);
        Path expectedPath = name.toPath();
        IOHelpers.writeTo(name, content);

        listener.expectNotCalledWith(expectations, expectedPath);
    }

    protected File newFileName(String name) {
        File answer = new File(dataDir, name);
        answer.getParentFile().mkdirs();
        return answer;
    }


    protected <T> T doTest(WatcherListener listener, Callable<T> task) throws Exception {
        watcher.addListener(listener);
        try {
            return task.call();
        } finally {
            watcher.removeListener(listener);
        }
    }
}
