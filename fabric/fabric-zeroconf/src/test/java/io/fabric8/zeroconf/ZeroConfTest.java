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
package io.fabric8.zeroconf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class ZeroConfTest {
    protected ZeroConfBridge activator = new ZeroConfBridge();

    @Before
    public void start() throws Exception {
        activator.start();
    }

    @After
    public void stop() throws Exception {
        activator.stop();
    }

    @Test
    public void viewZeroConf() throws Exception {
        System.out.println("Waiting for ZK");
        Thread.sleep(15 * 1000);
        System.out.println("Done!");

    }
}
