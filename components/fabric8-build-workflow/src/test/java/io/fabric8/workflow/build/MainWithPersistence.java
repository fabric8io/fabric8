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
package io.fabric8.workflow.build;

import io.fabric8.workflow.build.signal.BuildSignallerService;
import org.jbpm.test.JBPMHelper;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class MainWithPersistence {

    public static void main(String[] args) {
        KieServices ks = KieServices.Factory.get();
        KieContainer kContainer = ks.getKieClasspathContainer();
        KieBase kbase = kContainer.getKieBase("kbase");

        RuntimeManager manager = createRuntimeManager(kbase);
        RuntimeEngine engine = manager.getRuntimeEngine(null);
        KieSession ksession = engine.getKieSession();


        BuildSignallerService signallerService = new BuildSignallerService(ksession);
        signallerService.start();

        signallerService.join();

        manager.disposeRuntimeEngine(engine);
        System.exit(0);
    }

    private static RuntimeManager createRuntimeManager(KieBase kbase) {
        JBPMHelper.startH2Server();
        JBPMHelper.setupDataSource();
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
        RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.Factory.get()
                .newDefaultBuilder().entityManagerFactory(emf)
                .knowledgeBase(kbase);
        return RuntimeManagerFactory.Factory.get()
                .newSingletonRuntimeManager(builder.get(), "com.sample:example:1.0");
    }

}
