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
package org.fusesource.camel.tooling.util.parser;

import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLSource;
import de.pdark.decentxml.XMLTokenizer;

/**
 * Patch for <a href="https://code.google.com/p/decentxml/issues/detail?id=5">issue #5</a>
 */
public class PatchedXMLParser extends XMLParser {
    public PatchedXMLParser() {
    }

    protected XMLTokenizer createTokenizer(XMLSource source) {
        return new PatchedXMLTokenizer(source);
    }
}
