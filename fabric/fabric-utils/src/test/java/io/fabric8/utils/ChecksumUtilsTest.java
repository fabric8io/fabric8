package io.fabric8.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

public class ChecksumUtilsTest {

    @Test
    public void checksumTest() throws IOException {
        
        //well known CRC32 checksums
        InputStream stream = new ByteArrayInputStream("The quick brown fox jumps over the lazy dog".getBytes("UTF-8"));
        
        Assert.assertEquals(0x414fa339, ChecksumUtils.checksum(stream));

        stream = new ByteArrayInputStream("".getBytes("UTF-8"));
        
        Assert.assertEquals(0x0, ChecksumUtils.checksum(stream));
        
        try {
            ChecksumUtils.checksum(null);
            Assert.fail("Expected NullPointerException but nothing happened");
        } catch(NullPointerException e) {
            
        }

    }
    
}
