package org.fusesource.fabric.itests.paxexam.camel;


import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.itests.paxexam.FabricFeaturesTest;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.util.Set;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CamelProfileLongTest extends FabricFeaturesTest {

    @Before
    public void setUp() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        Set<Container> containers = ContainerBuilder.create().withName("camel").withProfiles("camel").assertProvisioningResult().build();
        for (Container container : containers) {
            prepareFeaturesForTesting(container.getId(), "camel-blueprint", "camel", "camel-blueprint");
            prepareFeaturesForTesting(container.getId(), "camel-jms", "camel", "camel-jms");
            prepareFeaturesForTesting(container.getId(), "camel-http", "camel", "camel-http");
            prepareFeaturesForTesting(container.getId(), "camel-cxf", "camel", "camel-cxf");
            prepareFeaturesForTesting(container.getId(), "camel-cache", "camel", "camel-cache");
            prepareFeaturesForTesting(container.getId(), "camel-castor", "camel", "camel-castor");
            prepareFeaturesForTesting(container.getId(), "camel-crypto", "camel", "camel-crypto");
            prepareFeaturesForTesting(container.getId(), "camel-http", "camel", "camel-http");
            prepareFeaturesForTesting(container.getId(), "camel-http4", "camel", "camel-http4");
            prepareFeaturesForTesting(container.getId(), "camel-mina", "camel", "camel-mina");
            prepareFeaturesForTesting(container.getId(), "camel-jetty", "camel", "camel-jetty");
            prepareFeaturesForTesting(container.getId(), "camel-servlet", "camel", "camel-servlet");
            prepareFeaturesForTesting(container.getId(), "camel-jms", "camel", "camel-jms");
            prepareFeaturesForTesting(container.getId(), "camel-jmx", "camel", "camel-jmx");
            prepareFeaturesForTesting(container.getId(), "camel-ahc", "camel", "camel-ahc");
            prepareFeaturesForTesting(container.getId(), "camel-amqp", "camel", "camel-amqp");
            prepareFeaturesForTesting(container.getId(), "camel-atom", "camel", "camel-atom");
            prepareFeaturesForTesting(container.getId(), "camel-aws", "camel", "camel-aws");
            prepareFeaturesForTesting(container.getId(), "camel-bam", "camel", "camel-bam");
            prepareFeaturesForTesting(container.getId(), "camel-bean-validator", "camel", "camel-bean-validator");
            prepareFeaturesForTesting(container.getId(), "camel-bindy", "camel", "camel-bindy");
            prepareFeaturesForTesting(container.getId(), "camel-cometd", "camel", "camel-cometd");
            prepareFeaturesForTesting(container.getId(), "camel-csv", "camel", "camel-csv");
            prepareFeaturesForTesting(container.getId(), "camel-dozer", "camel", "camel-dozer");
            prepareFeaturesForTesting(container.getId(), "camel-eventadmin", "camel", "camel-eventadmin");
            prepareFeaturesForTesting(container.getId(), "camel-exec", "camel", "camel-exec");
            prepareFeaturesForTesting(container.getId(), "camel-flatpack", "camel", "camel-flatpack");
            prepareFeaturesForTesting(container.getId(), "camel-freemarker", "camel", "camel-freemarker");
            prepareFeaturesForTesting(container.getId(), "camel-ftp", "camel", "camel-ftp");
            prepareFeaturesForTesting(container.getId(), "camel-guice", "camel", "camel-guice");
            prepareFeaturesForTesting(container.getId(), "camel-groovy", "camel", "camel-groovy");
            prepareFeaturesForTesting(container.getId(), "camel-hazelcast", "camel", "camel-hazelcast");
            prepareFeaturesForTesting(container.getId(), "camel-hawtdb", "camel", "camel-hawtdb");
            prepareFeaturesForTesting(container.getId(), "camel-hdfs", "camel", "camel-hdfs");
            prepareFeaturesForTesting(container.getId(), "camel-hl7", "camel", "camel-hl7");
            prepareFeaturesForTesting(container.getId(), "camel-ibatis", "camel", "camel-ibatis");
            prepareFeaturesForTesting(container.getId(), "camel-irc", "camel", "camel-irc");
            prepareFeaturesForTesting(container.getId(), "camel-jackson", "camel", "camel-jackson");
            prepareFeaturesForTesting(container.getId(), "camel-jasypt", "camel", "camel-jasypt");
            prepareFeaturesForTesting(container.getId(), "camel-jaxb", "camel", "camel-jaxb");
            prepareFeaturesForTesting(container.getId(), "camel-jclouds", "camel", "camel-jclouds");
            prepareFeaturesForTesting(container.getId(), "camel-jcr", "camel", "camel-jcr");
            prepareFeaturesForTesting(container.getId(), "camel-jing", "camel", "camel-jing");
            prepareFeaturesForTesting(container.getId(), "camel-jibx", "camel", "camel-jibx");
            prepareFeaturesForTesting(container.getId(), "camel-jdbc", "camel", "camel-jdbc");
            prepareFeaturesForTesting(container.getId(), "camel-josql", "camel", "camel-josql");
            prepareFeaturesForTesting(container.getId(), "camel-josql", "camel", "camel-josql");
            prepareFeaturesForTesting(container.getId(), "camel-jpa", "camel", "camel-jpa");
            prepareFeaturesForTesting(container.getId(), "camel-jxpath", "camel", "camel-jxpath");
            prepareFeaturesForTesting(container.getId(), "camel-juel", "camel", "camel-juel");
            prepareFeaturesForTesting(container.getId(), "camel-kestrel", "camel", "camel-kestrel");
            prepareFeaturesForTesting(container.getId(), "camel-krati", "camel", "camel-krati");
            prepareFeaturesForTesting(container.getId(), "camel-ldap", "camel", "camel-ldap");
            prepareFeaturesForTesting(container.getId(), "camel-lucene", "camel", "camel-lucene");
            prepareFeaturesForTesting(container.getId(), "camel-mail", "camel", "camel-mail");
            prepareFeaturesForTesting(container.getId(), "camel-msv", "camel", "camel-msv");
            prepareFeaturesForTesting(container.getId(), "camel-mvel", "camel", "camel-mvel");
            prepareFeaturesForTesting(container.getId(), "camel-mybatis", "camel", "camel-mybatis");
            prepareFeaturesForTesting(container.getId(), "camel-nagios", "camel", "camel-nagios");
            prepareFeaturesForTesting(container.getId(), "camel-netty", "camel", "camel-netty");
            prepareFeaturesForTesting(container.getId(), "camel-ognl", "camel", "camel-ognl");
            prepareFeaturesForTesting(container.getId(), "camel-paxlogging", "camel", "camel-paxlogging");
            prepareFeaturesForTesting(container.getId(), "camel-printer", "camel", "camel-printer");
            prepareFeaturesForTesting(container.getId(), "camel-protobuf", "camel", "camel-protobuf");
            prepareFeaturesForTesting(container.getId(), "camel-quartz", "camel", "camel-quartz");
            prepareFeaturesForTesting(container.getId(), "camel-quickfix", "camel", "camel-quickfix");
            prepareFeaturesForTesting(container.getId(), "camel-restlet", "camel", "camel-restlet");
            prepareFeaturesForTesting(container.getId(), "camel-rmi", "camel", "camel-rmi");
            prepareFeaturesForTesting(container.getId(), "camel-routebox", "camel", "camel-routebox");
            prepareFeaturesForTesting(container.getId(), "camel-ruby", "camel", "org.jruby.jruby");
            prepareFeaturesForTesting(container.getId(), "camel-rss", "camel", "camel-rss");
            prepareFeaturesForTesting(container.getId(), "camel-saxon", "camel", "camel-saxon");
            prepareFeaturesForTesting(container.getId(), "camel-scala", "camel", "camel-scala");
            prepareFeaturesForTesting(container.getId(), "camel-script", "camel", "camel-script");
            prepareFeaturesForTesting(container.getId(), "camel-sip", "camel", "camel-sip");
            prepareFeaturesForTesting(container.getId(), "camel-shiro", "camel", "camel-shiro");
            prepareFeaturesForTesting(container.getId(), "camel-smpp", "camel", "camel-smpp");
            prepareFeaturesForTesting(container.getId(), "camel-snmp", "camel", "camel-snmp");
            prepareFeaturesForTesting(container.getId(), "camel-soap", "camel", "camel-soap");
            prepareFeaturesForTesting(container.getId(), "camel-solr", "camel", "camel-solr");
            prepareFeaturesForTesting(container.getId(), "camel-spring-integration", "camel", "camel-spring-integration");
            prepareFeaturesForTesting(container.getId(), "camel-spring-javaconfig", "camel", "camel-spring-javaconfig");
            prepareFeaturesForTesting(container.getId(), "camel-spring-security", "camel", "camel-spring-security");
            prepareFeaturesForTesting(container.getId(), "camel-spring-ws", "camel", "camel-spring-ws");
            prepareFeaturesForTesting(container.getId(), "camel-sql", "camel", "camel-sql");
            prepareFeaturesForTesting(container.getId(), "camel-stax", "camel", "camel-stax");
            prepareFeaturesForTesting(container.getId(), "camel-stream", "camel", "camel-stream");
            prepareFeaturesForTesting(container.getId(), "camel-string-template", "camel", "org.apache.servicemix.bundles.stringtemplate");
            prepareFeaturesForTesting(container.getId(), "camel-syslog", "camel", "camel-syslog");
            prepareFeaturesForTesting(container.getId(), "camel-tagsoup", "camel", "camel-tagsoup");
            prepareFeaturesForTesting(container.getId(), "camel-velocity", "camel", "camel-velocity");
            prepareFeaturesForTesting(container.getId(), "camel-xmlbeans", "camel", "camel-xmlbeans");
            prepareFeaturesForTesting(container.getId(), "camel-xmlsecurity", "camel", "camel-xmlsecurity");
            prepareFeaturesForTesting(container.getId(), "camel-xmpp", "camel", "camel-xmpp");
            prepareFeaturesForTesting(container.getId(), "camel-xstream", "camel", "camel-xstream");
            prepareFeaturesForTesting(container.getId(), "camel-zookeeper", "camel", "camel-zookeeper");

            //prepareFeaturesForTesting(container.getId(), "camel-script camel-script-jruby", "camel", "camel-script-jruby");
            //prepareFeaturesForTesting(container.getId(), "camel-script camel-script-javascript", "camel", "camel-script-javascript");
            //prepareFeaturesForTesting(container.getId(), "camel-script camel-script-groovy", "camel", "camel-script-groovy");
        }

    }

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }
}
