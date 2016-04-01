/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.cdi.deltaspike;

import org.apache.deltaspike.core.impl.scope.conversation.ConversationBeanHolder;
import org.apache.deltaspike.core.impl.scope.viewaccess.ViewAccessBeanAccessHistory;
import org.apache.deltaspike.core.impl.scope.viewaccess.ViewAccessBeanHolder;
import org.apache.deltaspike.core.impl.scope.viewaccess.ViewAccessViewHistory;
import org.apache.deltaspike.core.impl.scope.window.DefaultWindowContextQuotaHandler;
import org.apache.deltaspike.core.impl.scope.window.WindowBeanHolder;
import org.apache.deltaspike.core.impl.scope.window.WindowContextProducer;
import org.apache.deltaspike.core.impl.scope.window.WindowContextQuotaHandlerCache;
import org.apache.deltaspike.core.impl.scope.window.WindowIdHolder;
import org.apache.deltaspike.core.spi.config.ConfigSourceProvider;
import org.apache.deltaspike.core.spi.scope.window.WindowContextQuotaHandler;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.File;

public class DeltaspikeTestBase {

    public static Class[] getDeltaSpikeHolders() {
        return new Class<?>[]{
                WindowContextProducer.class,
                WindowContextQuotaHandlerCache.class,
                DefaultWindowContextQuotaHandler.class,
                WindowContextQuotaHandler.class,
                WindowBeanHolder.class,
                WindowIdHolder.class,
                ConversationBeanHolder.class,
                ViewAccessBeanHolder.class,
                ViewAccessBeanAccessHistory.class,
                ViewAccessViewHistory.class};
    }
    
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("org.apache.deltaspike.core:deltaspike-core-impl").withTransitivity().as(File.class))
                .addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("io.fabric8:fabric8-cdi").withoutTransitivity().as(File.class))
                .addAsServiceProvider(ConfigSourceProvider.class);
    }
}
