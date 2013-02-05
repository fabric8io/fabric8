package org.fusesource.fabric.itests.paxexam.camel;


import org.fusesource.fabric.itests.paxexam.FabricFeaturesTest;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CamelProfileLongTest extends FabricFeaturesTest {

    @Before
    public void setUp() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        createAndAssertChildContainer("camel1", "root", "camel");
        prepareFeaturesForTesting("camel1", "camel-blueprint", "camel", "camel-blueprint");
        prepareFeaturesForTesting("camel1", "camel-jms", "camel", "camel-jms");
        prepareFeaturesForTesting("camel1", "camel-http", "camel", "camel-http");
        prepareFeaturesForTesting("camel1", "camel-cxf", "camel", "camel-cxf");
        prepareFeaturesForTesting("camel1", "camel-cache", "camel", "camel-cache");
        prepareFeaturesForTesting("camel1", "camel-castor", "camel", "camel-castor");
        prepareFeaturesForTesting("camel1", "camel-crypto", "camel", "camel-crypto");
        prepareFeaturesForTesting("camel1", "camel-http", "camel", "camel-http");
        prepareFeaturesForTesting("camel1", "camel-http4", "camel", "camel-http4");
        prepareFeaturesForTesting("camel1", "camel-mina", "camel", "camel-mina");
        prepareFeaturesForTesting("camel1", "camel-jetty", "camel", "camel-jetty");
        prepareFeaturesForTesting("camel1", "camel-servlet", "camel", "camel-servlet");
        prepareFeaturesForTesting("camel1", "camel-jms", "camel", "camel-jms");
        prepareFeaturesForTesting("camel1", "camel-jmx", "camel", "camel-jmx");
        prepareFeaturesForTesting("camel1", "camel-ahc", "camel", "camel-ahc");
        prepareFeaturesForTesting("camel1", "camel-amqp", "camel", "camel-amqp");
        prepareFeaturesForTesting("camel1", "camel-atom", "camel", "camel-atom");
        prepareFeaturesForTesting("camel1", "camel-aws", "camel", "camel-aws");
        prepareFeaturesForTesting("camel1", "camel-bam", "camel", "camel-bam");
        prepareFeaturesForTesting("camel1", "camel-bean-validator", "camel", "camel-bean-validator");
        prepareFeaturesForTesting("camel1", "camel-bindy", "camel", "camel-bindy");
        prepareFeaturesForTesting("camel1", "camel-cometd", "camel", "camel-cometd");
        prepareFeaturesForTesting("camel1", "camel-csv", "camel", "camel-csv");
        prepareFeaturesForTesting("camel1", "camel-dozer", "camel", "camel-dozer");
        prepareFeaturesForTesting("camel1", "camel-eventadmin", "camel", "camel-eventadmin");
        prepareFeaturesForTesting("camel1", "camel-exec", "camel", "camel-exec");
        prepareFeaturesForTesting("camel1", "camel-flatpack", "camel", "camel-flatpack");
        prepareFeaturesForTesting("camel1", "camel-freemarker", "camel", "camel-freemarker");
        prepareFeaturesForTesting("camel1", "camel-ftp", "camel", "camel-ftp");
        prepareFeaturesForTesting("camel1", "camel-guice", "camel", "camel-guice");
        prepareFeaturesForTesting("camel1", "camel-groovy", "camel", "camel-groovy");
        prepareFeaturesForTesting("camel1", "camel-hazelcast", "camel", "camel-hazelcast");
        prepareFeaturesForTesting("camel1", "camel-hawtdb", "camel", "camel-hawtdb");
        prepareFeaturesForTesting("camel1", "camel-hdfs", "camel", "camel-hdfs");
        prepareFeaturesForTesting("camel1", "camel-hl7", "camel", "camel-hl7");
        prepareFeaturesForTesting("camel1", "camel-ibatis", "camel", "camel-ibatis");
        prepareFeaturesForTesting("camel1", "camel-irc", "camel", "camel-irc");
        prepareFeaturesForTesting("camel1", "camel-jackson", "camel", "camel-jackson");
        prepareFeaturesForTesting("camel1", "camel-jasypt", "camel", "camel-jasypt");
        prepareFeaturesForTesting("camel1", "camel-jaxb", "camel", "camel-jaxb");
        prepareFeaturesForTesting("camel1", "camel-jclouds", "camel", "camel-jclouds");
        prepareFeaturesForTesting("camel1", "camel-jcr", "camel", "camel-jcr");
        prepareFeaturesForTesting("camel1", "camel-jing", "camel", "camel-jing");
        prepareFeaturesForTesting("camel1", "camel-jibx", "camel", "camel-jibx");
        prepareFeaturesForTesting("camel1", "camel-jdbc", "camel", "camel-jdbc");
        prepareFeaturesForTesting("camel1", "camel-josql", "camel", "camel-josql");
        prepareFeaturesForTesting("camel1", "camel-josql", "camel", "camel-josql");
        prepareFeaturesForTesting("camel1", "camel-jpa", "camel", "camel-jpa");
        prepareFeaturesForTesting("camel1", "camel-jxpath", "camel", "camel-jxpath");
        prepareFeaturesForTesting("camel1", "camel-juel", "camel", "camel-juel");
        prepareFeaturesForTesting("camel1", "camel-kestrel", "camel", "camel-kestrel");
        prepareFeaturesForTesting("camel1", "camel-krati", "camel", "camel-krati");
        prepareFeaturesForTesting("camel1", "camel-ldap", "camel", "camel-ldap");
        prepareFeaturesForTesting("camel1", "camel-lucene", "camel", "camel-lucene");
        prepareFeaturesForTesting("camel1", "camel-mail", "camel", "camel-mail");
        prepareFeaturesForTesting("camel1", "camel-msv", "camel", "camel-msv");
        prepareFeaturesForTesting("camel1", "camel-mvel", "camel", "camel-mvel");
        prepareFeaturesForTesting("camel1", "camel-mybatis", "camel", "camel-mybatis");
        prepareFeaturesForTesting("camel1", "camel-nagios", "camel", "camel-nagios");
        prepareFeaturesForTesting("camel1", "camel-netty", "camel", "camel-netty");
        prepareFeaturesForTesting("camel1", "camel-ognl", "camel", "camel-ognl");
        prepareFeaturesForTesting("camel1", "camel-paxlogging", "camel", "camel-paxlogging");
        prepareFeaturesForTesting("camel1", "camel-printer", "camel", "camel-printer");
        prepareFeaturesForTesting("camel1", "camel-protobuf", "camel", "camel-protobuf");
        prepareFeaturesForTesting("camel1", "camel-quartz", "camel", "camel-quartz");
        prepareFeaturesForTesting("camel1", "camel-quickfix", "camel", "camel-quickfix");
        prepareFeaturesForTesting("camel1", "camel-restlet", "camel", "camel-restlet");
        prepareFeaturesForTesting("camel1", "camel-rmi", "camel", "camel-rmi");
        prepareFeaturesForTesting("camel1", "camel-routebox", "camel", "camel-routebox");
        prepareFeaturesForTesting("camel1", "camel-ruby", "camel", "org.jruby.jruby");
        prepareFeaturesForTesting("camel1", "camel-rss", "camel", "camel-rss");
        prepareFeaturesForTesting("camel1", "camel-saxon", "camel", "camel-saxon");
        prepareFeaturesForTesting("camel1", "camel-scala", "camel", "camel-scala");
        prepareFeaturesForTesting("camel1", "camel-script", "camel", "camel-script");
        prepareFeaturesForTesting("camel1", "camel-sip", "camel", "camel-sip");
        prepareFeaturesForTesting("camel1", "camel-shiro", "camel", "camel-shiro");
        prepareFeaturesForTesting("camel1", "camel-smpp", "camel", "camel-smpp");
        prepareFeaturesForTesting("camel1", "camel-snmp", "camel", "camel-snmp");
        prepareFeaturesForTesting("camel1", "camel-soap", "camel", "camel-soap");
        prepareFeaturesForTesting("camel1", "camel-solr", "camel", "camel-solr");
        prepareFeaturesForTesting("camel1", "camel-spring-integration", "camel", "camel-spring-integration");
        prepareFeaturesForTesting("camel1", "camel-spring-javaconfig", "camel", "camel-spring-javaconfig");
        prepareFeaturesForTesting("camel1", "camel-spring-security", "camel", "camel-spring-security");
        prepareFeaturesForTesting("camel1", "camel-spring-ws", "camel", "camel-spring-ws");
        prepareFeaturesForTesting("camel1", "camel-sql", "camel", "camel-sql");
        prepareFeaturesForTesting("camel1", "camel-stax", "camel", "camel-stax");
        prepareFeaturesForTesting("camel1", "camel-stream", "camel", "camel-stream");
        prepareFeaturesForTesting("camel1", "camel-string-template", "camel", "org.apache.servicemix.bundles.stringtemplate");
        prepareFeaturesForTesting("camel1", "camel-syslog", "camel", "camel-syslog");
        prepareFeaturesForTesting("camel1", "camel-tagsoup", "camel", "camel-tagsoup");
        prepareFeaturesForTesting("camel1", "camel-velocity", "camel", "camel-velocity");
        prepareFeaturesForTesting("camel1", "camel-xmlbeans", "camel", "camel-xmlbeans");
        prepareFeaturesForTesting("camel1", "camel-xmlsecurity", "camel", "camel-xmlsecurity");
        prepareFeaturesForTesting("camel1", "camel-xmpp", "camel", "camel-xmpp");
        prepareFeaturesForTesting("camel1", "camel-xstream", "camel", "camel-xstream");
        prepareFeaturesForTesting("camel1", "camel-zookeeper", "camel", "camel-zookeeper");

        //prepareFeaturesForTesting("camel1", "camel-script camel-script-jruby", "camel", "camel-script-jruby");
        //prepareFeaturesForTesting("camel1", "camel-script camel-script-javascript", "camel", "camel-script-javascript");
        //prepareFeaturesForTesting("camel1", "camel-script camel-script-groovy", "camel", "camel-script-groovy");

    }

    @After
    public void tearDown() throws InterruptedException {
        destroyChildContainer("camel1");
    }
}
