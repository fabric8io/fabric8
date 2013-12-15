package io.fabric8.utils;

import org.junit.Test;

import static io.fabric8.utils.PatchUtils.appendVersionRange;
import static io.fabric8.utils.PatchUtils.extractUrl;
import static io.fabric8.utils.PatchUtils.extractVersionRange;
import static org.junit.Assert.assertEquals;

/**
 * Test cases for {@link PatchUtils}
 */
public class PatchUtilsTest {

    @Test
    public void testExtractUrlAndRange() {
        String url = appendVersionRange("mvn:org.fusesource.test/test/1.0.0", "[1.0.0,1.1.0)");
        assertEquals("mvn:org.fusesource.test/test/1.0.0;range=[1.0.0,1.1.0)", url);
        doAssertExtract(url, "mvn:org.fusesource.test/test/1.0.0", "[1.0.0,1.1.0)");

        // ensure it also works if there's no version range
        doAssertExtract("mvn:org.fusesource.test/test/1.0.0",
                        "mvn:org.fusesource.test/test/1.0.0", null);
    }

    private void doAssertExtract(String override, String url, String range) {
        assertEquals("Should extract URL", url, extractUrl(override));
        assertEquals("Should extract version range", range, extractVersionRange(override));
    }

}
