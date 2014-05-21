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
package io.fabric8.git.http;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.fabric8.zookeeper.ZkPath;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.eclipse.jgit.http.server.GitServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricGitServlet extends GitServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricGitServlet.class);
    private final CuratorFramework curator;
    private final String path = ZkPath.GIT_TRIGGER.getPath();
    private SharedCount counter;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public FabricGitServlet(CuratorFramework curator) {
        this.curator = curator;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            this.counter = new SharedCount(curator, path, 0);
            this.counter.start();
        } catch (Exception e) {
            LOGGER.error("Error starting SharedCount for ZkNode " + path + " due " + e.getMessage(), e);
            throw new ServletException("Error starting SharedCount for ZkNode " + path, e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            this.counter.close();
        } catch (IOException e) {
            LOGGER.warn("Error closing SharedCount due " + e.getMessage() + ". This exception is ignored.", e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String service = req.getParameter("service");
        // now check if it was a push, if so then update ZK
        boolean isPush = service != null && service.equals("git-receive-pack");

        LOGGER.trace("FabricGitServlet service git service={}, isPush={}", service, isPush);

        // get either a read or write lock (push = write lock, pull = read lock)
        // as we do not want concurrent writes to the git repo
        Lock lock = isPush ? rwLock.writeLock() : rwLock.readLock();
        try {
            lock.lock();
            super.service(req, res);
        } finally {
            lock.unlock();

            if (isPush) {
                int value = counter.getCount();
                int newValue = value + 1;
                LOGGER.debug("Updating counter to {}", newValue);
                try {
                    counter.trySetCount(newValue);
                } catch (Exception e) {
                    // we dont want stacktrace in WARN
                    LOGGER.debug("Error updating counter on ZkPath: " + path + " due " + e.getMessage() + ". This exception is ignored.", e);
                    LOGGER.warn("Error updating counter on ZkPath: " + path + " due " + e.getMessage() + ". This exception is ignored.");
                }
            }
        }
    }

}
