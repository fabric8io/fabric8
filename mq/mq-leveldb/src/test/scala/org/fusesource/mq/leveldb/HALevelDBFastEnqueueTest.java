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

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;

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
        store.setDirectory(new File("target/activemq-data/leveldb"));
        store.setDfsDirectory("localhost");
        return store;
    }

}
