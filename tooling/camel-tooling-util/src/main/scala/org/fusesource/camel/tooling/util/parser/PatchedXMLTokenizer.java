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

import de.pdark.decentxml.Token;
import de.pdark.decentxml.XMLParseException;
import de.pdark.decentxml.XMLSource;
import de.pdark.decentxml.XMLTokenizer;
import de.pdark.decentxml.validation.CharValidator;

/**
 * Patch for <a href="https://code.google.com/p/decentxml/issues/detail?id=5">issue #5</a>
 */
public class PatchedXMLTokenizer extends XMLTokenizer {
    private CharValidator charValidator = new CharValidator();

    public PatchedXMLTokenizer(XMLSource source) {
        super(source);
    }


    /**
     * Read the attribute of an element.
     * <p/>
     * <p>The resulting token will contain the name, "=" plus the
     * quotes and the value.
     */
    protected void parseAttribute(Token token) {
        // TODO this is a monster copy/paste - would be nicer to fix in the underlying library! :)
        token.setType(Type.ATTRIBUTE);

        parseName("attribute");

        if (pos == token.getStartOffset())
            throw new XMLParseException("Expected attribute name", source, pos);

        skipWhiteSpace();
        expect('=');
        skipWhiteSpace();

        char c = 0;
        if (pos < source.length())
            c = source.charAt(pos);
        if (c != '\'' && c != '"')
            throw new XMLParseException("Expected single or double quotes", source, pos);

        char endChar = c;
        boolean insideEntity = false;
        int errorPos = pos;

        while (true) {
            pos++;
            if (pos >= source.length()) {
                int i = Math.min(20, source.length() - token.getStartOffset());
                throw new XMLParseException("Missing end quote (" + endChar + ") of attribute: "
                        + lookAheadForErrorMessage(null, token.getStartOffset(), i), token);
            }

            c = source.charAt(pos);
            if (c == endChar)
                break;

            if (c == '<')
                throw new XMLParseException("Illegal character in attribute value: '" + c + "'", source, pos);

            if (c == '&') {
                insideEntity = true;
                errorPos = pos;
            } else if (c == ';') {
                verifyEntity(errorPos, pos + 1);
                insideEntity = false;
            } else {
                String msg = charValidator.isValid(source, pos);
                if (msg != null)
                    throw new XMLParseException("Illegal character found in attribute value. " + msg, source, pos);

                skipChar(c);
                pos--;
            }
        }

        if (insideEntity) {
            throw new XMLParseException("Missing ';' after '&': " + lookAheadForErrorMessage(null, errorPos, 20), source, errorPos);
        }

        // Skip end-char
        pos++;
    }
}
