package org.fusesource.camel.component.sap.integration;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Integration test cases for server RFC calls.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class ITestServerCall extends CamelSpringTestSupport {

	@Test
	public void test() throws Exception {
		Thread.sleep(60000);
	}

	@Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("sap:server:nplServer:PARAM_TEST").to("mock:result");
            }
        };
    }

	@Override
	protected ClassPathXmlApplicationContext createApplicationContext() {
		return new ClassPathXmlApplicationContext(
				"org/fusesource/camel/component/sap/integration/ITestCallConfig.xml");
	}

}
