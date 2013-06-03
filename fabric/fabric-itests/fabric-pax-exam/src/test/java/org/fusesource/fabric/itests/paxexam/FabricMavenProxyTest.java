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

package org.fusesource.fabric.itests.paxexam;

import junit.framework.Assert;
import org.apache.curator.framework.CuratorFramework;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.nio.entity.FileNIOEntity;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.Provision;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getChildren;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getSubstitutedPath;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.debugConfiguration;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricMavenProxyTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    public void testUpload() throws Exception {
        String featureLocation = System.getProperty("feature.location");
        System.out.println("Testing with feature from:" + featureLocation);
        System.err.println(executeCommand("fabric:create -n"));
        Set<Container> containers = ContainerBuilder.create(2).withName("maven").withProfiles("fabric").assertProvisioningResult().build();

        FabricService fabricService = getFabricService();
        CuratorFramework curator = getCurator();
        List<String> children = getChildren(curator, ZkPath.MAVEN_PROXY.getPath("upload"));
        List<String> uploadUrls = new ArrayList<String>();
        for (String child : children) {
            String uploadeUrl = getSubstitutedPath(curator, ZkPath.MAVEN_PROXY.getPath("upload") + "/" + child);
            uploadUrls.add(uploadeUrl);
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

        FileNIOEntity entity = new FileNIOEntity(new File(featureLocation),"text/xml");
        put.setEntity(entity);
        HttpResponse response = client.execute(put);
        System.err.println("Response:" + response.getStatusLine());
        Assert.assertTrue(response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 202);

        System.err.println(executeCommand("fabric:profile-edit --repositories mvn:itest/itest/1.0/xml/features default"));
        System.err.println(executeCommand("fabric:profile-edit --features example-cbr default"));
        Provision.waitForContainerStatus(containers, PROVISION_TIMEOUT);
    }


    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                mavenBundle("org.apache.httpcomponents", "httpcore-osgi").versionAsInProject(),
                mavenBundle("org.apache.httpcomponents", "httpclient-osgi").versionAsInProject(),
                editConfigurationFilePut("etc/system.properties", "feature.location", FabricMavenProxyTest.class.getResource("/test-features.xml").getFile()),
                debugConfiguration("5005", false)
        };
    }
}
