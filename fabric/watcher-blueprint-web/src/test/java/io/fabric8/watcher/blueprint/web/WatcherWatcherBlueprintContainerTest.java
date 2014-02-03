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
package io.fabric8.watcher.blueprint.web;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class WatcherWatcherBlueprintContainerTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(WatcherWatcherBlueprintContainerTest.class);

    protected static WatcherBlueprintContainer watcher = new WatcherBlueprintContainer();
    protected static File dataDir;
    protected static String expectedName = "James";
    protected long timeout = 10000;


    public static File getBaseDir() {
        String basedir = System.getProperty("basedir", ".");
        return new File(basedir);
    }

    @BeforeClass
    public static void init() throws IOException {
        dataDir = new File(getBaseDir(), "src/test/data");
        watcher.setRootDirectory(dataDir);
        watcher.getProperties().put("my.name", expectedName);
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
        BeanA testBean = BeanA.assertCreated(timeout);
        assertEquals("test bean name", expectedName, testBean.getName());

        // rescan runs in another thread so give it a bit time to run
        Thread.sleep(2000);

        Set<URL> urls = watcher.getContainerURLs();
        LOG.info("Found context urls " + urls);
        assertNotNull("Should have found some context urls", urls);
        assertTrue("Should have found some context urls", urls.size() > 0);
    }
}
