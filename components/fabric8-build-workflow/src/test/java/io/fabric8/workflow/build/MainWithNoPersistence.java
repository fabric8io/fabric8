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
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.internal.runtime.manager.InternalRuntimeManager;
import org.kie.internal.runtime.manager.context.EmptyContext;

import java.util.Collection;

/**
 * Main entry point for the application to interact with process engine.
 * It maintains single <code>RuntimeManager</code> instance that is the actual
 * Process Engine with all assets deployed to it.
 */
public class MainWithNoPersistence {


    private RuntimeManager runtimeManager;
    private KieBase kieBase;

    public static void main(String[] a) {
        MainWithNoPersistence processEngine = new MainWithNoPersistence();
        processEngine.init();
        RuntimeManager runtimeManager = processEngine.getRuntimeManager();
        RuntimeEngine engine = runtimeManager.getRuntimeEngine(EmptyContext.get());
        KieSession ksession = engine.getKieSession();
        KieBase kbase = engine.getKieSession().getKieBase();

        BuildSignallerService signallerService = new BuildSignallerService(ksession);
        signallerService.start();

        signallerService.join();

        runtimeManager.disposeRuntimeEngine(engine);
    }

    /**
     * Initializes process engine by creating <code>RuntimeEngine</code> instance will all assets deployed.
     */
    public void init() {
        if (runtimeManager == null) {

            KieServices ks = KieServices.Factory.get();
            KieContainer kContainer = ks.getKieClasspathContainer();
            kieBase = kContainer.getKieBase("kbase");

            RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.Factory.get().newEmptyBuilder()
                    .persistence(false).knowledgeBase(kieBase);

            runtimeManager = org.kie.api.runtime.manager.RuntimeManagerFactory.Factory.get()
                    .newSingletonRuntimeManager(builder.get(), "com.sample:example:1.0");
        }
    }

    /**
     * Disposes ProcessEngine by closing RuntimeManager instance.
     */
    public void dispose() {
        if (runtimeManager != null) {
            runtimeManager.close();
            runtimeManager = null;
        }
    }

    /**
     * Returns all available process definitions for this process engine.
     *
     * @return
     */
    public Collection<org.kie.api.definition.process.Process> getProcesses() {
        if (runtimeManager == null) {
            throw new IllegalStateException("RuntimeManager not initialized, did you forget to call init?");
        }
        return ((InternalRuntimeManager) runtimeManager).getEnvironment().getKieBase().getProcesses();
    }

    /**
     * Returns <code>RuntimeManager</code> for this process engine
     *
     * @return
     */
    public RuntimeManager getRuntimeManager() {
        if (runtimeManager == null) {
            throw new IllegalStateException("RuntimeManager not initialized, did you forget to call init?");
        }
        return runtimeManager;
    }
}


