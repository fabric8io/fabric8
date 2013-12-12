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
package io.fabric8.watcher;

import org.fusesource.common.util.XmlHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Helper functions for working with {@link Path}
 */
public class Paths {
    private static final transient Logger LOG = LoggerFactory.getLogger(Paths.class);

    /**
     * Returns true if the file can be parsed as XML and it contains one of the given namespace URs
     */
    public static boolean hasNamespace(Path file, String... namespaceURis) {
        try {
            return XmlHelper.hasNamespace(getNamespaces(file), namespaceURis);
        } catch (Exception e) {
            LOG.warn("Failed to parse XML " + file + ". " + e, e);
            return false;
        }
    }

    /**
     * Returns the namespace URIs found in the given XML file
     */
    public static Set<String> getNamespaces(Path path)
            throws IOException, ParserConfigurationException, SAXException {
        return XmlHelper.getNamespaces(path.toUri().toURL().openStream());
    }
}
