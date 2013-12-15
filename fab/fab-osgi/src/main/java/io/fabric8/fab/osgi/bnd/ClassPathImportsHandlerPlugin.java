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
package io.fabric8.fab.osgi.bnd;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Processor;
import aQute.lib.spring.XMLType;
import aQute.lib.spring.XMLTypeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * Bnd plugin to add bundles imports for Spring resources that are imported through a classpath URL
 */
public class ClassPathImportsHandlerPlugin extends XMLTypeProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassPathImportsHandlerPlugin.class);

    @Override
    protected List<XMLType> getTypes(Analyzer analyzer) throws Exception {
        List<XMLType> types = new ArrayList<XMLType>();

        try {
            String header = analyzer.getProperty("Spring-Context", "META-INF/spring");
            process(types,"imports.xsl", header, ".*\\.xml");
        } catch (Exception e) {
            LOGGER.warn("Error while adding bundle imports for Spring <import/> elements", e);
        }

        return types;
    }

    protected void process(List<XMLType> types, String resource, String paths, String pattern) throws Exception {
        Map<String,Map<String,String>> map = Processor.parseHeader(paths, null);
        for ( String path : map.keySet() ) {
            types.add( new ClassPathImportsXmlType( getClass().getResource(resource), path, pattern ));
        }
    }

    private class ClassPathImportsXmlType extends XMLType {

        public ClassPathImportsXmlType(URL source, String root, String paths) throws Exception {
            super(source, root, paths);
        }

        public Set<String> analyze(InputStream in) throws Exception {
            Set<String> refers = super.analyze(in);
            // the XSL returns a set of classpath: URLs, we should translate those to the corresponding package names
            return transformToJavaNotation(refers);
        }

        private Set<String> transformToJavaNotation(Set<String> refers) {
            Set<String> result = new HashSet<String>();
            for (String refer : refers) {
                try {
                    if (refer.startsWith("classpath:")) {
                        String folder = refer.substring(10, refer.lastIndexOf("/"));
                        result.add(folder.replaceAll("/", "."));
                    }

                } catch (Exception e) {
                    LOGGER.warn(String.format("Unable to add bundle import to match <import resource='%s'/>", refer), e);
                }
            }
            return result;
        }
    }
}

