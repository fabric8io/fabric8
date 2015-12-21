/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.camel.commands.project.helper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Vetoed;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.CatalogHelper;
import org.apache.camel.catalog.DefaultCamelCatalog;

public class CamelCatalogService {

    private CamelCatalog instance;

    @Produces
    public CamelCatalog createCamelCatalog() {
        if (instance == null) {
            instance = new CachedCamelCatalog();
        }
        return instance;
    }

    /**
     * A cached {@link CamelCatalog} that loads the resources into memory on-startup
     */
    @Vetoed // must be inner class as weld will scan it and manage it so we have 2 CamelCatalog beans and it fails to start up
    private static class CachedCamelCatalog extends DefaultCamelCatalog {

        // TODO: currently only what camel-forge is using (there is also the EIP model to be cached)

        private List<String> findComponentNames;
        private List<String> findDataFormatNames;
        private List<String> findLanguageNames;
        private Map<String, String> componentJSonSchema = new HashMap<>();
        private Map<String, String> dataFormatJSonSchema = new HashMap<>();
        private Map<String, String> languageJSonSchema = new HashMap<>();
        private Set<String> findComponentLabels;
        private Set<String> findDataFormatLabels;
        private Set<String> findLanguageLabels;

        public CachedCamelCatalog() {
            // warm up the cache
            findComponentNames();
            findDataFormatNames();
            findLanguageNames();
            findComponentLabels();
            findDataFormatLabels();
            findLanguageLabels();

            for (String scheme : findComponentNames) {
                componentJSonSchema(scheme);
            }
            for (String name : findDataFormatNames) {
                dataFormatJSonSchema(name);
            }
            for (String name : findLanguageNames) {
                languageJSonSchema(name);
            }
        }

        @Override
        public List<String> findComponentNames() {
            if (findComponentNames == null) {
                findComponentNames = super.findComponentNames();
                // special for activemq, and we need to re-sort after adding
                findComponentNames.add("activemq");
                Collections.sort(findComponentNames);
            }
            return findComponentNames;
        }

        @Override
        public List<String> findDataFormatNames() {
            if (findDataFormatNames == null) {
                findDataFormatNames = super.findDataFormatNames();
            }
            return findDataFormatNames;
        }

        @Override
        public List<String> findLanguageNames() {
            if (findLanguageNames == null) {
                findLanguageNames = super.findLanguageNames();
            }
            return findLanguageNames;
        }

        @Override
        public String componentJSonSchema(String name) {
            String answer = componentJSonSchema.get(name);
            if (answer == null) {
                // special for activemq to load from activemq-camel classpath
                if ("activemq".equals(name)) {
                    String file = "org/apache/activemq/camel/component/activemq.json";
                    InputStream is = DefaultCamelCatalog.class.getClassLoader().getResourceAsStream(file);
                    if (is != null) {
                        try {
                            answer = CatalogHelper.loadText(is);
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                } else {
                    answer = super.componentJSonSchema(name);
                }
            }
            if (answer != null) {
                componentJSonSchema.put(name, answer);
            }
            return answer;
        }

        @Override
        public String dataFormatJSonSchema(String name) {
            String answer = dataFormatJSonSchema.get(name);
            if (answer == null) {
                answer = super.dataFormatJSonSchema(name);
                dataFormatJSonSchema.put(name, answer);
            }
            return answer;
        }

        @Override
        public String languageJSonSchema(String name) {
            String answer = languageJSonSchema.get(name);
            if (answer == null) {
                answer = super.languageJSonSchema(name);
                languageJSonSchema.put(name, answer);
            }
            return answer;
        }

        @Override
        public Set<String> findComponentLabels() {
            if (findComponentLabels == null) {
                findComponentLabels = super.findComponentLabels();
            }
            return findComponentLabels;
        }

        @Override
        public Set<String> findDataFormatLabels() {
            if (findDataFormatLabels == null) {
                findDataFormatLabels = super.findDataFormatLabels();
            }
            return findDataFormatLabels;
        }

        @Override
        public Set<String> findLanguageLabels() {
            if (findLanguageLabels == null) {
                findLanguageLabels = super.findLanguageLabels();
            }
            return findLanguageLabels;
        }
    }
}
