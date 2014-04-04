package io.fabric8.utils;

import org.junit.Assert;
import org.junit.Test;

public class Base64EncoderTest {

    @Test
    public void encodeTest() {
        
        //RFC4648 test vectors
        Assert.assertEquals("", Base64Encoder.encode(""));
        Assert.assertEquals("Zg==", Base64Encoder.encode("f"));
        Assert.assertEquals("Zm8=", Base64Encoder.encode("fo"));
        Assert.assertEquals("Zm9v", Base64Encoder.encode("foo"));
        Assert.assertEquals("Zm9vYg==", Base64Encoder.encode("foob"));
        Assert.assertEquals("Zm9vYmE=", Base64Encoder.encode("fooba"));
        Assert.assertEquals("Zm9vYmFy", Base64Encoder.encode("foobar"));
        
    }

    @Test
    public void decodeTest() {
        
        //RFC4648 test vectors
        Assert.assertEquals("", Base64Encoder.decode(""));
        Assert.assertEquals("f", Base64Encoder.decode("Zg=="));
        Assert.assertEquals("fo", Base64Encoder.decode("Zm8="));
        Assert.assertEquals("foo", Base64Encoder.decode("Zm9v"));
        Assert.assertEquals("foob", Base64Encoder.decode("Zm9vYg=="));
        Assert.assertEquals("fooba", Base64Encoder.decode("Zm9vYmE="));
        Assert.assertEquals("foobar", Base64Encoder.decode("Zm9vYmFy"));
        
    }

}
