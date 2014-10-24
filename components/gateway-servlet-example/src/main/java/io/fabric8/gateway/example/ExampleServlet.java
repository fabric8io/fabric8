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
package io.fabric8.gateway.example;

import io.fabric8.gateway.model.HttpProxyRuleBase;
import io.fabric8.gateway.servlet.ProxyServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * Provide the gateway mapping rules
 */
public class ExampleServlet extends ProxyServlet {

    /**
     * We could load the mapping rules from a database, from configuration files or whatnot.
     *
     * For now lets just define them in the Java DSL
     */
    @Override
    protected void loadRuleBase(ServletConfig config, HttpProxyRuleBase ruleBase) throws ServletException {
        ruleBase.rule("/search/{path}").to("http://google.com/?q={path}");
    }
}
