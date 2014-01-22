package io.fabric8.api.scr.support;

import org.junit.Test;
import static org.junit.Assert.*;
import static io.fabric8.api.scr.support.ConfigInjection.*;
public class ConfigInjectionTest {

    @Test
    public void testNormalizeName() {
        assertEquals(null, normalizePropertyName(null));
        assertEquals("", normalizePropertyName(""));
        assertEquals("", normalizePropertyName("."));
        assertEquals("t", normalizePropertyName("t"));
        assertEquals("test", normalizePropertyName("test"));
        assertEquals("Test", normalizePropertyName(".test"));
        assertEquals("test", normalizePropertyName("test."));
        assertEquals("Test", normalizePropertyName(".test."));
        assertEquals("myTest", normalizePropertyName("my.test"));
        assertEquals("my", normalizePropertyName("my."));
    }
}
