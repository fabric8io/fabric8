/*
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

package org.fusesource.bai.config;

import org.fusesource.bai.xml.ConfigHelper;
import org.junit.Test;

import javax.xml.bind.JAXBException;

public class ConfigMarshalTest {
    @Test
    public void fullConfig() throws Exception {
        PolicySet config = new PolicySet();
        config.policy("full").
                excludeContext("*", "audit*").
                includeEndpoint("activemq:*").
                excludeEvent(EventType.FAILURE_HANDLED).
                filter().xpath("/person[@name='James']").
                body().xpath("/foo");

        printXml(config);
    }

    @Test
    public void minimal() throws Exception {
        PolicySet config = new PolicySet();
        config.policy("minimal").
                excludeEndpoint("log:*");

        printXml(config);
    }

    protected void printXml(PolicySet config) throws JAXBException {
        String xml = ConfigHelper.toXml(config);
        System.out.println("XML: " + xml);
    }

}
