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
package org.fusesource.fabric.jaxb.dynamic.profile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Throwables;
import com.google.common.io.Closeables;

import org.apache.aries.util.AriesFrameworkUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.common.util.Maps;
import org.fusesource.common.util.Strings;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.Containers;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.DataStorePlugin;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.PlaceholderResolver;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Profiles;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.fusesource.fabric.jaxb.dynamic.CompileResults;
import org.fusesource.fabric.jaxb.dynamic.CompileResultsHandler;
import org.fusesource.fabric.jaxb.dynamic.DynamicCompiler;
import org.fusesource.fabric.jaxb.dynamic.DynamicXJC;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches the {@link DataStore} for XSD files to recompile
 */
@Component(name = "org.fusesource.fabric.profile.jaxb.compiler", description = "Fabric Profile JAXB Compiler", immediate = true)
@Service({DynamicCompiler.class})
public class ProfileDynamicJaxbCompiler implements DynamicCompiler {
    public static final String PROPERTY_SCHEMA_PATH = "schemaPath";

    private static final transient Logger LOG = LoggerFactory.getLogger(ProfileDynamicJaxbCompiler.class);

    @Reference(referenceInterface = DataStore.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    private String schemaPath;
    private BundleContext bundleContext;
    private Timer timer = new Timer();
    private AtomicBoolean startedFlag = new AtomicBoolean(false);

    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final ConcurrentMap<String, PathChildrenCache>
            pathCacheMap = new ConcurrentHashMap<String, PathChildrenCache>();
    private long timerDelay = 1000;
    private CompileResults compileResults;
    private CompileResultsHandler handler;
    private Runnable changeRunnable = new Runnable() {
        @Override
        public void run() {
            asyncRecompile();
        }
    };

    public ProfileDynamicJaxbCompiler() {
    }

    @Activate
    public void init(Map<String,String> configuration) {
        try {
            this.schemaPath = Maps.stringValue(configuration, PROPERTY_SCHEMA_PATH, "schemas");
            watchSchemaFolders();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Deactivate
    public void destroy() throws IOException {
        executorService.shutdown();
        timer.cancel();
    }

    public void setHandler(CompileResultsHandler handler) throws Exception {
        this.handler = handler;
        // lets pass in the first set of results if we've compiled before the
        // handler is registered
        if (handler != null && compileResults != null) {
            handler.onCompileResults(compileResults);
        }
    }

    // Properties
    //-------------------------------------------------------------------------

    /**
     * Returns the latest XJC compiler results
     */
    public CompileResults getCompileResults() {
        return compileResults;
    }

    public FabricService getFabricService() {
        return fabricService.get();
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public String getSchemaPath() {
        return schemaPath;
    }

    public void setSchemaPath(String schemaPath) {
        this.schemaPath = schemaPath;
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public long getTimerDelay() {
        return timerDelay;
    }

    public void setTimerDelay(long timerDelay) {
        this.timerDelay = timerDelay;
    }

    // Implementation
    //-------------------------------------------------------------------------

    protected void asyncRecompile() {
        if (startedFlag.compareAndSet(false, true)) {
            timer.schedule(new RecompileTask(), timerDelay);
        }
    }


    /**
     * Lets try resolve the current {@link #getSchemaPath()} based on the profile paths
     */
    protected void watchSchemaFolders() throws Exception {
        getFabricService().getDataStore().trackConfiguration(changeRunnable);
    }

    private class RecompileTask extends TimerTask {
        @Override
        public void run() {
            recompile();
        }
    }

    protected void recompile() {
        LOG.debug("Looking for XSDs to recompile");

        Set<String> urls = new TreeSet<String>();
        FabricService fabric = getFabricService();
        Container container = fabric.getCurrentContainer();
        String version = container.getVersion().getId();
        List<Profile> profiles = Containers.overlayProfiles(container);
        List<String> profileIds = Profiles.profileIds(profiles);
        Collection<String> names = fabric.getDataStore().listFiles(version, profileIds, schemaPath);
        for (String name : names) {
            if (name.endsWith(".xsd")) {
                String prefix = schemaPath;
                if (Strings.isNullOrBlank(prefix)) {
                    prefix += "/";
                }
                urls.add("profile:" + prefix + name);
            }
        }

        LOG.info("Recompiling XSDs at URLs: " + urls);
        startedFlag.set(false);

        ClassLoader classLoader = AriesFrameworkUtil.getClassLoader(bundleContext.getBundle());
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        DynamicXJC xjc = new DynamicXJC(classLoader);
        xjc.setSchemaUrls(new ArrayList<String>(urls));
        compileResults = xjc.compileSchemas();
        if (handler != null) {
            handler.onCompileResults(compileResults);
        }
    }
}
