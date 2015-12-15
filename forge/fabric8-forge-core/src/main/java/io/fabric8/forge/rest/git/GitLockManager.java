/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.forge.rest.git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages locks on git folders
 */
@Singleton
public class GitLockManager {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitLockManager.class);

    private Map<String, ReentrantLock> locks = new HashMap<>();

    public <T> T withLock(File gitFolder, Callable<T> block) throws Exception {
        ReentrantLock lock = getLock(gitFolder);
        lock.lock();
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Started lock for " + gitFolder + " instance " + lock);
            }
            return block.call();
        } finally {
            lock.unlock();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Ended lock for " + gitFolder + " instance " + lock);
            }
        }
    }

    private ReentrantLock getLock(File gitFolder) throws IOException {
        String key = gitFolder.getCanonicalPath();
        synchronized (locks) {
            ReentrantLock answer = locks.get(key);
            if (answer == null) {
                answer = new ReentrantLock();
                locks.put(key, answer);
            }
            return answer;
        }
    }
}
