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
package io.fabric8.watcher.spring.context;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.fabric8.watcher.Paths;
import io.fabric8.watcher.Processor;
import io.fabric8.watcher.file.FileWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import static io.fabric8.watcher.PathHelper.toUrlString;

/**
 * A {@link io.fabric8.watcher.file.FileWatcher} which finds Spring Application Context XML files
 * using the given file patterns and loads them up and starts them automatically
 */
public class WatcherSpringContext extends FileWatcher {
    private static final transient Logger LOG = LoggerFactory.getLogger(WatcherSpringContext.class);

    public static final String SPRING_BEANS_NAMESPACE_URI = "http://www.springframework.org/schema/beans";

    private ConcurrentHashMap<String, FileSystemXmlApplicationContext> contextMap
            = new ConcurrentHashMap<String, FileSystemXmlApplicationContext>();
    private ApplicationContext parentApplicationContext;
    private AtomicBoolean closing = new AtomicBoolean(false);

    public WatcherSpringContext() {
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
        LOG.info("Watching directory " + getRoot() + " for Spring XML files to load");
    }

    public void destroy() {
        if (closing.compareAndSet(false, true)) {
            Set<Map.Entry<String, FileSystemXmlApplicationContext>> entries = contextMap.entrySet();
            for (Map.Entry<String, FileSystemXmlApplicationContext> entry : entries) {
                String url = entry.getKey();
                FileSystemXmlApplicationContext context = entry.getValue();
                closeContext(url, context);
            }
        }
        super.destroy();
    }

    // Properties
    //-------------------------------------------------------------------------

    public SortedSet<String> getApplicationContextPaths() {
        return new TreeSet<String>(contextMap.keySet());
    }

    public FileSystemXmlApplicationContext getApplicationContext(String path) {
        return contextMap.get(path);
    }

    public ApplicationContext getParentApplicationContext() {
        return parentApplicationContext;
    }

    public void setParentApplicationContext(ApplicationContext parentApplicationContext) {
        this.parentApplicationContext = parentApplicationContext;
    }

    // Implementation
    //-------------------------------------------------------------------------
    protected void addPath(Path path) {
        String url = null;
        try {
            url = toUrlString(path);
        } catch (MalformedURLException e) {
            LOG.warn("Ignored path " + path + " due to: " + e, e);
        }
        if (url != null) {
            FileSystemXmlApplicationContext context = contextMap.get(url);
            if (context != null) {
                try {
                    LOG.info("Refreshing context at path " + path + " context " + context);
                    context.refresh();
                } catch (Exception e) {
                    LOG.warn("Failed to refresh context at " + path + " context " + context + ". " + e, e);
                }
            } else {
                context = createContext(path, url);
                if (context != null) {
                    contextMap.put(url, context);
                    try {
                        LOG.info("Starting context at path " + path + " context " + context);
                        context.start();
                    } catch (Exception e) {
                        LOG.warn("Failed to start context at " + path + " context " + context + ". " + e, e);
                    }
                }
            }
        }
    }

    protected void removePath(Path path) {
        try {
            String url = toUrlString(path);
            FileSystemXmlApplicationContext context = contextMap.remove(url);
            closeContext(url, context);
        } catch (MalformedURLException e) {
            LOG.warn("Ignored path " + path + " due to: " + e, e);
        }
    }

    protected void closeContext(String url, FileSystemXmlApplicationContext context) {
        if (context != null) {
            try {
                LOG.info("Closing context at path " + url + " context " + context);
                context.close();
            } catch (Exception e) {
                LOG.info("Failed to close at " + url + " context " + context + ". " + e, e);
            }
        }
    }

    protected FileSystemXmlApplicationContext createContext(Path path, String url) {
        if (!Paths.hasNamespace(path, SPRING_BEANS_NAMESPACE_URI)) {
            LOG.info("Ignoring XML file " + path + " which is not a spring XML");
            return null;
        }
        String[] locations = {url};
        if (parentApplicationContext != null) {
            return new FileSystemXmlApplicationContext(locations, true, parentApplicationContext);
        } else {
            return new FileSystemXmlApplicationContext(locations, true);
        }
    }

}
