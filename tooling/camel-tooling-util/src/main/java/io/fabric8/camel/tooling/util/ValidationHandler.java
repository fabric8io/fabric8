/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.camel.tooling.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import static io.fabric8.camel.tooling.util.CamelNamespaces.*;

public class ValidationHandler implements ErrorHandler {

    List<SAXParseException> warnings = new LinkedList<SAXParseException>();
    List<SAXParseException> errors = new LinkedList<SAXParseException>();
    List<SAXParseException> fatalErrors = new LinkedList<SAXParseException>();

    @Override
    public void warning(SAXParseException exception) throws SAXException {
        warnings.add(exception);
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
        errors.add(exception);
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        fatalErrors.add(exception);
    }

    public List<SAXParseException> getWarnings() {
        return warnings;
    }

    public List<SAXParseException> getErrors() {
        return errors;
    }

    public List<SAXParseException> getFatalErrors() {
        return fatalErrors;
    }

    public String userMessage() {
        StringWriter sw = new StringWriter();
        for (SAXParseException ex : errors) {
            sw.append(", ").append(ex.getMessage());
        }
        for (SAXParseException ex : fatalErrors) {
            sw.append(", ").append(ex.getMessage());
        }
        String text = sw.toString();
        if (text.length() > 2) {
            text = text.substring(2); // first comma
        }
        int idx = text.indexOf(":");
        if (text.startsWith("cvc-complex-type") && idx > 0) {
            text = text.substring(idx + 1).trim();
        }
        for (String uri: camelNamespaces) {
            text = text.replaceAll("\"" + uri + "\":", "");
        }
        return text;
    }

    public void validate(Document doc) throws IOException, SAXException {
        Validator validator = camelSchemas().newValidator();
        validator.setErrorHandler(this);

        validate(validator, doc.getRootElement());
    }

    private void validate(Validator validator, Element e) throws IOException, SAXException {
        String uri = getNamespaceURI(e);
        if (uri != null && Arrays.asList(camelNamespaces).contains(uri)) {
            String text = nodeWithNamespacesToText(e, e);
            validator.validate(new StreamSource(new StringReader(text)));
        } else {
            for (Node node: e.getNodes()) {
                if (node instanceof Element) {
                    validate(validator, (Element) node);
                }
            }
        }
    }

    public boolean hasErrors() {
        return !errors.isEmpty() || !fatalErrors.isEmpty();
    }

    public void checkForErrors() throws ValidationException {
        if (hasErrors()) {
            StringWriter sw = new StringWriter();
            for (SAXParseException ex : errors) {
                sw.append(", ").append(ex.getMessage());
            }
            for (SAXParseException ex : fatalErrors) {
                sw.append(", ").append(ex.getMessage());
            }
            String text = sw.toString();
            if (text.length() > 2) {
                text = text.substring(2); // first comma
            }
            throw new ValidationException("Validation failed: " + text, userMessage(), errors, fatalErrors, warnings);
        }
    }

}
