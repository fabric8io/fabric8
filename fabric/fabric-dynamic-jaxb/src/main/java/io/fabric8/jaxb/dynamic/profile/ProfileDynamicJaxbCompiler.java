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
package io.fabric8.jaxb.dynamic.profile;

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

import io.hawt.util.introspect.ClassLoaderProvider;
import io.hawt.introspect.Introspector;

import org.apache.aries.util.AriesFrameworkUtil;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.common.util.Maps;
import org.fusesource.common.util.Strings;
import io.fabric8.api.Container;
import io.fabric8.api.Containers;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Profiles;
import io.fabric8.jaxb.dynamic.CompileResults;
import io.fabric8.jaxb.dynamic.CompileResultsHandler;
import io.fabric8.jaxb.dynamic.DynamicCompiler;
import io.fabric8.jaxb.dynamic.DynamicXJC;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches the {@link DataStore} for XSD files to recompile
 */
@Component(name = "io.fabric8.profile.jaxb.compiler", label = "Fabric8 Profile JAXB Compiler", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service({DynamicCompiler.class})
public class ProfileDynamicJaxbCompiler implements DynamicCompiler {
    public static final String PROPERTY_SCHEMA_PATH = "schemaPath";

    private static final transient Logger LOG = LoggerFactory.getLogger(ProfileDynamicJaxbCompiler.class);

    private BundleContext bundleContext;

    @Reference(referenceInterface = FabricService.class)
    private FabricService fabricService;

    @Reference(referenceInterface = Introspector.class, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private Introspector introspector;

    private String schemaPath;
    private Timer timer = new Timer();
    private AtomicBoolean startedFlag = new AtomicBoolean(false);

    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final ConcurrentMap<String, PathChildrenCache> pathCacheMap = new ConcurrentHashMap<String, PathChildrenCache>();
    private long timerDelay = 1000;
    private CompileResults compileResults;
    private CompileResultsHandler handler;
    private Runnable changeRunnable = new Runnable() {
        @Override
        public void run() {
            asyncRecompile();
        }
    };
    private Introspector localIntrospector;

    public ProfileDynamicJaxbCompiler() {
    }

    @Activate
    void activate(BundleContext bundleContext, Map<String,String> configuration) {
        try {
            this.bundleContext = bundleContext;
            this.schemaPath = Maps.stringValue(configuration, PROPERTY_SCHEMA_PATH, "schemas");
            getDataStore().trackConfiguration(changeRunnable);

            if (introspector == null) {
                localIntrospector = new Introspector();
                localIntrospector.init();
                introspector = localIntrospector;
            }
            asyncRecompile();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Deactivate
    void deactivate() throws Exception {
        getDataStore().untrackConfiguration(changeRunnable);
        executorService.shutdown();
        timer.cancel();
        if (localIntrospector != null) {
            localIntrospector.destroy();
        }
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
        return fabricService;
    }

    public DataStore getDataStore() {
        return getFabricService().getDataStore();
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
                if (Strings.isNotBlank(prefix)) {
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
        if (introspector != null) {
            introspector.setClassLoaderProvider("dynamic.jaxb", new ClassLoaderProvider() {
                @Override
                public ClassLoader getClassLoader() {
                    return compileResults.getClassLoader();
                }
            });
        }
    }
}
