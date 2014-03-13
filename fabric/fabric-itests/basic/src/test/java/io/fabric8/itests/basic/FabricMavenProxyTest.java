/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.itests.basic;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.nio.entity.FileNIOEntity;
import org.apache.karaf.tooling.exam.options.KarafDistributionOption;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricMavenProxyTest extends FabricTestSupport {

    @Test
    public void testUpload() throws Exception {
        String featureLocation = System.getProperty("feature.location");
        System.out.println("Testing with feature from:" + featureLocation);
        System.err.println(executeCommand("fabric:create -n"));
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            Set<ContainerProxy> containers = ContainerBuilder.create(fabricProxy, 2).withName("maven").withProfiles("fabric").assertProvisioningResult().build();
            try {
                List<String> uploadUrls = new ArrayList<String>();
                ServiceProxy<CuratorFramework> curatorProxy = ServiceProxy.createServiceProxy(bundleContext, CuratorFramework.class);
                try {
                    CuratorFramework curator = curatorProxy.getService();
                    List<String> children = ZooKeeperUtils.getChildren(curator, ZkPath.MAVEN_PROXY.getPath("upload"));
                    for (String child : children) {
                        String uploadeUrl = ZooKeeperUtils.getSubstitutedPath(curator, ZkPath.MAVEN_PROXY.getPath("upload") + "/" + child);
                        uploadUrls.add(uploadeUrl);
                    }
                } finally {
                    curatorProxy.close();
                }
                //Pick a random maven proxy from the list.
                Random random = new Random();
                int index = random.nextInt(uploadUrls.size());
                String targetUrl = uploadUrls.get(index);

                String uploadUrl = targetUrl + "itest/itest/1.0/itest-1.0-features.xml";
                System.out.println("Using URI: " + uploadUrl);
                DefaultHttpClient client = new DefaultHttpClient();
                HttpPut put = new HttpPut(uploadUrl);
                client.getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("admin", "admin"));

                FileNIOEntity entity = new FileNIOEntity(new File(featureLocation), "text/xml");
                put.setEntity(entity);
                HttpResponse response = client.execute(put);
                System.err.println("Response:" + response.getStatusLine());
                Assert.assertTrue(response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 202);

                System.err.println(executeCommand("fabric:profile-edit --repositories mvn:itest/itest/1.0/xml/features default"));
                System.err.println(executeCommand("fabric:profile-edit --features example-cbr default"));
                Provision.containerStatus(containers, PROVISION_TIMEOUT);
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[] {
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                mavenBundle("org.apache.httpcomponents", "httpcore-osgi").versionAsInProject(),
                mavenBundle("org.apache.httpcomponents", "httpclient-osgi").versionAsInProject(),
                mavenBundle("io.fabric8", "fabric-maven-proxy").versionAsInProject(),
                KarafDistributionOption.editConfigurationFilePut("etc/system.properties", "feature.location",
                        FabricMavenProxyTest.class.getResource("/test-features.xml").getFile()), KarafDistributionOption.debugConfiguration("5005", false) };
    }
}
