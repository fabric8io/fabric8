/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.camel.c24io.spring;

import junit.framework.TestCase;
import org.apache.camel.RuntimeCamelException;
import org.junit.Test;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @version $Revision$
 */
public class SpringWithBadElementNameTest extends TestCase {
    @Test
    public void testBadElementName() throws Exception {
        try {
            AbstractXmlApplicationContext appContext = new ClassPathXmlApplicationContext("io/fabric8/camel/c24io/spring/badElementName.xml");
            appContext.start();
            fail("should have failed!");
        } catch (RuntimeCamelException e) {
            System.out.println("Caught expected: " + e);
            e.printStackTrace();
        }
    }
}