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
package io.fabric8.dozer.file;

import org.apache.camel.TypeConverter;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.foo.NewCustomer;
import org.foo.OldCustomer;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

@Ignore("[FABRIC-623][7.4] Fix fabric watcher-dozer CamelSpringDozerTest")
public class CamelSpringDozerTest extends CamelSpringTestSupport {

    @Test
    public void testDozer() throws Exception {
        assertNotNull(context);

        TypeConverter tc = context.getTypeConverterRegistry().lookup(OldCustomer.class, NewCustomer.class);
        assertNotNull("Should have registered Dozer type converters", tc);

        OldCustomer old = new OldCustomer();
        old.setId(123);
        old.setName("Donald Duck");
        old.setStreet("Duckstreet 13");
        old.setZip("90210");

        String out = template.requestBody("direct:start", old, String.class);
        assertEquals("The new customer Donald Duck lives at Duckstreet 13 zip 90210", out);
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("io/fabric8/dozer/file/CamelSpringDozer-context.xml");
    }
}
