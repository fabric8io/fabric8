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
package io.fabric8.internal.locks;

import java.util.concurrent.atomic.AtomicInteger;

public class LockData {

    private final Thread thread;
    private final String lockPath;
    private final AtomicInteger count = new AtomicInteger(1);

    public LockData(Thread thread, String lockPath) {
        this.thread = thread;
        this.lockPath = lockPath;
    }

    public String getLockPath() {
        return lockPath;
    }

    public AtomicInteger getCount() {
        return count;
    }
}
