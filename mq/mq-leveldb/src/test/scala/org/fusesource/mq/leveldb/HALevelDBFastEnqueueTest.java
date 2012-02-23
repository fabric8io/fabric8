/*
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
package org.fusesource.mq.leveldb;

import org.apache.hadoop.fs.FileUtil;
import org.fusesource.mq.leveldb.util.FileSupport;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class HALevelDBFastEnqueueTest extends LevelDBFastEnqueueTest {

    @BeforeClass
    static public void startHDFS() throws Exception{
        TestingHDFSServer.start();
    }

    @AfterClass
    static public void stopHDFS() throws Exception {
        TestingHDFSServer.stop();
    }

    protected LevelDBStore createStore() {
        HALevelDBStore store = new HALevelDBStore();
        store.setDirectory(dataDirectory());
        store.setDfsDirectory("localhost");
        return store;
    }

    private File dataDirectory() {
        return new File("target/activemq-data/leveldb");
    }

    /**
     * On restart we will also delete the local file system store, so that we test restoring from
     * HDFS.
     */
    protected void restartBroker(int restartDelay, int checkpoint) throws Exception {
        stopBroker();
        FileUtil.fullyDelete(dataDirectory());
        TimeUnit.MILLISECONDS.sleep(restartDelay);
        startBroker(false, checkpoint);
    }

}
