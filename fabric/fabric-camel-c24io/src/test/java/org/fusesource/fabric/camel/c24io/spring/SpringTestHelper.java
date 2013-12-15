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

import org.apache.camel.CamelContext;
import org.apache.camel.Service;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;

import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @version 
 */
public final class SpringTestHelper {
    
    private SpringTestHelper() {        
    }
    
    public static CamelContext createSpringCamelContext(CamelTestSupport test, String classpathUri) throws Exception {
        test.setUseRouteBuilder(false);

        final AbstractXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(classpathUri);
        test.setCamelContextService(new Service() {
            public void start() throws Exception {
                applicationContext.start();
            }

            public void stop() throws Exception {
                applicationContext.stop();
            }
        });

        return SpringCamelContext.springCamelContext(applicationContext);
    }
}
