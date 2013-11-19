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
package org.fusesource.insight.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Property;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultProducerTemplate;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.fusesource.insight.camel.breadcrumb.Breadcrumbs;
import org.fusesource.insight.camel.profiler.Profiler;
import org.fusesource.insight.camel.profiler.Stats;
import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @version
 */
public class ContainerTest {

    @Test
    public void testProfilerStrategy() throws Exception {
        Profiler profiler = new Profiler();
        Breadcrumbs breadcrumbs = new Breadcrumbs();

        CamelContext context = new DefaultCamelContext();
        profiler.manage(context);
        breadcrumbs.manage(context);
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:a")
                        .doTry()
                        .to("seda:polyglot")
                        .choice()
                        .when(body().isEqualTo("<hello/>"))
                        .to("seda:english")
                        .throwException(new Exception("Error processing exchange"))
                        .endChoice()
                        .when(body().isEqualTo("<hallo/>"))
                        .to("seda:dutch")
                        .delay(2)
                        .to("seda:german")
                        .endChoice()
                        .otherwise()
                        .to("seda:french").endDoTry()
                        .doCatch(Throwable.class)
                        .to("seda:errors");

                String[] eps = { "polyglot", "english", "dutch", "german", "french", "errors" };
                for (String s : eps) {
                    from("seda:" + s)
                            .aggregate(constant("ok"), new BodyInAggregatingStrategy()).completionSize(3)
                            .to("mock:" + s);
                }

            }
        });
        context.start();

        final ProducerTemplate template = new DefaultProducerTemplate(context);
        template.start();

        final String[] values = { "<hello/>", "<hallo/>", "<bonjour/>" };
        final Random rnd = new Random();

        for (int i = 0; i < 100; i++) {
            template.sendBody("direct:a", values[rnd.nextInt(values.length)]);
        }
        profiler.reset();

        long t0 = System.nanoTime();
        int nbThreads = 10;
        final CountDownLatch latch = new CountDownLatch(nbThreads);
        for (int t = 0; t < nbThreads; t++) {
            new Thread() {
                public void run() {
                    for (int i = 0; i < 1000; i++) {
                        template.sendBody("direct:a", values[rnd.nextInt(values.length)]);
                    }
                    latch.countDown();
                }
            }.start();
        }
        latch.await();
        long t1 = System.nanoTime();
        System.out.println("Total time: " + TimeUnit.MILLISECONDS.convert(t1 - t0, TimeUnit.NANOSECONDS));
        print(profiler.getStatistics());

        System.out.println();

        MBeanServer mbeanServer = context.getManagementStrategy().getManagementAgent().getMBeanServer();
        ObjectName on = context.getManagementStrategy().getManagementNamingStrategy().getObjectNameForCamelContext(context);
        String xml = (String) mbeanServer.invoke(on, "dumpRoutesStatsAsXml", new Object[]{false, true}, new String[]{"boolean", "boolean"});
        System.out.println(xml);
    }

    public static int DESCRIPTION_LENGTH = 60;

    protected void print(Map<ProcessorDefinition<?>, Stats> statistics) {
        System.out.println(String.format("%-" + DESCRIPTION_LENGTH + "s %8s %8s %8s %8s", "Processor", "Count", "Time(ms)", "Total(ms)", "Mean(µs)"));
        print(statistics, null, "");
    }

    private void print(Map<ProcessorDefinition<?>, Stats> statistics, Stats parent, String indent) {
        for (Map.Entry<ProcessorDefinition<?>, Stats> e : statistics.entrySet()) {
            ProcessorDefinition<?> p = e.getKey();
            Stats s = e.getValue();
            if (s.getParent() == parent) {
                String name = indent + p.toString();
                if (name.length() > DESCRIPTION_LENGTH) {
                    name = name.substring(0, DESCRIPTION_LENGTH - 4) + "...]";
                } else {
                    while (name.length() < DESCRIPTION_LENGTH) {
                        name += " ";
                    }
                }
                long count = s.getCount();
                long self = s.getSelf();
                long total = s.getTotal();
                System.out.println(String.format("%s %8d %8d %8d %8d",
                        name,
                        count,
                        TimeUnit.MILLISECONDS.convert(self, TimeUnit.NANOSECONDS),
                        TimeUnit.MILLISECONDS.convert(total, TimeUnit.NANOSECONDS),
                        TimeUnit.MICROSECONDS.convert(count > 0 ? total / count : 0, TimeUnit.NANOSECONDS)));
                print(statistics, s, indent + "  ");
            }
        }
    }

    public class BodyInAggregatingStrategy implements AggregationStrategy {

        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            if (oldExchange == null) {
                return newExchange;
            }

            String oldBody = oldExchange.getIn().getBody(String.class);
            String newBody = newExchange.getIn().getBody(String.class);
            oldExchange.getIn().setBody(oldBody + "+" + newBody);
            return oldExchange;
        }

        /**
         * An expression used to determine if the aggregation is complete
         */
        public boolean isCompleted(@Property(Exchange.AGGREGATED_SIZE) Integer aggregated) {
            if (aggregated == null) {
                return false;
            }

            return aggregated == 3;
        }

    }


}
