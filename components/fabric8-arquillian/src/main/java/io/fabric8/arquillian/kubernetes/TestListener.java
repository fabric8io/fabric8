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
package io.fabric8.arquillian.kubernetes;

import io.fabric8.arquillian.utils.Namespaces;
import io.fabric8.kubernetes.api.Annotations;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.event.suite.AfterTestLifecycleEvent;
import org.jboss.arquillian.test.spi.event.suite.BeforeTestLifecycleEvent;

public class TestListener {

    private static final int MAX_ANNOTATION_KEY_LENGTH = 63;

    public void start(@Observes(precedence = Integer.MIN_VALUE) BeforeTestLifecycleEvent event, KubernetesClient client, Session session) {
        String pkg = event.getTestClass().getJavaClass().getPackage().getName();
        String className = event.getTestClass().getJavaClass().getSimpleName();
        String methodName = event.getTestMethod().getName();
        Namespaces.updateNamespaceTestStatus(client, session, trimName(pkg, className, methodName), "RUNNING");
    }

    public void stop(@Observes(precedence = Integer.MIN_VALUE) AfterTestLifecycleEvent event, TestResult result, KubernetesClient client, Session session) {
        String pkg = event.getTestClass().getJavaClass().getPackage().getName();
        String className = event.getTestClass().getJavaClass().getSimpleName();
        String methodName = event.getTestMethod().getName();

        Namespaces.updateNamespaceTestStatus(client, session, trimName(pkg, className, methodName), result.getStatus().name());
        switch (result.getStatus()) {
            case PASSED:
                session.getPassed().incrementAndGet();
                break;
            case FAILED:
                session.getFailed().incrementAndGet();
                break;
            case SKIPPED:
                session.getSkiped().incrementAndGet();
        }
    }

    static String trimName(String packageName, String className, String methodName) {
        StringBuilder sb = new StringBuilder();
        sb.append(trimPackage(packageName)).append(".").append(className).append(".").append(methodName);
        String result = sb.toString();
        int prefixLength = Annotations.Tests.TEST_CASE_STATUS.length();
        if (prefixLength + result.length() > MAX_ANNOTATION_KEY_LENGTH) {
            result = result.substring(prefixLength + result.length() - MAX_ANNOTATION_KEY_LENGTH);
        }
        if (result.charAt(0) == '.') {
            result = result.substring(1);
        }
        return result;
    }

    static String trimPackage(String pkg) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String part : pkg.split("\\.")) {
            if (first) {
                first = false;
            } else {
                sb.append(".");
            }
            sb.append(part.substring(0, 1));
        }
        return sb.toString();
    }
}
