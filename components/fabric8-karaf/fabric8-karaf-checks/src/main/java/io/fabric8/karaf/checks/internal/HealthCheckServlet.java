/**
 * Copyright 2005-2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

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
package io.fabric8.karaf.checks.internal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.fabric8.karaf.checks.Check;
import io.fabric8.karaf.checks.HealthChecker;

public class HealthCheckServlet extends HttpServlet {

    private final CopyOnWriteArrayList<HealthChecker> checkers;

    public HealthCheckServlet(CopyOnWriteArrayList<HealthChecker> checkers) {
        this.checkers = checkers;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Check> checks = new ArrayList<>();
        for (HealthChecker checker : checkers) {
            checks.addAll(checker.getFailingHealthChecks());
        }
        if (checks.isEmpty()) {
            resp.getWriter().println("HEALTHY");
        } else {
            resp.setStatus(503);
            resp.getWriter().println("NOT HEALTHY");
            for (Check check : checks) {
                resp.getWriter().println(check.getName() + ": " + check.getLongDescription());
            }
        }
    }

}
