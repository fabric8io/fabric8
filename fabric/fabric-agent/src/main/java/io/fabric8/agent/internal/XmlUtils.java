/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Utils class to manipulate XML document in a thread safe way.
 */
public class XmlUtils {

    private static final ThreadLocal<DocumentBuilderFactory> DOCUMENT_BUILDER_FACTORY = new ThreadLocal<DocumentBuilderFactory>();
    private static final ThreadLocal<TransformerFactory> TRANSFORMER_FACTORY = new ThreadLocal<TransformerFactory>();
    private static final ThreadLocal<SAXParserFactory> SAX_PARSER_FACTORY = new ThreadLocal<SAXParserFactory>();

    public static Document parse(String uri) throws TransformerException, IOException, SAXException, ParserConfigurationException {
        DocumentBuilder db = documentBuilder();
        try {
            return db.parse(uri);
        } finally {
            db.reset();
        }
    }

    public static Document parse(InputStream stream) throws TransformerException, IOException, SAXException, ParserConfigurationException {
        DocumentBuilder db = documentBuilder();
        try {
            return db.parse(stream);
        } finally {
            db.reset();
        }
    }

    public static Document parse(File f) throws TransformerException, IOException, SAXException, ParserConfigurationException {
        DocumentBuilder db = documentBuilder();
        try {
            return db.parse(f);
        } finally {
            db.reset();
        }
    }

    public static Document parse(File f, ErrorHandler errorHandler) throws TransformerException, IOException, SAXException, ParserConfigurationException {
        DocumentBuilder db = documentBuilder();
        db.setErrorHandler(errorHandler);
        try {
            return db.parse(f);
        } finally {
            db.reset();
        }
    }

    public static void transform(Source xmlSource, Result outputTarget) throws TransformerException {
        Transformer t = transformer();
        try {
            t.transform(xmlSource, outputTarget);
        } finally {
            t.reset();
        }
    }

    public static void transform(Source xsltSource, Source xmlSource, Result outputTarget) throws TransformerException {
        Transformer t = transformer(xsltSource);
        try {
            t.transform(xmlSource, outputTarget);
        } finally {
            t.reset();
        }
    }

    public static XMLReader xmlReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAX_PARSER_FACTORY.get();
        if (spf == null) {
            spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            SAX_PARSER_FACTORY.set(spf);
        }
        return spf.newSAXParser().getXMLReader();
    }

    public static DocumentBuilder documentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DOCUMENT_BUILDER_FACTORY.get();
        if (dbf == null) {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DOCUMENT_BUILDER_FACTORY.set(dbf);
        }
        return dbf.newDocumentBuilder();
    }

    public static Transformer transformer() throws TransformerConfigurationException {
        TransformerFactory tf = TRANSFORMER_FACTORY.get();
        if (tf == null) {
            tf = TransformerFactory.newInstance();
            TRANSFORMER_FACTORY.set(tf);
        }
        return tf.newTransformer();
    }

    private static Transformer transformer(Source xsltSource) throws TransformerConfigurationException {
        TransformerFactory tf = TRANSFORMER_FACTORY.get();
        if (tf == null) {
            tf = TransformerFactory.newInstance();
            TRANSFORMER_FACTORY.set(tf);
        }
        return tf.newTransformer(xsltSource);
    }

}
