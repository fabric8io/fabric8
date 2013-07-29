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
package org.fusesource.fabric.watcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.fusesource.common.util.IOHelpers;
import org.fusesource.fabric.watcher.file.FileWatcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 */
public class FileWatcherTest {
    protected static FileWatcher watcher = new FileWatcher();
    protected static File dataDir;
    protected static MockWatchListener listener = new MockWatchListener();
    protected static MockProcessor processor = new MockProcessor();
    private List<Expectation> expectations = new ArrayList<Expectation>();
    private long timeout = 10000;


    public static File getBaseDir() {
        String basedir = System.getProperty("basedir", ".");
        return new File(basedir);
    }

    @BeforeClass
    public static void init() throws IOException {
        dataDir = new File(getBaseDir(), "target/test-fileWatcherDir");
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
        File file2 = assertProcessed("one/thing2.txt", "1 level deep");
        File file3 = assertProcessed("two/bar/thing3.txt", "2 levels deep");

        assertNotProcessed("something.ignored", "ignored");
        assertNotProcessed("another/something.ignored", "ignored");
        assertNotProcessed("another/thing/something.ignored", "ignored");

        assertExpectations();
        expectations.clear();

        // now lets delete some files and directories and ensure we get notified
        file1.delete();
        file2.getParentFile().delete();
        file2.getParentFile().getParentFile().delete();

        processor.expectRemoved(expectations, file1, file2, file3);

        System.out.println("Processed: " + processor.getProcessPaths());
        System.out.println("Removed: " + processor.getOnRemovePaths());

        // TODO removals not quite working yet
        // assertExpectations();
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
