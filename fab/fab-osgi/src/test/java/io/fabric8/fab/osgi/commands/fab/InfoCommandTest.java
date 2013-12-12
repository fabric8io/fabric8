package io.fabric8.fab.osgi.commands.fab;

import org.junit.Test;

import java.util.List;

import static io.fabric8.fab.osgi.commands.fab.InfoCommand.getClassPathElements;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests cases for the helper method in {@link InfoCommand}
 */
public class InfoCommandTest {

    private static final String BUNDLE_CLASSPATH =
            ".,org.apache.servicemix.bundles.org.apache.servicemix.bundles.commons-httpclient.jar," +
            "commons-logging.commons-logging.jar," +
            "org.apache.servicemix.bundles.org.apache.servicemix.bundles.commons-codec.jar";

    @Test
    public void testGetClassPathElements() {
        List<String> elements = getClassPathElements(BUNDLE_CLASSPATH);
        assertEquals(3, elements.size());
        assertTrue(elements.contains("org.apache.servicemix.bundles.org.apache.servicemix.bundles.commons-httpclient.jar"));
        assertTrue(elements.contains("commons-logging.commons-logging.jar"));
        assertTrue(elements.contains("org.apache.servicemix.bundles.org.apache.servicemix.bundles.commons-codec.jar"));
    }



}
