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
        Set<Container> containers = ContainerBuilder.create().withName("camel").withProfiles("default").assertProvisioningResult().build();
            prepareFeaturesForTesting(containers, "camel-blueprint", "camel", "camel-blueprint");
            prepareFeaturesForTesting(containers, "camel-jms", "camel", "camel-jms");
            prepareFeaturesForTesting(containers, "camel-http", "camel", "camel-http");
            prepareFeaturesForTesting(containers, "camel-cxf", "camel", "camel-cxf");
            prepareFeaturesForTesting(containers, "camel-cache", "camel", "camel-cache");
            prepareFeaturesForTesting(containers, "camel-castor", "camel", "camel-castor");

            prepareFeaturesForTesting(containers, "camel-http", "camel", "camel-http");
            prepareFeaturesForTesting(containers, "camel-http4", "camel", "camel-http4");
            prepareFeaturesForTesting(containers, "camel-mina", "camel", "camel-mina");
            prepareFeaturesForTesting(containers, "camel-jetty", "camel", "camel-jetty");
            prepareFeaturesForTesting(containers, "camel-servlet", "camel", "camel-servlet");
            prepareFeaturesForTesting(containers, "camel-jms", "camel", "camel-jms");
            prepareFeaturesForTesting(containers, "camel-jmx", "camel", "camel-jmx");
            prepareFeaturesForTesting(containers, "camel-ahc", "camel", "camel-ahc");
            prepareFeaturesForTesting(containers, "camel-amqp", "camel", "camel-amqp");
            prepareFeaturesForTesting(containers, "camel-atom", "camel", "camel-atom");
            prepareFeaturesForTesting(containers, "camel-aws", "camel", "camel-aws");
            prepareFeaturesForTesting(containers, "camel-bam", "camel", "camel-bam");
            prepareFeaturesForTesting(containers, "camel-bean-validator", "camel", "camel-bean-validator");
            prepareFeaturesForTesting(containers, "camel-bindy", "camel", "camel-bindy");
            prepareFeaturesForTesting(containers, "camel-cometd", "camel", "camel-cometd");
            prepareFeaturesForTesting(containers, "camel-csv", "camel", "camel-csv");
            prepareFeaturesForTesting(containers, "camel-dozer", "camel", "camel-dozer");
            prepareFeaturesForTesting(containers, "camel-eventadmin", "camel", "camel-eventadmin");
            prepareFeaturesForTesting(containers, "camel-exec", "camel", "camel-exec");
            prepareFeaturesForTesting(containers, "camel-flatpack", "camel", "camel-flatpack");
            prepareFeaturesForTesting(containers, "camel-freemarker", "camel", "camel-freemarker");
            prepareFeaturesForTesting(containers, "camel-ftp", "camel", "camel-ftp");
            prepareFeaturesForTesting(containers, "camel-guice", "camel", "camel-guice");
            prepareFeaturesForTesting(containers, "camel-groovy", "camel", "camel-groovy");
            prepareFeaturesForTesting(containers, "camel-hazelcast", "camel", "camel-hazelcast");
            prepareFeaturesForTesting(containers, "camel-hawtdb", "camel", "camel-hawtdb");
            prepareFeaturesForTesting(containers, "camel-hdfs", "camel", "camel-hdfs");
            prepareFeaturesForTesting(containers, "camel-hl7", "camel", "camel-hl7");
            prepareFeaturesForTesting(containers, "camel-ibatis", "camel", "camel-ibatis");
            prepareFeaturesForTesting(containers, "camel-irc", "camel", "camel-irc");
            prepareFeaturesForTesting(containers, "camel-jackson", "camel", "camel-jackson");
            prepareFeaturesForTesting(containers, "camel-jasypt", "camel", "camel-jasypt");
            prepareFeaturesForTesting(containers, "camel-jaxb", "camel", "camel-jaxb");
            prepareFeaturesForTesting(containers, "camel-jclouds", "camel", "camel-jclouds");
            prepareFeaturesForTesting(containers, "camel-jcr", "camel", "camel-jcr");
            prepareFeaturesForTesting(containers, "camel-jing", "camel", "camel-jing");
            prepareFeaturesForTesting(containers, "camel-jibx", "camel", "camel-jibx");
            prepareFeaturesForTesting(containers, "camel-jdbc", "camel", "camel-jdbc");
            prepareFeaturesForTesting(containers, "camel-josql", "camel", "camel-josql");
            prepareFeaturesForTesting(containers, "camel-josql", "camel", "camel-josql");
            prepareFeaturesForTesting(containers, "camel-jpa", "camel", "camel-jpa");
            prepareFeaturesForTesting(containers, "camel-jxpath", "camel", "camel-jxpath");
            prepareFeaturesForTesting(containers, "camel-juel", "camel", "camel-juel");
            prepareFeaturesForTesting(containers, "camel-kestrel", "camel", "camel-kestrel");
            prepareFeaturesForTesting(containers, "camel-krati", "camel", "camel-krati");
            prepareFeaturesForTesting(containers, "camel-ldap", "camel", "camel-ldap");
            prepareFeaturesForTesting(containers, "camel-lucene", "camel", "camel-lucene");
            prepareFeaturesForTesting(containers, "camel-mail", "camel", "camel-mail");
            prepareFeaturesForTesting(containers, "camel-msv", "camel", "camel-msv");
            prepareFeaturesForTesting(containers, "camel-mvel", "camel", "camel-mvel");
            prepareFeaturesForTesting(containers, "camel-mybatis", "camel", "camel-mybatis");
            prepareFeaturesForTesting(containers, "camel-nagios", "camel", "camel-nagios");
            prepareFeaturesForTesting(containers, "camel-netty", "camel", "camel-netty");
            prepareFeaturesForTesting(containers, "camel-ognl", "camel", "camel-ognl");
            prepareFeaturesForTesting(containers, "camel-paxlogging", "camel", "camel-paxlogging");
            prepareFeaturesForTesting(containers, "camel-printer", "camel", "camel-printer");
            prepareFeaturesForTesting(containers, "camel-protobuf", "camel", "camel-protobuf");
            prepareFeaturesForTesting(containers, "camel-quartz", "camel", "camel-quartz");
            prepareFeaturesForTesting(containers, "camel-quickfix", "camel", "camel-quickfix");
            prepareFeaturesForTesting(containers, "camel-restlet", "camel", "camel-restlet");
            prepareFeaturesForTesting(containers, "camel-rmi", "camel", "camel-rmi");
            prepareFeaturesForTesting(containers, "camel-routebox", "camel", "camel-routebox");
            prepareFeaturesForTesting(containers, "camel-ruby", "camel", "org.jruby.jruby");
            prepareFeaturesForTesting(containers, "camel-rss", "camel", "camel-rss");
            prepareFeaturesForTesting(containers, "camel-saxon", "camel", "camel-saxon");
            prepareFeaturesForTesting(containers, "camel-scala", "camel", "camel-scala");
            prepareFeaturesForTesting(containers, "camel-script", "camel", "camel-script");
            prepareFeaturesForTesting(containers, "camel-sip", "camel", "camel-sip");
            prepareFeaturesForTesting(containers, "camel-shiro", "camel", "camel-shiro");
            prepareFeaturesForTesting(containers, "camel-smpp", "camel", "camel-smpp");
            prepareFeaturesForTesting(containers, "camel-snmp", "camel", "camel-snmp");
            prepareFeaturesForTesting(containers, "camel-soap", "camel", "camel-soap");
            prepareFeaturesForTesting(containers, "camel-solr", "camel", "camel-solr");
            prepareFeaturesForTesting(containers, "camel-spring-integration", "camel", "camel-spring-integration");
            prepareFeaturesForTesting(containers, "camel-spring-javaconfig", "camel", "camel-spring-javaconfig");
            prepareFeaturesForTesting(containers, "camel-spring-security", "camel", "camel-spring-security");
            prepareFeaturesForTesting(containers, "camel-spring-ws", "camel", "camel-spring-ws");
            prepareFeaturesForTesting(containers, "camel-sql", "camel", "camel-sql");
            prepareFeaturesForTesting(containers, "camel-stax", "camel", "camel-stax");
            prepareFeaturesForTesting(containers, "camel-stream", "camel", "camel-stream");
            prepareFeaturesForTesting(containers, "camel-string-template", "camel", "org.apache.servicemix.bundles.stringtemplate");
            prepareFeaturesForTesting(containers, "camel-syslog", "camel", "camel-syslog");
            prepareFeaturesForTesting(containers, "camel-tagsoup", "camel", "camel-tagsoup");
            prepareFeaturesForTesting(containers, "camel-velocity", "camel", "camel-velocity");
            prepareFeaturesForTesting(containers, "camel-xmlbeans", "camel", "camel-xmlbeans");
            prepareFeaturesForTesting(containers, "camel-xmlsecurity", "camel", "camel-xmlsecurity");
            prepareFeaturesForTesting(containers, "camel-xmpp", "camel", "camel-xmpp");
            prepareFeaturesForTesting(containers, "camel-xstream", "camel", "camel-xstream");
            prepareFeaturesForTesting(containers, "camel-zookeeper", "camel", "camel-zookeeper");

            //prepareFeaturesForTesting(containers, "camel-crypto", "camel", "camel-crypto");
            //prepareFeaturesForTesting(containers, "camel-script camel-script-jruby", "camel", "camel-script-jruby");
            //prepareFeaturesForTesting(containers, "camel-script camel-script-javascript", "camel", "camel-script-javascript");
            //prepareFeaturesForTesting(containers, "camel-script camel-script-groovy", "camel", "camel-script-groovy");

    }

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }
}
