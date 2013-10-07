package org.fusesource.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * A helper method to get the namespaces on an XML file
 */
public class XmlHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(XmlHelper.class);
    private static SAXParserFactory factory;

    /**
     * Returns true if the file can be parsed as XML and it contains one of the given namespace URs
     */
    public static boolean hasNamespace(File file, String... namespaceURis) {
        try {
            return hasNamespace(getNamespaces(file), namespaceURis);
        } catch (Exception e) {
            LOG.warn("Failed to parse XML " + file + ". " + e, e);
            return false;
        }
    }

    /**
     * Returns true if the file can be parsed as XML and it contains one of the given namespace URs
     */
    public static boolean hasNamespace(InputStream file, String... namespaceURis) {
        try {
            return hasNamespace(getNamespaces(file), namespaceURis);
        } catch (Exception e) {
            LOG.warn("Failed to parse XML " + file + ". " + e, e);
            return false;
        }
    }

    /**
     * Returns true if the file can be parsed as XML and it contains one of the given namespace URs
     */
    public static boolean hasNamespace(InputSource file, String... namespaceURis) {
        try {
            return hasNamespace(getNamespaces(file), namespaceURis);
        } catch (Exception e) {
            LOG.warn("Failed to parse XML " + file + ". " + e, e);
            return false;
        }
    }

    /**
     * Returns true if the set of namespaces containers one of the given given namespace URIs
     * @param namespaces
     * @param namespaceURis
     * @return
     */
    public static boolean hasNamespace(Set<String> namespaces, String... namespaceURis) {
        if (namespaces != null) {
            for (String namespaceURi : namespaceURis) {
                if (namespaces.contains(namespaceURi)) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Returns the namespace URIs found in the given XML file
     */
    public static Set<String> getNamespaces(File file) throws ParserConfigurationException, SAXException, IOException {
        return getNamespaces(new InputSource(new FileReader(file)));
    }

    /**
     * Returns the namespace URIs found in the given XML file
     */
    public static Set<String> getNamespaces(InputStream is)
            throws IOException, SAXException, ParserConfigurationException {
        return getNamespaces(new InputSource(is));
    }

    /**
     * Returns the namespace URIs found in the given XML file
     */
    public static Set<String> getNamespaces(InputSource source) throws ParserConfigurationException, SAXException, IOException {
        XmlNamespaceFinder finder = createNamespaceFinder();
        Set<String> answer = finder.parseContents(source);
        if (factory == null) {
            factory = finder.getFactory();
        }
        return answer;
    }

    public static SAXParserFactory getFactory() {
        return factory;
    }

    public static void setFactory(SAXParserFactory factory) {
        XmlHelper.factory = factory;
    }

    protected static XmlNamespaceFinder createNamespaceFinder() {
        XmlNamespaceFinder finder = new XmlNamespaceFinder();
        if (factory != null) {
            finder.setFactory(factory);
        }
        return finder;
    }

}
