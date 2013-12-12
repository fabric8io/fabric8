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
package io.fabric8.jaxb.dynamic.watcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.fabric8.jaxb.dynamic.CompileResults;
import io.fabric8.jaxb.dynamic.CompileResultsHandler;
import io.fabric8.jaxb.dynamic.DefaultDynamicCompiler;
import io.fabric8.jaxb.dynamic.DynamicCompiler;
import io.fabric8.watcher.Processor;
import io.fabric8.watcher.file.FileWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.watcher.PathHelper.toUrlString;

/**
 * A {@link FileWatcher} which implements the {@link DynamicCompiler} API by
 * watching for XSD files and recompiling the JAXB context via XJC whenever a
 * new schema is added, updated or removed.
 */
public class FileWatcherDynamicCompiler extends FileWatcher implements DynamicCompiler {
    private static final transient Logger LOG = LoggerFactory.getLogger(FileWatcherDynamicCompiler.class);

    private CompileResults compileResults;
    private CompileResultsHandler handler;
    private ConcurrentHashMap<String, Path> urlMap = new ConcurrentHashMap<String, Path>();
    private AtomicBoolean compileScheduled = new AtomicBoolean(false);
    private long compileDelayMillis = 1000;
    private ClassLoader classLoader;

    public FileWatcherDynamicCompiler() {
        setFileMatchPattern("glob:**.xsd");
        setProcessor(new Processor() {
            public void process(Path path) {
                addCompilePath(path);
            }

            public void onRemove(Path path) {
                removeCompilePath(path);
            }
        });
    }

    public void setHandler(CompileResultsHandler handler) throws Exception {
        this.handler = handler;
        // lets pass in the first set of results if we've compiled before the
        // handler is registered
        if (handler != null && compileResults != null) {
            handler.onCompileResults(compileResults);
        }
    }

    public void init() throws IOException {
        super.init();
        LOG.info("Watching directory " + getRoot() + " for XML Schema files to dynamically compile");
    }

    // Properties
    //-------------------------------------------------------------------------


    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Returns the latest compiler results
     */
    public CompileResults getCompileResults() {
        return compileResults;
    }

    public long getCompileDelayMillis() {
        return compileDelayMillis;
    }

    public void setCompileDelayMillis(long compileDelayMillis) {
        this.compileDelayMillis = compileDelayMillis;
    }

    // Implementation
    //-------------------------------------------------------------------------
    protected void addCompilePath(Path path) {
        try {
            String url = toUrlString(path);
            Path old = urlMap.put(url, path);
            if (old == null) {
                scheduleRecompile();
            }
        } catch (MalformedURLException e) {
            LOG.warn("Ignored path " + path + " due to: " + e, e);
        }
    }

    protected void removeCompilePath(Path path) {
        try {
            String url = toUrlString(path);
            Path old = urlMap.remove(url);
            if (old != null) {
                scheduleRecompile();
            }
        } catch (MalformedURLException e) {
            LOG.warn("Ignored path " + path + " due to: " + e, e);
        }
    }

    protected void scheduleRecompile() {
        if (compileScheduled.compareAndSet(false, true)) {
            ExecutorService executor = getExecutor();
            Runnable command = new Runnable() {
                public void run() {
                    compileScheduled.set(false);
                    doCompile();
                }
            };

            // lets schedule if we can otherwise execute async
            if (executor instanceof ScheduledExecutorService) {
                ScheduledExecutorService scheduledExecutorService = (ScheduledExecutorService)executor;
                scheduledExecutorService.schedule(command, compileDelayMillis, TimeUnit.MILLISECONDS);
            } else {
                executor.execute(command);
            }
        }
    }

    protected void doCompile() {
        Set<String> urls = urlMap.keySet();
        LOG.info("Compilng XSD urls: " + urls);
        compileResults = DefaultDynamicCompiler.doCompile(classLoader, urls);
        if (handler != null) {
            handler.onCompileResults(compileResults);
        }
    }
}
