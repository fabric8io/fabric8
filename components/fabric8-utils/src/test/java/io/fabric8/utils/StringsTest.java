/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.utils;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringsTest {

    @Test
    public void notEmptyTest() {
        
        Assert.assertTrue(Strings.notEmpty("Some text"));
        Assert.assertTrue(Strings.notEmpty(" "));
        Assert.assertFalse(Strings.notEmpty(""));
        Assert.assertFalse(Strings.notEmpty(null));
        
    }
    
    @Test
    public void nullIfEmptyTest() {
        
        Assert.assertNotNull(Strings.nullIfEmpty("Some text"));
        Assert.assertNotNull(Strings.nullIfEmpty(" "));
        Assert.assertNull(Strings.nullIfEmpty(""));
        Assert.assertNull(Strings.nullIfEmpty(null));        
        
    }
    
    @Test
    public void emptyIfNullTest() {
        
        assertEquals("Some text", Strings.emptyIfNull("Some text"));
        assertEquals(" ", Strings.emptyIfNull(" "));
        assertEquals("", Strings.emptyIfNull(""));
        assertEquals("", Strings.emptyIfNull(null));
        
    }
    
    @Test
    public void defaultIfEmptyTest() {
        
        assertEquals("Some text", Strings.defaultIfEmpty("Some text", "default"));
        assertEquals(" ", Strings.defaultIfEmpty(" ", "default"));
        assertEquals("default", Strings.defaultIfEmpty("", "default"));
        assertEquals("default", Strings.defaultIfEmpty(null, "default"));
        
    }
    
    @Test
    public void splitAsListTest() {
        
        List<String> list = Strings.splitAsList("a,b, c ,d", ",");
        
        Assert.assertTrue(list.size() == 4);
        
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals(" c ", list.get(2));
        assertEquals("d", list.get(3));
        
        list = Strings.splitAsList("a,b,c;,d,", ";");

        Assert.assertTrue(list.size() == 2);

        list = Strings.splitAsList("a,b,c;,d,", "~");

        Assert.assertTrue(list.size() == 1);

        list = Strings.splitAsList(null, "~");

        Assert.assertTrue(list.size() == 0);

        list = Strings.splitAsList("", null);

        Assert.assertTrue(list.size() == 0);

        try {
            Strings.splitAsList("a,b,c,d", null);
            Assert.fail("Expected NullPointerException but nothing happened");
        } catch(NullPointerException e) {
            //OK
        }
        
    }
    
    @Test
    public void splitAndTrimAsListTest() {
        
        List<String> list = Strings.splitAndTrimAsList("  a  ,  b  ,  c  ,  d  ", ",");
        
        Assert.assertTrue(list.size() == 4);

        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
        assertEquals("d", list.get(3));
        
        list = Strings.splitAndTrimAsList("a,b,c;,d,", ";");

        Assert.assertTrue(list.size() == 2);

        list = Strings.splitAndTrimAsList("a,b,c;,d,", "~");

        Assert.assertTrue(list.size() == 1);

        list = Strings.splitAndTrimAsList(null, "~");

        Assert.assertTrue(list.size() == 0);

        list = Strings.splitAndTrimAsList("", null);

        Assert.assertTrue(list.size() == 0);

        try {
            Strings.splitAndTrimAsList("a,b,c,d", null);
            Assert.fail("Expected NullPointerException but nothing happened");
        } catch(NullPointerException e) {
            //OK
        }
        
    }
    
    @Test
    public void joinTest() {
        
        assertEquals("a,b,3,d,1.2,f,null", Strings.join(",", "a", "b", 3, "d", 1.2d, "f", null));
        assertEquals("a b 3d1.2fnull", Strings.join("", "a", " b ", 3, "d", 1.2d, "f", null));
        assertEquals("", Strings.join(""));
        
    }
    
    @Test
    public void joinNotNullTest() {
        
        assertEquals("a,b,3,d,1.2,f", Strings.joinNotNull(",", "a", "b", 3, "d", 1.2d, "f", null));
        assertEquals("a b 3d1.2f", Strings.joinNotNull("", "a", " b ", 3, "d", 1.2d, "f", null));
        assertEquals("", Strings.joinNotNull(""));
        
    }
    
    @Test
    public void toStringTest() {
        
        assertEquals("foobar", Strings.toString("foobar"));
        assertEquals("12345", Strings.toString(12345));
        assertEquals("null", Strings.toString(null));
        
    }
    
    @Test
    public void unquoteTest() {
        
        assertEquals("foobar", Strings.unquote("\"foobar\""));
        assertEquals("\"foobar\"", Strings.unquote("\"\"foobar\"\""));
        assertEquals("'foobar'", Strings.unquote("'foobar'"));
        Assert.assertNull(Strings.unquote(null));
        
    }
    
    @Test
    public void isNullOrBlankTest() {
        
        Assert.assertTrue(Strings.isNullOrBlank(null));
        Assert.assertTrue(Strings.isNullOrBlank(""));
        Assert.assertTrue(Strings.isNullOrBlank(" "));
        Assert.assertTrue(Strings.isNullOrBlank("   "));
        Assert.assertFalse(Strings.isNullOrBlank("foobar"));       
        
    }
    
    @Test
    public void isNotBlankTest() {
        
        Assert.assertFalse(Strings.isNotBlank(null));
        Assert.assertFalse(Strings.isNotBlank(""));
        Assert.assertFalse(Strings.isNotBlank(" "));
        Assert.assertFalse(Strings.isNotBlank("   "));
        Assert.assertTrue(Strings.isNotBlank("foobar"));      
        
    }
    
    @Test
    public void parseDelimitedStringTest() {
        
        List<String> list = Strings.parseDelimitedString("a,b,3, d ,some space ,f", ",");

        Assert.assertTrue(list.size() == 6);

        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("3", list.get(2));
        assertEquals("d", list.get(3));
        assertEquals("some space", list.get(4));
        assertEquals("f", list.get(5));

        list = Strings.parseDelimitedString("a,b,3, d ,some space ,f", ",", false);

        Assert.assertTrue(list.size() == 6);

        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("3", list.get(2));
        assertEquals(" d ", list.get(3));
        assertEquals("some space ", list.get(4));
        assertEquals("f", list.get(5));

    }


    @Test
    public void testCamelCase() throws Exception {
        assertEquals("fooBarWhatnot", Strings.convertToCamelCase("foo-bar-whatnot", "-"));
        assertEquals("fooBarWhatnot", Strings.convertToCamelCase("foo--bar-whatnot", "-"));
    }


    @Test
    public void testReplaceAllWithoutRegex() throws Exception {
        assertEquals("bar-123-bar-bar", Strings.replaceAllWithoutRegex("foo-123-foo-foo", "foo","bar"));
        assertEquals("-barbar-", Strings.replaceAllWithoutRegex("-foofoo-", "foo","bar"));
        assertEquals("foo {{'{{'}} bar {{'{{'}} whatnot", Strings.replaceAllWithoutRegex("foo {{ bar {{ whatnot", "{{", "{{'{{'}}"));
        assertEquals("-bar-barbar-bar", Strings.replaceAllWithoutRegex("-foo-foofoo-foo", "foo","bar"));
    }


}
