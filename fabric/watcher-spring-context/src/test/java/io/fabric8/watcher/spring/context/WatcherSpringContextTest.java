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
package io.fabric8.watcher.spring.context;

import java.io.File;
import java.io.IOException;
import java.util.SortedSet;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class WatcherSpringContextTest  {
    private static final transient Logger LOG = LoggerFactory.getLogger(WatcherSpringContextTest.class);

    protected static WatcherSpringContext watcher = new WatcherSpringContext();
    protected static File dataDir;
    private long timeout = 10000;
    protected static File tmpDir;


    public static File getBaseDir() {
        String basedir = System.getProperty("basedir", ".");
        return new File(basedir);
    }

    @BeforeClass
    public static void init() throws IOException {
        dataDir = new File(getBaseDir(), "src/test/data");
        watcher.setRootDirectory(dataDir);
        watcher.init();
    }

    @AfterClass
    public static void destroy() {
        if (watcher != null) {
            watcher.destroy();
        }
    }

    @Test
    public void testWatcher() throws Exception {
        BeanA.assertCreated(timeout);

        // rescan runs in another thread so give it a bit time to run
        Thread.sleep(2000);

        SortedSet<String> paths = watcher.getApplicationContextPaths();
        LOG.info("Found context paths " + paths);
        assertNotNull("Should have found some context paths", paths);
        assertTrue("Should have found some context paths", paths.size() > 0);
    }
}
