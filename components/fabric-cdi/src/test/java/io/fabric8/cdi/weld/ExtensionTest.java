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

package io.fabric8.cdi.weld;

import io.fabric8.cdi.ServiceConverters;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesClient;
import org.junit.Assert;
import org.junit.Test;

public class ExtensionTest extends WeldTestBase {
    
    @Test
    public void testClientInjection() {
        container.instance().select(ServiceConverters.class).get();
        Kubernetes client =  container.instance().select(KubernetesClient.class).get();
        Assert.assertNotNull(client);
    }

    @Test
    public void testServiceInjection() {
        TestBean testBean = container.instance().select(TestBean.class).get();
        Assert.assertNotNull(testBean);
        Assert.assertNotNull(testBean.getKubernetesUrl());
        Assert.assertNotNull(testBean.getConsoleUrl());
    }
}
