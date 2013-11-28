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
package org.fusesource.fabric.itests.paxexam.support;

import java.util.concurrent.Callable;

public class WaitForConditionTask implements Callable<Boolean> {

    private final Long timeOut;
    private final Callable<Boolean> condition;

    public WaitForConditionTask(Long timeOut, Callable<Boolean> condition) {
        this.timeOut = timeOut;
        this.condition = condition;
    }


    @Override
    public Boolean call() throws Exception {
        boolean done = condition.call();
        Exception lastError = null;
        for (long t = 0; (!done && t < timeOut); t += 2000L) {
            lastError = null;
            try {
                done = condition.call();
            } catch (Exception e) {
                lastError = e;
            }
            if (!done) {
                Thread.sleep(2000L);
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        return done;
    }
}
