package io.fabric8.utils;



import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.junit.Assert;
import org.junit.Test;

public class FilesTest {

    @Test
    public void normalizePathTest() {

        Assert.assertEquals("/some/path", Files.normalizePath("\\some\\path", '\\', '/'));
        Assert.assertEquals("/some/path", Files.normalizePath("/some/path", '\\', '/'));
        Assert.assertEquals("\\some\\path", Files.normalizePath("/some/path", '/', '\\'));
        Assert.assertEquals("\\some\\path", Files.normalizePath("\\some\\path", '/', '\\'));
        
    }
    
    @Test
    public void toStringTest() throws IOException {
        
        final String testString = "This is a test string";
        
        final InputStream stream = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        
        Assert.assertEquals(testString, Files.toString(stream));
        stream.reset();
        Assert.assertEquals(testString, Files.toString(stream, Charset.forName("UTF-8")));
        
    }
    
}
