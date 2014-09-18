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

import io.fabric8.zookeeper.ZkPath;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.lib.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class FabricGitServlet extends GitServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricGitServlet.class);
    
    private final CuratorFramework curator;
    private SharedCount counter;
    private Git git;

    FabricGitServlet(Git git, CuratorFramework curator) {
        this.curator = curator;
        this.git = git;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            counter = new SharedCount(curator, ZkPath.GIT_TRIGGER.getPath(), 0);
            counter.start();
        } catch (Exception ex) {
            LOGGER.error("Error starting SharedCount", ex);
            throw new ServletException("Error starting SharedCount", ex);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            counter.close();
        } catch (IOException ex) {
            LOGGER.warn("Error closing SharedCount due to: " + ex + ". This exception is ignored.");
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        LOGGER.debug("GitHttp service req={}", req);
        super.service(req, res);
        LOGGER.debug("GitHttp service res={}", res);
        
        // Ignore unwanted service requests
        String resContentType = res.getContentType();
        if (resContentType.contains("x-git-receive-pack-result")) {
            
            LOGGER.info("GitHttp service res={}", res);
            
            int httpStatus = 0;
            try {
                Method method = res.getClass().getMethod("getStatus");
                httpStatus = (Integer) method.invoke(res, new Object[] {});
            } catch (Exception ex) {
                LOGGER.error("Cannot obtain http response code: " + ex);
            }
            
            if (httpStatus == HttpServletResponse.SC_OK) {
                try {
                    List<Ref> refs = git.branchList().call();
                    LOGGER.info("Remote git content updated: {}", refs);
                    while (!counter.trySetCount(counter.getCount() + 1));
                } catch (Exception ex) {
                    LOGGER.debug("Error incrementing shared counter: " + ex + ". This exception is ignored.", ex);
                    LOGGER.warn("Error incrementing shared counter: " + ex + ". This exception is ignored.");
                }
            }
        } 
    }
}
