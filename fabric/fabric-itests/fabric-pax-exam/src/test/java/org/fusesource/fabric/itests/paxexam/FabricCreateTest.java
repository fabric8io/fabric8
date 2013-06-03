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

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.apache.curator.framework.state.ConnectionStateListener;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.zookeeper.internal.OsgiZkClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.debugConfiguration;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricCreateTest extends FabricTestSupport {

    @Test
    public void testImportedProfiles() throws Exception {
        FabricService fabricService = getFabricService();
        assertNotNull(fabricService);

        System.err.println(executeCommand("fabric:create -n"));

        Profile karafProfile = fabricService.getDefaultVersion().getProfile("karaf");
        assertNotNull(karafProfile);

        Profile camelProfile = fabricService.getDefaultVersion().getProfile("camel");
        assertNotNull(camelProfile);

        Profile activeMq = fabricService.getDefaultVersion().getProfile("mq");
        assertNotNull(activeMq);
    }


    /*
    @Test
    public void testCreateWithConnectionLoss() throws Exception {
        FabricService fabricService = getFabricService();
        assertNotNull(fabricService);

        //Generate a connection loss right after the client is connected
        bundleContext.registerService(ConnectionStateListener.class.getName(),
                new ConnectionStateListener() {
                    private boolean first = true;

                    @Override
                    public void onConnected() {
                        System.err.println("Connected");
                        System.err.flush();
                        if (first) {
                            first = false;
                            try {
                                final OsgiZkClient zooKeeper = (OsgiZkClient) getCurator();
                                Thread.sleep(200);
                                zooKeeper.testGenerateConnectionLoss();
                            } catch (Exception e) {
                                e.printStackTrace(System.err);
                            }
                        }
                    }

                    @Override
                    public void onDisconnected() {
                        System.err.println("Disconnected");
                        System.err.flush();
                    }
                }, new Hashtable<String, Object>());

        System.err.println(executeCommand("fabric:create -n", 90000L, false));
        System.err.println(executeCommand("fabric:container-list", 90000L, false));

    }*/

    @Test
    public void testCreateWithProfileSelection() throws Exception {
        FabricService fabricService = getFabricService();
        assertNotNull(fabricService);

        System.err.println(executeCommand("fabric:create -n --profile camel"));

        Profile[] profiles = fabricService.getCurrentContainer().getProfiles();
        List<String> profileNames = new LinkedList<String>();
        for (Profile profile:profiles) {
            profileNames.add(profile.getId());
        }

        assertTrue(profileNames.contains("fabric"));
        assertTrue(profileNames.contains("camel"));
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration())
        };
    }
}
