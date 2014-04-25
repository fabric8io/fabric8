/**
 *  Copyright 2005-2014 Red Hat, Inc.
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

package io.fabric8.example.drools;

import java.util.Collection;

import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.junit4.CamelSpringTestSupport;
import org.drools.core.runtime.impl.ExecutionResultImpl;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CamelContextXmlTest extends CamelSpringTestSupport {

    // templates to send to input endpoints
    @Produce(uri = "direct://ruleOnBody")
    protected ProducerTemplate ruleOnBodyEndpoint;
    @Produce(uri = "direct://ruleOnCommand")
    protected ProducerTemplate ruleOnCommandEndpoint;

    @Test
    public void testRuleOnBody() throws Exception {
        Person person = new Person();
        person.setName("Young Scott");
        person.setAge(18);

        Person response = ruleOnBodyEndpoint.requestBody(person, Person.class);

        assertNotNull(response);
        assertFalse(person.isCanDrink());

        // Test for alternative result

        person.setName("Scott");
        person.setAge(21);

        response = ruleOnBodyEndpoint.requestBody(person, Person.class);

        assertNotNull(response);
        assertTrue(person.isCanDrink());
    }

    @Test
    public void testRuleOnCommand() throws Exception {
        Person person = new Person();
        person.setName("Young Scott");
        person.setAge(18);

        ExecutionResultImpl response = ruleOnCommandEndpoint.requestBody(person, ExecutionResultImpl.class);

        assertNotNull(response);

        // Expecting single result value of type Person
        Collection<String> identifiers = response.getIdentifiers();
        assertNotNull(identifiers);
        assertTrue(identifiers.size() >= 1);

        for (String identifier : identifiers) {
            final Object value = response.getValue(identifier);
            assertNotNull(value);
            assertIsInstanceOf(Person.class, value);
            assertFalse(((Person) value).isCanDrink());
            System.out.println(identifier + " = " + value);
        }

        // Test for alternative result

        person.setName("Scott");
        person.setAge(21);

        response = ruleOnCommandEndpoint.requestBody(person, ExecutionResultImpl.class);

        assertNotNull(response);

        // Expecting single result value of type Person
        identifiers = response.getIdentifiers();
        assertNotNull(identifiers);
        assertTrue(identifiers.size() >= 1);

        for (String identifier : identifiers) {
            final Object value = response.getValue(identifier);
            assertNotNull(value);
            assertIsInstanceOf(Person.class, value);
            assertTrue(((Person) value).isCanDrink());
            System.out.println(identifier + " = " + value);
        }
    }

    @Override
    protected ClassPathXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("META-INF/spring/camel-context.xml");
    }
}
