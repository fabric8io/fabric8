package org.fusesource.fabric.itests.paxexam.camel;


import org.fusesource.fabric.itests.paxexam.FabricFeaturesTest;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CamelProfileLongTest extends FabricFeaturesTest {

    @After
    public void tearDown() throws InterruptedException {
        destroyChildContainer("camel1");
    }

    @Test
    public void testFeatures() throws Exception {
        System.err.println(executeCommand("fabric:create"));
        createAndAssetChildContainer("camel1", "root", "camel");
        assertProvisionedFeature("camel1", "camel-jms", "camel", "camel-jms");
        assertProvisionedFeature("camel1", "camel-http", "camel", "camel-http");
        assertProvisionedFeature("camel1", "camel-cxf", "camel", "camel-cxf");
        assertProvisionedFeature("camel1", "camel-cache", "camel", "camel-cache");
        assertProvisionedFeature("camel1", "camel-castor", "camel", "camel-castor");
        assertProvisionedFeature("camel1", "camel-crypto", "camel", "camel-crypto");
        assertProvisionedFeature("camel1", "camel-http", "camel", "camel-http");
        assertProvisionedFeature("camel1", "camel-http4", "camel", "camel-http4");
        assertProvisionedFeature("camel1", "camel-mina", "camel", "camel-mina");
        assertProvisionedFeature("camel1", "camel-jetty", "camel", "camel-jetty");
        assertProvisionedFeature("camel1", "camel-servlet", "camel", "camel-servlet");
        assertProvisionedFeature("camel1", "camel-jms", "camel", "camel-jms");
        assertProvisionedFeature("camel1", "camel-jmx", "camel", "camel-jmx");
        assertProvisionedFeature("camel1", "camel-ahc", "camel", "camel-ahc");
        assertProvisionedFeature("camel1", "camel-amqp", "camel", "camel-amqp");
        assertProvisionedFeature("camel1", "camel-atom", "camel", "camel-atom");
        assertProvisionedFeature("camel1", "camel-aws", "camel", "camel-aws");
        assertProvisionedFeature("camel1", "camel-bam", "camel", "camel-bam");
        assertProvisionedFeature("camel1", "camel-bean-validator", "camel", "camel-bean-validator");
        assertProvisionedFeature("camel1", "camel-bindy", "camel", "camel-bindy");
        assertProvisionedFeature("camel1", "camel-cometd", "camel", "camel-cometd");
        assertProvisionedFeature("camel1", "camel-csv", "camel", "camel-csv");
        assertProvisionedFeature("camel1", "camel-dozer", "camel", "camel-dozer");
        assertProvisionedFeature("camel1", "camel-eventadmin", "camel", "camel-eventadmin");
        assertProvisionedFeature("camel1", "camel-exec", "camel", "camel-exec");
        assertProvisionedFeature("camel1", "camel-flatpack", "camel", "camel-flatpack");
        assertProvisionedFeature("camel1", "camel-freemarker", "camel", "camel-freemarker");
        assertProvisionedFeature("camel1", "camel-ftp", "camel", "camel-ftp");
        assertProvisionedFeature("camel1", "camel-guice", "camel", "camel-guice");
        assertProvisionedFeature("camel1", "camel-groovy", "camel", "camel-groovy");
        assertProvisionedFeature("camel1", "camel-hazelcast", "camel", "camel-hazelcast");
        assertProvisionedFeature("camel1", "camel-hawtdb", "camel", "camel-hawtdb");
        assertProvisionedFeature("camel1", "camel-hdfs", "camel", "camel-hdfs");
        assertProvisionedFeature("camel1", "camel-hl7", "camel", "camel-hl7");
        assertProvisionedFeature("camel1", "camel-ibatis", "camel", "camel-ibatis");
        assertProvisionedFeature("camel1", "camel-irc", "camel", "camel-irc");
        assertProvisionedFeature("camel1", "camel-jackson", "camel", "camel-jackson");
        assertProvisionedFeature("camel1", "camel-jasypt", "camel", "camel-jasypt");
        assertProvisionedFeature("camel1", "camel-jaxb", "camel", "camel-jaxb");
        assertProvisionedFeature("camel1", "camel-jclouds", "camel", "camel-jclouds");
        assertProvisionedFeature("camel1", "camel-jcr", "camel", "camel-jcr");
        assertProvisionedFeature("camel1", "camel-jing", "camel", "camel-jing");
        assertProvisionedFeature("camel1", "camel-jibx", "camel", "camel-jibx");
        assertProvisionedFeature("camel1", "camel-jdbc", "camel", "camel-jdbc");
        assertProvisionedFeature("camel1", "camel-josql", "camel", "camel-josql");
        assertProvisionedFeature("camel1", "camel-josql", "camel", "camel-josql");
        assertProvisionedFeature("camel1", "camel-jpa", "camel", "camel-jpa");
        assertProvisionedFeature("camel1", "camel-jxpath", "camel", "camel-jxpath");
        assertProvisionedFeature("camel1", "camel-juel", "camel", "camel-juel");
        assertProvisionedFeature("camel1", "camel-kestrel", "camel", "camel-kestrel");
        assertProvisionedFeature("camel1", "camel-krati", "camel", "camel-krati");
        assertProvisionedFeature("camel1", "camel-ldap", "camel", "camel-ldap");
        assertProvisionedFeature("camel1", "camel-lucene", "camel", "camel-lucene");
        assertProvisionedFeature("camel1", "camel-mail", "camel", "camel-mail");
        assertProvisionedFeature("camel1", "camel-msv", "camel", "camel-msv");
        assertProvisionedFeature("camel1", "camel-mvel", "camel", "camel-mvel");
        assertProvisionedFeature("camel1", "camel-mybatis", "camel", "camel-mybatis");
        assertProvisionedFeature("camel1", "camel-nagios", "camel", "camel-nagios");
        assertProvisionedFeature("camel1", "camel-netty", "camel", "camel-netty");
        assertProvisionedFeature("camel1", "camel-ognl", "camel", "camel-ognl");
        assertProvisionedFeature("camel1", "camel-paxlogging", "camel", "camel-paxlogging");
        assertProvisionedFeature("camel1", "camel-printer", "camel", "camel-printer");
        assertProvisionedFeature("camel1", "camel-protobuf", "camel", "camel-protobuf");
        assertProvisionedFeature("camel1", "camel-quartz", "camel", "camel-quartz");
        assertProvisionedFeature("camel1", "camel-quickfix", "camel", "camel-quickfix");
        assertProvisionedFeature("camel1", "camel-restlet", "camel", "camel-restlet");
        assertProvisionedFeature("camel1", "camel-rmi", "camel", "camel-rmi");
        assertProvisionedFeature("camel1", "camel-routebox", "camel", "camel-routebox");
        assertProvisionedFeature("camel1", "camel-ruby", "camel", "camel-ruby");
        assertProvisionedFeature("camel1", "camel-rss", "camel", "camel-rss");
        assertProvisionedFeature("camel1", "camel-saxon", "camel", "camel-saxon");
        assertProvisionedFeature("camel1", "camel-scala", "camel", "camel-scala");
        assertProvisionedFeature("camel1", "camel-script-jruby", "camel", "camel-script-jruby");
        assertProvisionedFeature("camel1", "camel-script-javascript", "camel", "camel-script-javascript");
        assertProvisionedFeature("camel1", "camel-script-groovy", "camel", "camel-script-groovy");
        assertProvisionedFeature("camel1", "camel-script", "camel", "camel-script");
        assertProvisionedFeature("camel1", "camel-sip", "camel", "camel-sip");
        assertProvisionedFeature("camel1", "camel-shiro", "camel", "camel-shiro");
        assertProvisionedFeature("camel1", "camel-smpp", "camel", "camel-smpp");
        assertProvisionedFeature("camel1", "camel-snmp", "camel", "camel-snmp");
        assertProvisionedFeature("camel1", "camel-soap", "camel", "camel-soap");
        assertProvisionedFeature("camel1", "camel-solr", "camel", "camel-solr");
        assertProvisionedFeature("camel1", "camel-spring-integration", "camel", "camel-spring-integration");
        assertProvisionedFeature("camel1", "camel-spring-javaconfig", "camel", "camel-spring-javaconfig");
        assertProvisionedFeature("camel1", "camel-spring-security", "camel", "camel-spring-security");
        assertProvisionedFeature("camel1", "camel-spring-ws", "camel", "camel-spring-ws");
        assertProvisionedFeature("camel1", "camel-sql", "camel", "camel-sql");
        assertProvisionedFeature("camel1", "camel-stax", "camel", "camel-stax");
        assertProvisionedFeature("camel1", "camel-stream", "camel", "camel-stream");
        assertProvisionedFeature("camel1", "camel-string-template", "camel", "camel-string-template");
        assertProvisionedFeature("camel1", "camel-syslog", "camel", "camel-syslog");
        assertProvisionedFeature("camel1", "camel-tagsoup", "camel", "camel-tagsoup");
        assertProvisionedFeature("camel1", "camel-velocity", "camel", "camel-velocity");
        assertProvisionedFeature("camel1", "camel-xmlbeans", "camel", "camel-xmlbeans");
        assertProvisionedFeature("camel1", "camel-xmlsecurity", "camel", "camel-xmlsecurity");
        assertProvisionedFeature("camel1", "camel-xmpp", "camel", "camel-xmpp");
        assertProvisionedFeature("camel1", "camel-xstream", "camel", "camel-xstream");
        assertProvisionedFeature("camel1", "camel-zookeeper", "camel", "camel-zookeeper");
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                fabricDistributionConfiguration(), keepRuntimeFolder(),
                logLevel(LogLevelOption.LogLevel.ERROR)};
    }
}
