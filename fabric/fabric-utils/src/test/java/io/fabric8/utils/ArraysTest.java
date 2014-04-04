package io.fabric8.utils;

import org.junit.Assert;
import org.junit.Test;

public class ArraysTest {

    @Test
    public void joinTest() {
        
        Assert.assertEquals("A,B,C,D,E", Arrays.join(",", "A", "B", "C", "D", "E"));
        Assert.assertEquals("1:2:3:4:5", Arrays.join(":", 1, 2, 3, 4, 5));
        Assert.assertEquals("1.1;2;C;4;5.5", Arrays.join(";", 1.1d, 2, "C", 4, 5.5f));

    }
    
}
