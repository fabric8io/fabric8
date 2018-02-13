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
package io.fabric8.karaf.cm;

import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KubernetesConfigAdminBridgeTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesConfigAdminBridgeTest.class);

    @Mock
    private ConfigurationAdmin caService;

    private ConfigMapList cmEmptyList = new ConfigMapList();

    @Test
    public void testAand(){
        System.setProperty("fabric8.pid.filters", "appName=A,database.name=my.oracle.datasource");
        KubernetesMockServer plainServer = new KubernetesMockServer(false);

        plainServer.expect().get().withPath("/api/v1/namespaces/test/configmaps?labelSelector=karaf.pid,appName%20in%20(A),database.name%20in%20(my.oracle.datasource)&watch=true").andReturnChunked(200).always();
        plainServer.expect().get().withPath("/api/v1/namespaces/test/configmaps?labelSelector=karaf.pid,appName%20in%20(A),database.name%20in%20(my.oracle.datasource)").andReturn(200, cmEmptyList).once();

        KubernetesConfigAdminBridge kcab = new KubernetesConfigAdminBridge();
        kcab.bindConfigAdmin( caService );
        kcab.bindKubernetesClient( plainServer.createClient() );

        kcab.activate();
    }

    @Test
    public void testOr(){
        System.setProperty("fabric8.pid.filters", "appName=A;B");
        KubernetesMockServer plainServer = new KubernetesMockServer(false);

        plainServer.expect().get().withPath("/api/v1/namespaces/test/configmaps?labelSelector=karaf.pid,appName%20in%20(A,B)&watch=true").andReturnChunked(200).always();
        plainServer.expect().get().withPath("/api/v1/namespaces/test/configmaps?labelSelector=karaf.pid,appName%20in%20(A,B)").andReturn(200, cmEmptyList).once();

        KubernetesConfigAdminBridge kcab = new KubernetesConfigAdminBridge();
        kcab.bindConfigAdmin( caService );
        kcab.bindKubernetesClient( plainServer.createClient() );

        kcab.activate();
    }

    @Test
    public void testAndOr(){
        System.setProperty("fabric8.pid.filters", "appName=A;B,database.name=my.oracle.datasource");
        KubernetesMockServer plainServer = new KubernetesMockServer(false);

        plainServer.expect().get().withPath("/api/v1/namespaces/test/configmaps?labelSelector=karaf.pid,appName%20in%20(A,B),database.name%20in%20(my.oracle.datasource)&watch=true").andReturnChunked(200).always();
        plainServer.expect().get().withPath("/api/v1/namespaces/test/configmaps?labelSelector=karaf.pid,appName%20in%20(A,B),database.name%20in%20(my.oracle.datasource)").andReturn(200, cmEmptyList).once();

        KubernetesConfigAdminBridge kcab = new KubernetesConfigAdminBridge();
        kcab.bindConfigAdmin( caService );
        kcab.bindKubernetesClient( plainServer.createClient() );

        kcab.activate();
    }
}

