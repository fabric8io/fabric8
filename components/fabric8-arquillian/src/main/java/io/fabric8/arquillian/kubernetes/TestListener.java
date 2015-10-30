/*
 * Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.arquillian.kubernetes;

import io.fabric8.arquillian.kubernetes.log.Logger;
import io.fabric8.arquillian.utils.Namespaces;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.event.suite.AfterTestLifecycleEvent;
import org.jboss.arquillian.test.spi.event.suite.BeforeTestLifecycleEvent;

public class TestListener {

    public void start(@Observes(precedence = Integer.MIN_VALUE) BeforeTestLifecycleEvent event, KubernetesClient client, Session session) {
        Namespaces.updateNamespaceTestStatus(client, session, event.getTestClass().getName(), "RUNNING");
    }

    public void stop(@Observes(precedence = Integer.MIN_VALUE) AfterTestLifecycleEvent event, TestResult result, KubernetesClient client, Session session, Logger logger) {
        Namespaces.updateNamespaceTestStatus(client, session, event.getTestClass().getName(), result.getStatus().name());
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
}
