/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.arquillian.kubernetes;

import io.fabric8.arquillian.kubernetes.log.Logger;
import io.fabric8.utils.Systems;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a testing session.
 * It is used for scoping pods, service and replication controllers created during the test.
 */
public class Session {
    private final String id;
    private final Logger logger;
    private final String namespace;
    private final AtomicInteger passed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger skiped = new AtomicInteger();
    private File basedir;

    public Session(String id, String namespace, Logger logger) {
        this.id = id;
        this.logger = logger;
        this.namespace = namespace;
    }
    void init() {
        logger.status("Initializing Session:" + id);
    }

    void destroy() {
        logger.status("Destroying Session:" + id);
        System.out.flush();
    }

    public String getId() {
        return id;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns the namespace ID for this test case session
     */
    public String getNamespace() {
        return namespace;
    }

    public AtomicInteger getPassed() {
        return passed;
    }

    public AtomicInteger getFailed() {
        return failed;
    }

    public AtomicInteger getSkiped() {
        return skiped;
    }


    public File getBaseDir() {
        if (basedir == null) {
            basedir = new File(System.getProperty("basedir", "."));
        }
        return basedir;
    }
}
