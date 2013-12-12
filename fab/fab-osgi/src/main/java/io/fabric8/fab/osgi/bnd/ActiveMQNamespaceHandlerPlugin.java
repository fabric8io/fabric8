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
import aQute.lib.spring.XMLType;
import aQute.lib.spring.XMLTypeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Bnd plugin to add import for elements in the ActiveMQ Spring namespace
 */
public class ActiveMQNamespaceHandlerPlugin extends XMLTypeProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQNamespaceHandlerPlugin.class);

    @Override
    protected List<XMLType> getTypes(Analyzer analyzer) throws Exception {
        List<XMLType> types = new ArrayList<XMLType>();

        try {
            String header = analyzer.getProperty("Spring-Context", "META-INF/spring");
            process(types,"activemq.xsl", header, ".*\\.xml");
        } catch (Exception e) {
            LOGGER.warn("Error while adding bundle imports for ActiveMQ Spring namespace elements", e);
        }

        return types;
    }

}
