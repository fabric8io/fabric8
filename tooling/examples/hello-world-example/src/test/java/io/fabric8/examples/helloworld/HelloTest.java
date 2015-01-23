/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.examples.helloworld;

import io.fabric8.examples.helloworld.impl.DefaultHello;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class HelloTest {

    public static Logger LOG = LoggerFactory.getLogger(HelloTest.class);

    private HelloInterface helloInterface;

    @Before
    public void init() {
        helloInterface = mock(HelloInterface.class);
        when(helloInterface.hello(anyString())).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return "Hello " + invocation.getArguments()[0] + "!";
            }
        });
    }

    @Test
    public void testMockedHello() {
        LOG.info("testMockedHello()");
        assertThat(helloInterface.hello("World"), equalTo("Hello World!"));
    }

    @Test
    public void testHello() {
        LOG.info("testHello()");
        assertThat(new DefaultHello().hello("World"), equalTo("Hi World"));
    }

}
