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
package io.fabric8.dozer.file;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.CamelContext;
import org.apache.camel.util.ServiceHelper;
import org.dozer.DozerBeanMapper;
import io.fabric8.watcher.Processor;
import io.fabric8.watcher.file.FileWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link FileWatcher} which watches for Dozer mapping XML files and adds/removes
 * the mappings from a {@link org.apache.camel.CamelContext}s {@link org.apache.camel.spi.TypeConverterRegistry}
 * whenever files is added, updated or removed.
 */
public class FileWatcherMapping extends FileWatcher {
    private static final transient Logger LOG = LoggerFactory.getLogger(FileWatcherMapping.class);

    private ConcurrentHashMap<String, WatcherDozerTypeConverterLoader> loaders = new ConcurrentHashMap<String, WatcherDozerTypeConverterLoader>();
    private CamelContext camelContext;
    private DozerBeanMapper mapper;

    public FileWatcherMapping() {
    }

    public void init() throws IOException {
        if (camelContext == null) {
            throw new IllegalArgumentException("CamelContext must be configured on " + this);
        }

        mapper = new DozerBeanMapper();

        setFileMatchPattern("glob:META-INF/services/dozer/*.xml");
        setProcessor(new Processor() {
            public void process(Path path) {
                addOrUpdateMapping(path);
            }

            public void onRemove(Path path) {
                removeMapping(path);
            }
        });

        super.init();

        LOG.info("Watching directory " + getRoot() + " for Dozer XML Mapping file changes");
    }

    @Override
    public void destroy() {
        if (mapper != null) {
            mapper.destroy();
        }

        super.destroy();
    }

    // Properties
    //-------------------------------------------------------------------------

    public CamelContext getCamelContext() {
        return camelContext;
    }

    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    // Implementation
    //-------------------------------------------------------------------------
    protected void addOrUpdateMapping(Path path) {
        try {
            String url = toUrlString(path);
            WatcherDozerTypeConverterLoader loader = loaders.get(url);
            if (loader == null) {
                addMapping(url);
            } else {
                updateMapping(url);
            }
        } catch (Exception e) {
            LOG.warn("Ignored path " + path + " due to: " + e, e);
        }
    }

    protected void removeMapping(Path path) {
        try {
            String url = toUrlString(path);

            LOG.info("Removing Dozer Mapping file " + url);
            WatcherDozerTypeConverterLoader loader = loaders.remove(url);
            if (url != null) {
                // remove by stopping the loader
                ServiceHelper.stopAndShutdownService(loader);
                camelContext.removeService(loader);
            }
        } catch (Exception e) {
            LOG.warn("Ignored path " + path + " due to: " + e, e);
        }
    }

    private void addMapping(String url) throws Exception {
        LOG.info("Adding Dozer Mapping file " + url);
        WatcherDozerTypeConverterLoader loader = new WatcherDozerTypeConverterLoader(camelContext, url, mapper);
        // add by adding the loader as a service
        camelContext.addService(loader);
        loaders.put(url, loader);
    }

    private void updateMapping(String url) {
        LOG.info("Updating Dozer Mapping file " + url);
        WatcherDozerTypeConverterLoader loader = loaders.get(url);
        if (url != null) {
            try {
                // update by restarting loader
                ServiceHelper.stopAndShutdownService(loader);
                camelContext.removeService(loader);
                ServiceHelper.startService(loader);
            } catch (Exception e) {
                LOG.warn("Error updating dozer mapping due to: " + e, e);
            }
        }
    }

    protected static String toUrlString(Path path) throws MalformedURLException {
        return path.toUri().toURL().toString();
    }

}
