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
package org.fusesource.fabric.dozer.file;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.CamelContext;
import org.apache.camel.converter.dozer.DozerTypeConverter;
import org.apache.camel.converter.dozer.DozerTypeConverterLoader;
import org.apache.camel.spi.ClassResolver;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.camel.util.ResourceHelper;
import org.dozer.DozerBeanMapper;
import org.dozer.classmap.ClassMap;
import org.dozer.classmap.MappingFileData;
import org.dozer.config.BeanContainer;
import org.dozer.loader.xml.MappingFileReader;
import org.dozer.loader.xml.XMLParserFactory;
import org.dozer.util.DozerClassLoader;
import org.fusesource.fabric.watcher.Processor;
import org.fusesource.fabric.watcher.file.FileWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link FileWatcher} which watches for Dozer mapping XML files and adds/removes
 * the mappings from a {@link org.apache.camel.CamelContext}s {@link org.apache.camel.spi.TypeConverterRegistry}
 * whenever files is added, updated or removed.
 */
public class FileWatcherMapping extends FileWatcher {
    private static final transient Logger LOG = LoggerFactory.getLogger(FileWatcherMapping.class);

    // TODO: Wait for next perfects build so we can reuse camel-dozer

    private ConcurrentHashMap<String, Path> urlMap = new ConcurrentHashMap<String, Path>();
    private CamelContext camelContext;
    private DozerTypeConverterLoader loader;

    public FileWatcherMapping() {
        setFileMatchPattern("glob:META-INF/services/dozer/*.xml");
        setProcessor(new Processor() {
            public void process(Path path) {
                addOrUpdateMapping(path);
            }

            public void onRemove(Path path) {
                removeMapping(path);
            }
        });
    }

    public void init() throws IOException {
        super.init();

        if (camelContext == null) {
            throw new IllegalArgumentException("CamelContext must be configured on " + this);
        }

        loader = new DozerTypeConverterLoader(camelContext);

        CamelToDozerClassResolverAdapter adapter = new CamelToDozerClassResolverAdapter(camelContext);
        BeanContainer.getInstance().setClassLoader(adapter);

        LOG.info("Watching directory " + getRoot() + " for Dozer XML Mapping file changes");
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
            Path old = urlMap.put(url, path);
            if (old == null) {
                addMapping(path);
            } else {
                updateMapping(path);
            }
        } catch (Exception e) {
            LOG.warn("Ignored path " + path + " due to: " + e, e);
        }
    }

    private void addMapping(Path path) throws Exception {
        LOG.info("Adding Dozer Mapping file " + path);

        List<String> mappingFiles = new ArrayList<String>(1);
        mappingFiles.add(toUrlString(path));

        DozerBeanMapper dozer = new DozerBeanMapper();
        dozer.setMappingFiles(mappingFiles);

        List<ClassMap> maps = loadMappings(camelContext, dozer);
        registerClassMaps(camelContext.getTypeConverterRegistry(), dozer, maps);
    }

    private void updateMapping(Path path) {
        LOG.info("Updating Dozer Mapping file " + path);
        // todo impl
    }

    protected void removeMapping(Path path) {
        LOG.info("Removing Dozer Mapping file " + path);
        try {
            String url = toUrlString(path);
            Path old = urlMap.remove(url);
            if (old != null) {
                // todo impl
            }
        } catch (Exception e) {
            LOG.warn("Ignored path " + path + " due to: " + e, e);
        }
    }

    protected String toUrlString(Path path) throws MalformedURLException {
        return path.toUri().toURL().toString();
    }


    private List<ClassMap> loadMappings(CamelContext camelContext, DozerBeanMapper mapper) throws FileNotFoundException, MalformedURLException {
        List<ClassMap> answer = new ArrayList<ClassMap>();

        // load the class map using the class resolver so we can load from classpath in OSGi
        MappingFileReader reader = new MappingFileReader(XMLParserFactory.getInstance());
        List<String> mappingFiles = mapper.getMappingFiles();
        if (mappingFiles == null) {
            return Collections.emptyList();
        }

        for (String name : mappingFiles) {
            URL url = ResourceHelper.resolveMandatoryResourceAsUrl(camelContext.getClassResolver(), name);
            if (url != null) {
                MappingFileData data = reader.read(url);
                answer.addAll(data.getClassMaps());
            }
        }

        return answer;
    }

    private void registerClassMaps(TypeConverterRegistry registry, DozerBeanMapper dozer, List<ClassMap> all) {
        DozerTypeConverter converter = new DozerTypeConverter(dozer);
        for (ClassMap map : all) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Added {} -> {} as type converter to: {}", new Object[]{map.getSrcClassName(), map.getDestClassName(), registry});
            }
            registry.addTypeConverter(map.getSrcClassToMap(), map.getDestClassToMap(), converter);
            registry.addTypeConverter(map.getDestClassToMap(), map.getSrcClassToMap(), converter);
        }
    }

    private static final class CamelToDozerClassResolverAdapter implements DozerClassLoader {

        private final ClassResolver classResolver;

        private CamelToDozerClassResolverAdapter(CamelContext camelContext) {
            classResolver = camelContext.getClassResolver();
        }

        public Class<?> loadClass(String s) {
            return classResolver.resolveClass(s);
        }

        public URL loadResource(String s) {
            URL url = null;
            try {
                url = ResourceHelper.resolveMandatoryResourceAsUrl(classResolver, s);
            } catch (Exception e) {
                // ignore
            }
            if (url == null) {
                // using the classloader of DozerClassLoader as a fallback
                url = DozerClassLoader.class.getClassLoader().getResource(s);
            }
            return url;
        }
    }

}
