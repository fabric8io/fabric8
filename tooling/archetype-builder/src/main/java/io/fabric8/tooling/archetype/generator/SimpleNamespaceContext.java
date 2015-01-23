/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.tooling.archetype.generator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public class SimpleNamespaceContext implements NamespaceContext {

    private Map<String, String> prefix2Ns = new HashMap<String, String>();
    private Map<String, String> ns2Prefix = new HashMap<String, String>();

    public SimpleNamespaceContext() {
        prefix2Ns.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
        ns2Prefix.put(XMLConstants.XML_NS_URI, XMLConstants.XML_NS_PREFIX);
        prefix2Ns.put(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
        ns2Prefix.put(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE);
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return prefix2Ns.get(prefix);
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return ns2Prefix.get(namespaceURI);
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        return prefix2Ns.keySet().iterator();
    }

    /**
     * Registers prefix - namaspace URI mapping
     *
     * @param prefix
     * @param namespaceURI
     */
    public void registerMapping(String prefix, String namespaceURI) {
        prefix2Ns.put(prefix, namespaceURI);
        ns2Prefix.put(namespaceURI, prefix);
    }

}
