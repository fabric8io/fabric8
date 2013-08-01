package org.fusesource.common.util;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;


public class XmlNamespaceFinder extends DefaultHandler {

    private Set<String> namespaces = new HashSet<String>();
    private boolean namespaceFound = false;
    private SAXParserFactory factory;


    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (!namespaceFound) {
            if (uri != null && uri.length() > 0) {
                namespaces.add(uri);
            }
        }
    }

    public Set<String> getNamespaces() {
        return namespaces;
    }

    public SAXParserFactory getFactory() {
        return factory;
    }

    public void setFactory(SAXParserFactory factory) {
        this.factory = factory;
    }

    public Set<String> parseContents(InputSource contents) throws IOException, ParserConfigurationException, SAXException {
        namespaces.clear();
        // Parse the file into we have what we need (or an error occurs).
        if (factory == null) {
            factory = SAXParserFactory.newInstance();
        }
        if (factory != null) {
            SAXParser parser = createParser(factory);
            // to support external entities specified as relative URIs (see bug 63298)
            contents.setSystemId("/"); //$NON-NLS-1$
            parser.parse(contents, this);
        }
        return namespaces;
    }

    protected final SAXParser createParser(SAXParserFactory parserFactory)
            throws ParserConfigurationException, SAXException {
        parserFactory.setNamespaceAware(true);
        final SAXParser parser = parserFactory.newSAXParser();
        final XMLReader reader = parser.getXMLReader();
        //reader.setProperty("http://xml.org/sax/properties/lexical-handler", this); //$NON-NLS-1$
        // disable DTD validation (bug 63625)
        try {
            //	be sure validation is "off" or the feature to ignore DTD's will not apply
            reader.setFeature("http://xml.org/sax/features/validation", false); //$NON-NLS-1$
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$
        } catch (SAXNotRecognizedException e) {
            // not a big deal if the parser does not recognize the features
        } catch (SAXNotSupportedException e) {
            // not a big deal if the parser does not support the features
        }
        return parser;
    }

}