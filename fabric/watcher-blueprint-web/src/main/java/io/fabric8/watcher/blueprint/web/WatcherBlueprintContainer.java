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
package io.fabric8.watcher.blueprint.web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.blueprint.container.BlueprintContainerImpl;
import io.fabric8.watcher.PathHelper;
import io.fabric8.watcher.Paths;
import io.fabric8.watcher.Processor;
import io.fabric8.watcher.file.FileWatcher;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link io.fabric8.watcher.file.FileWatcher} which finds OSGi Blueprint XML files
 * using the given file patterns and loads them up and starts them automatically
 */
public class WatcherBlueprintContainer extends FileWatcher {
    private static final transient Logger LOG = LoggerFactory.getLogger(WatcherBlueprintContainer.class);
    public static final String BLUEPRINT_NAMESPACE_URI = "http://www.osgi.org/xmlns/blueprint/v1.0.0";

    private ConcurrentHashMap<URL, BlueprintContainer> containerMap
            = new ConcurrentHashMap<URL, BlueprintContainer>();
    private ClassLoader classLoader;
    private Map<String, String> properties = new HashMap<String, String>();
    private AtomicBoolean closing = new AtomicBoolean(false);
    private BlueprintContainer parentContainer;

    public WatcherBlueprintContainer() {
        this.classLoader = getClass().getClassLoader();
        setFileMatchPattern("glob:**.xml");
        setProcessor(new Processor() {
            public void process(Path path) {
                if (!closing.get()) {
                    addPath(path);
                }
            }

            public void onRemove(Path path) {
                if (!closing.get()) {
                    removePath(path);
                }
            }
        });
    }

    public void init() throws IOException {
        super.init();
        LOG.info("Watching directory " + getRoot() + " for Blueprint XML files to load");
    }

    public void destroy() {
        if (closing.compareAndSet(false, true)) {
            Set<Map.Entry<URL, BlueprintContainer>> entries = containerMap.entrySet();
            for (Map.Entry<URL, BlueprintContainer> entry : entries) {
                URL url = entry.getKey();
                BlueprintContainer container = entry.getValue();
                closeContainer(url, container);
            }
        }
        super.destroy();
    }

    // Properties
    //-------------------------------------------------------------------------

    public Set<URL> getContainerURLs() {
        return new HashSet<URL>(containerMap.keySet());
    }

    public BlueprintContainer getContainer(URL url) {
        return containerMap.get(url);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public BlueprintContainer getParentContainer() {
        return parentContainer;
    }

    public void setParentContainer(BlueprintContainer parentContainer) {
        this.parentContainer = parentContainer;
        if (parentContainer instanceof BlueprintContainerImpl) {
            BlueprintContainerImpl impl = (BlueprintContainerImpl)parentContainer;

            // TODO we could auto-default the ClassLoader and Properties here if
            // https://issues.apache.org/jira/browse/ARIES-1092 were fixed
/*
            TODO
            setProperties(impl.getProperties());
            setClassLoader(impl.getClassLoader());
*/
        }
    }

    // Implementation
    //-------------------------------------------------------------------------
    protected void addPath(Path path) {
        URL url = toUrl(path);
        if (url != null) {
            BlueprintContainer container = containerMap.get(url);
            if (container != null) {
                // There is no refresh API so lets close and restart
                closeContainer(url, container);
            }

            try {
                container = createContainer(path, url);
                if (container != null) {
                    containerMap.put(url, container);
                }
            } catch (Exception e) {
                LOG.info("Failed to create container at " + url + ". " + e, e);
            }
        }
    }


    protected void removePath(Path path) {
        URL url = toUrl(path);
        if (url != null) {
            BlueprintContainer container = containerMap.remove(url);
            closeContainer(url, container);
        }
    }

    protected BlueprintContainer createContainer(Path path, URL url) throws Exception {
        if (!Paths.hasNamespace(path, BLUEPRINT_NAMESPACE_URI)) {
            LOG.info("Ignoring XML file " + path + " which is not a blueprint XML");
            return null;
        }
        LOG.info("Creating container at " + url);
        List<URL> locations = Arrays.asList(url);
        return new BlueprintContainerImpl(classLoader, locations, properties, true);
    }

    protected void closeContainer(URL url, BlueprintContainer container) {
        if (container instanceof BlueprintContainerImpl) {
            BlueprintContainerImpl impl = (BlueprintContainerImpl)container;
            try {
                LOG.info("Closing container at path " + url + " container " + container);
                impl.destroy();
            } catch (Exception e) {
                LOG.info("Failed to close at " + url + " container " + container + ". " + e, e);
            }
        }
    }

    protected URL toUrl(Path path) {
        URL url = null;
        try {
            url = PathHelper.toURL(path);
        } catch (MalformedURLException e) {
            LOG.warn("Ignored path " + path + " due to: " + e, e);
        }
        return url;
    }

}
