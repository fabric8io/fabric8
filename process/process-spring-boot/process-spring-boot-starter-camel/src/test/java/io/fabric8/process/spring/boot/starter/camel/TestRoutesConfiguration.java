package io.fabric8.process.spring.boot.starter.camel;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestRoutesConfiguration {

    @Bean
    RouteBuilder routeBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:test").routeId("test").to("mock:test");
            }
        };
    }

}
