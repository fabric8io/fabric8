package io.fabric8.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PropertiesHelperTest {

    private Properties properties;
    
    @Before
    public void init() {
        
        properties = new Properties();
        properties.put("STRING_VALUE", "1234567890");
        properties.put("INT_VALUE", 1234567890);
        properties.put("DOUBLE_VALUE", 1234567895.4d);
        properties.put("LONG_VALUE", 1234567891l);
        
    }
    
    
    @Test
    public void getLongTest() {
        
        Assert.assertTrue(PropertiesHelper.getLong(properties, "STRING_VALUE", 1l) == 1234567890l);
        Assert.assertTrue(PropertiesHelper.getLong(properties, "LONG_VALUE", 1l) == 1234567891l);
        Assert.assertTrue(PropertiesHelper.getLong(properties, "INT_VALUE", 1l) == 1234567890l);
        Assert.assertTrue(PropertiesHelper.getLong(properties, "DOUBLE_VALUE", 1l) == 1234567895d);
        Assert.assertTrue(PropertiesHelper.getLong(properties, "NO_SUCH_VALUE", 1234567892l) == 1234567892l);
        Assert.assertTrue(PropertiesHelper.getLong(properties, "NO_SUCH_VALUE", null) == null);
        
    }

    @Test
    public void getLongValueTest() {
        
        Assert.assertTrue(PropertiesHelper.getLongValue(properties, "STRING_VALUE", 1l) == 1234567890l);
        Assert.assertTrue(PropertiesHelper.getLongValue(properties, "LONG_VALUE", 1l) == 1234567891l);
        Assert.assertTrue(PropertiesHelper.getLongValue(properties, "INT_VALUE", 1l) == 1234567890l);
        Assert.assertTrue(PropertiesHelper.getLongValue(properties, "DOUBLE_VALUE", 1l) == 1234567895d);
        Assert.assertTrue(PropertiesHelper.getLongValue(properties, "NO_SUCH_VALUE", 1234567892l) == 1234567892l);
        
    }
    
    @Test
    public void getLongValueTest2() {
        
        final Map<String, String> map = new HashMap<String, String>();
        map.put("STRING_VALUE", "1234567890");
        
        Assert.assertTrue(PropertiesHelper.getLongValue(map, "STRING_VALUE", 1l) == 1234567890l);
        Assert.assertTrue(PropertiesHelper.getLongValue(map, "NO_SUCH_VALUE", 1234567892l) == 1234567892l);
        
    }

    
}
