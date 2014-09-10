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
package io.fabric8.container.process;

import org.apache.curator.CuratorZookeeperClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.curator.framework.api.DeleteBuilder;
import org.apache.curator.framework.api.ExistsBuilder;
import org.apache.curator.framework.api.GetACLBuilder;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.framework.api.SetACLBuilder;
import org.apache.curator.framework.api.SetDataBuilder;
import org.apache.curator.framework.api.SyncBuilder;
import org.apache.curator.framework.api.UnhandledErrorListener;
import org.apache.curator.framework.api.transaction.CuratorTransaction;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.utils.EnsurePath;
import org.apache.zookeeper.Watcher;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.fabric8.container.process.JolokiaAgentHelper.substituteVariableExpression;
import static org.junit.Assert.assertEquals;

public class JolokiaAgentHelperSubstituteTest {

    private CuratorFramework curator;

    @Test
    public void testExpressions() throws Exception {
        System.setProperty("CHEESE", "Edam");

        String JAVA_HOME = System.getenv("JAVA_HOME");
        assertExpression("${env:JAVA_HOME}", JAVA_HOME);
        if (JAVA_HOME != null) {
            assertExpression("A${env:JAVA_HOME}B", "A" + JAVA_HOME + "B");
            assertExpression("A${env:JAVA_HOME?:DEFAULT}B", "A" + JAVA_HOME + "B");
        }
        assertExpression("${env:DOES_NOT_EXIST?:DEFAULT}", "DEFAULT");
        assertExpression("A${env:DOES_NOT_EXIST?:DEFAULT}B", "ADEFAULTB");
        assertExpression("${env:DOES_NOT_EXIST?:DEFAULT}${env:DOES_NOT_EXIST?:DEFAULT}", "DEFAULTDEFAULT");
        assertExpression("1${env:DOES_NOT_EXIST?:DEFAULT}2${env:DOES_NOT_EXIST?:DEFAULT}3", "1DEFAULT2DEFAULT3");

    }

    @Test
    public void testHttpUrlExpressions() throws Exception {
        Map<String, String> envVars = new HashMap<>();
        String expression = "http://${env:FABRIC8_LISTEN_ADDRESS}:${env:FABRIC8_HTTP_PROXY_PORT}/";
        assertExpression(expression, expression, true, envVars, curator);

        assertExpression(expression, "http://null:null/", false, envVars, curator);

        envVars.put("FABRIC8_LISTEN_ADDRESS", "localhost");
        envVars.put("FABRIC8_HTTP_PROXY_PORT", "8181");
        assertExpression(expression, "http://localhost:8181/", true, envVars, curator);
    }

    @Test
    public void testHttpUrlExpressionsWithCurator() throws Exception {
        curator = new MockCuratorFramework();
        testHttpUrlExpressions();
    }


    @Test
    public void testPreserveUnresolved() throws Exception {
        String[] expressions = {
                "${env:DOES_NOT_EXIST?:DEFAULT}",
                "A${env:DOES_NOT_EXIST?:DEFAULT}B",
                "${env:DOES_NOT_EXIST?:DEFAULT}${env:DOES_NOT_EXIST?:DEFAULT}",
                "1${env:DOES_NOT_EXIST?:DEFAULT}2${env:DOES_NOT_EXIST?:DEFAULT}3"

        };
        for (String expression : expressions) {
            assertExpression(expression, expression, true);
        }
    }

    @Test
    public void testProvidedEnvironmentVar() throws Exception {
        Map<String, String> vars = new HashMap<>();
        vars.put("CHEESE", "Edam");

        assertExpression("${env:CHEESE}", "Edam", false, vars, curator);
        assertExpression("A${env:CHEESE}B", "A" + "Edam" + "B", false, vars, curator);
        assertExpression("A${env:CHEESE?:DEFAULT}B", "A" + "Edam" + "B", false, vars, curator);

        assertExpression("${env:DOES_NOT_EXIST?:DEFAULT}", "DEFAULT", false, vars, curator);
        assertExpression("A${env:DOES_NOT_EXIST?:DEFAULT}B", "ADEFAULTB", false, vars, curator);
        assertExpression("${env:DOES_NOT_EXIST?:DEFAULT}${env:DOES_NOT_EXIST?:DEFAULT}", "DEFAULTDEFAULT", false, vars, curator);
        assertExpression("1${env:DOES_NOT_EXIST?:DEFAULT}2${env:DOES_NOT_EXIST?:DEFAULT}3", "1DEFAULT2DEFAULT3", false, vars, curator);
    }

    public String assertExpression(String expression, String expectedValue) {
        return assertExpression(expression, expectedValue, false);
    }

    public String assertExpression(String expression, String expectedValue, boolean preserveUnresolved) {
        return assertExpression(expression, expectedValue, preserveUnresolved, System.getenv(), curator);
    }

    public static String assertExpression(String expression, String expectedValue, boolean preserveUnresolved, Map<String, String> envVars, CuratorFramework curator) {
        String actual = substituteVariableExpression(expression, envVars, null, curator, preserveUnresolved);
        System.out.println("expression> " + expression + " => " + actual);
        assertEquals("Expression " + expression, expectedValue, actual);
        return actual;
    }

    private static class MockCuratorFramework implements CuratorFramework {
        @Override
        public void start() {
        }

        @Override
        public void close() {
        }

        @Override
        public CuratorFrameworkState getState() {
            return null;
        }

        @Override
        public boolean isStarted() {
            return false;
        }

        @Override
        public CreateBuilder create() {
            return null;
        }

        @Override
        public DeleteBuilder delete() {
            return null;
        }

        @Override
        public ExistsBuilder checkExists() {
            return null;
        }

        @Override
        public GetDataBuilder getData() {
            return null;
        }

        @Override
        public SetDataBuilder setData() {
            return null;
        }

        @Override
        public GetChildrenBuilder getChildren() {
            return null;
        }

        @Override
        public GetACLBuilder getACL() {
            return null;
        }

        @Override
        public SetACLBuilder setACL() {
            return null;
        }

        @Override
        public CuratorTransaction inTransaction() {
            return null;
        }

        @Override
        public void sync(String s, Object o) {
        }

        @Override
        public SyncBuilder sync() {
            return null;
        }

        @Override
        public Listenable<ConnectionStateListener> getConnectionStateListenable() {
            return null;
        }

        @Override
        public Listenable<CuratorListener> getCuratorListenable() {
            return null;
        }

        @Override
        public Listenable<UnhandledErrorListener> getUnhandledErrorListenable() {
            return null;
        }

        @Override
        public CuratorFramework nonNamespaceView() {
            return null;
        }

        @Override
        public CuratorFramework usingNamespace(String s) {
            return null;
        }

        @Override
        public String getNamespace() {
            return null;
        }

        @Override
        public CuratorZookeeperClient getZookeeperClient() {
            return null;
        }

        @Override
        public EnsurePath newNamespaceAwareEnsurePath(String s) {
            return null;
        }

        @Override
        public void clearWatcherReferences(Watcher watcher) {
        }

        @Override
        public boolean blockUntilConnected(int i, TimeUnit timeUnit) throws InterruptedException {
            return false;
        }

        @Override
        public void blockUntilConnected() throws InterruptedException {
        }
    }
}
