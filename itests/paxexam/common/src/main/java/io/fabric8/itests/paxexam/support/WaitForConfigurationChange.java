/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.itests.paxexam.support;

import io.fabric8.api.FabricService;

import java.util.concurrent.CountDownLatch;

public class WaitForConfigurationChange implements Runnable {

    private final FabricService fabricService;
    private final CountDownLatch latch;

    private WaitForConfigurationChange(FabricService fabricService, CountDownLatch latch) {
        this.fabricService = fabricService;
        this.latch = latch;
    }

    public static CountDownLatch on(FabricService fabricService) {
        CountDownLatch latch = new CountDownLatch(1);
        WaitForConfigurationChange task = new WaitForConfigurationChange(fabricService, latch);
        fabricService.trackConfiguration(task);
        return latch;
    }

    @Override
    public void run()  {
       latch.countDown();
    }
}
