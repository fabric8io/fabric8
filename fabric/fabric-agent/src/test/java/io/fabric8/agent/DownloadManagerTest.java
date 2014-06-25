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
package io.fabric8.agent;

import java.net.URL;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.fabric8.agent.download.DownloadFuture;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.FutureListener;
import io.fabric8.agent.mvn.MavenConfigurationImpl;
import io.fabric8.agent.mvn.MavenSettingsImpl;
import io.fabric8.agent.mvn.PropertiesPropertyResolver;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;

public class DownloadManagerTest {

    public static Logger LOG = LoggerFactory.getLogger(DownloadManagerTest.class);

    @Test
    public void downloadCamelCoreSnapshot() throws Exception {
        String home = System.getProperty("user.home");
        Properties properties = new Properties();
        properties.setProperty("mvn.localRepository", home + "/.m2/repository/@snapshots");
        properties.setProperty("mvn.repositories", "http://repo1.maven.org/maven2/");
        PropertiesPropertyResolver propertyResolver = new PropertiesPropertyResolver(properties);
        MavenConfigurationImpl mavenConfiguration = new MavenConfigurationImpl(propertyResolver, "mvn");
        mavenConfiguration.setSettings(new MavenSettingsImpl(new URL("file:" + home + "/.m2/settings.xml")));
        DownloadManager dm = new DownloadManager(mavenConfiguration, Executors.newSingleThreadExecutor());

        final CountDownLatch latch = new CountDownLatch(1);
        DownloadFuture df = dm.download("mvn:io.fabric8.archetypes/karaf-camel-cbr-archetype/1.1.0-SNAPSHOT");
        df.addListener(new FutureListener<DownloadFuture>() {
            @Override
            public void operationComplete(DownloadFuture future) {
                latch.countDown();
            }
        });

        latch.await(30, TimeUnit.SECONDS);
        assertNotNull(df.getUrl());
        assertNotNull(df.getFile());
        LOG.info("Downloaded URL={}, FILE={}", df.getUrl(), df.getFile());
    }

}
