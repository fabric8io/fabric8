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

package io.fabric8.spring.boot;

import io.fabric8.annotations.ServiceName;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.model.Service;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Fabric8Application.class)
public class Fabric8ApplicationTest {

    @Autowired
    private Kubernetes kubernetes;

    @Autowired
    @ServiceName("fabric8-console-service")
    private String consoleService;

    @Test
    public void testKubernetesClientAvailable() {
        System.out.println(consoleService);
        Assert.assertNotNull(kubernetes);
    }


}