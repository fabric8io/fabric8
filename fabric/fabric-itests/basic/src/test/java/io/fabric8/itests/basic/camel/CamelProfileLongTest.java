package io.fabric8.itests.basic.camel;

import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.FabricFeaturesTest;

import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CamelProfileLongTest extends FabricFeaturesTest {

    @Test
    public void testFeatures() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            CuratorFramework curator = fabricService.adapt(CuratorFramework.class);

            Set<ContainerProxy> containers = ContainerBuilder.create(fabricProxy).withName("feautre-camel").withProfiles("default").assertProvisioningResult().build();
            try {
                prepareFeaturesForTesting(containers, "camel-blueprint", "feautre-camel", "camel-blueprint");
                prepareFeaturesForTesting(containers, "camel-jms", "feautre-camel", "camel-jms");
                prepareFeaturesForTesting(containers, "camel-http", "feautre-camel", "camel-http");
                prepareFeaturesForTesting(containers, "camel-cxf", "feautre-camel", "camel-cxf");
                prepareFeaturesForTesting(containers, "camel-cache", "feautre-camel", "camel-cache");
                prepareFeaturesForTesting(containers, "camel-castor", "feautre-camel", "camel-castor");

                prepareFeaturesForTesting(containers, "camel-http", "feautre-camel", "camel-http");
                prepareFeaturesForTesting(containers, "camel-http4", "feautre-camel", "camel-http4");
                prepareFeaturesForTesting(containers, "camel-mina", "feautre-camel", "camel-mina");
                prepareFeaturesForTesting(containers, "camel-jetty", "feautre-camel", "camel-jetty");
                prepareFeaturesForTesting(containers, "camel-servlet", "feautre-camel", "camel-servlet");
                prepareFeaturesForTesting(containers, "camel-jms", "feautre-camel", "camel-jms");
                prepareFeaturesForTesting(containers, "camel-jmx", "feautre-camel", "camel-jmx");
                prepareFeaturesForTesting(containers, "camel-ahc", "feautre-camel", "camel-ahc");
                prepareFeaturesForTesting(containers, "camel-amqp", "feautre-camel", "camel-amqp");
                prepareFeaturesForTesting(containers, "camel-atom", "feautre-camel", "camel-atom");
                prepareFeaturesForTesting(containers, "camel-aws", "feautre-camel", "camel-aws");
                prepareFeaturesForTesting(containers, "camel-bam", "feautre-camel", "camel-bam");
                prepareFeaturesForTesting(containers, "camel-bean-validator", "feautre-camel", "camel-bean-validator");
                prepareFeaturesForTesting(containers, "camel-bindy", "feautre-camel", "camel-bindy");
                prepareFeaturesForTesting(containers, "camel-cometd", "feautre-camel", "camel-cometd");
                prepareFeaturesForTesting(containers, "camel-csv", "feautre-camel", "camel-csv");
                prepareFeaturesForTesting(containers, "camel-dozer", "feautre-camel", "camel-dozer");
                prepareFeaturesForTesting(containers, "camel-eventadmin", "feautre-camel", "camel-eventadmin");
                prepareFeaturesForTesting(containers, "camel-exec", "feautre-camel", "camel-exec");
                prepareFeaturesForTesting(containers, "camel-flatpack", "feautre-camel", "camel-flatpack");
                prepareFeaturesForTesting(containers, "camel-freemarker", "feautre-camel", "camel-freemarker");
                prepareFeaturesForTesting(containers, "camel-ftp", "feautre-camel", "camel-ftp");
                prepareFeaturesForTesting(containers, "camel-guice", "feautre-camel", "camel-guice");
                prepareFeaturesForTesting(containers, "camel-groovy", "feautre-camel", "camel-groovy");
                prepareFeaturesForTesting(containers, "camel-hazelcast", "feautre-camel", "camel-hazelcast");
                prepareFeaturesForTesting(containers, "camel-hawtdb", "feautre-camel", "camel-hawtdb");
                prepareFeaturesForTesting(containers, "camel-hdfs", "feautre-camel", "camel-hdfs");
                prepareFeaturesForTesting(containers, "camel-hl7", "feautre-camel", "camel-hl7");
                prepareFeaturesForTesting(containers, "camel-ibatis", "feautre-camel", "camel-ibatis");
                prepareFeaturesForTesting(containers, "camel-irc", "feautre-camel", "camel-irc");
                prepareFeaturesForTesting(containers, "camel-jackson", "feautre-camel", "camel-jackson");
                prepareFeaturesForTesting(containers, "camel-jasypt", "feautre-camel", "camel-jasypt");
                prepareFeaturesForTesting(containers, "camel-jaxb", "feautre-camel", "camel-jaxb");
                prepareFeaturesForTesting(containers, "camel-jclouds", "feautre-camel", "camel-jclouds");
                prepareFeaturesForTesting(containers, "camel-jcr", "feautre-camel", "camel-jcr");
                prepareFeaturesForTesting(containers, "camel-jing", "feautre-camel", "camel-jing");
                prepareFeaturesForTesting(containers, "camel-jibx", "feautre-camel", "camel-jibx");
                prepareFeaturesForTesting(containers, "camel-jdbc", "feautre-camel", "camel-jdbc");
                prepareFeaturesForTesting(containers, "camel-josql", "feautre-camel", "camel-josql");
                prepareFeaturesForTesting(containers, "camel-josql", "feautre-camel", "camel-josql");
                prepareFeaturesForTesting(containers, "camel-jpa", "feautre-camel", "camel-jpa");
                prepareFeaturesForTesting(containers, "camel-jxpath", "feautre-camel", "camel-jxpath");
                prepareFeaturesForTesting(containers, "camel-juel", "feautre-camel", "camel-juel");
                prepareFeaturesForTesting(containers, "camel-kestrel", "feautre-camel", "camel-kestrel");
                prepareFeaturesForTesting(containers, "camel-krati", "feautre-camel", "camel-krati");
                prepareFeaturesForTesting(containers, "camel-ldap", "feautre-camel", "camel-ldap");
                prepareFeaturesForTesting(containers, "camel-lucene", "feautre-camel", "camel-lucene");
                prepareFeaturesForTesting(containers, "camel-mail", "feautre-camel", "camel-mail");
                prepareFeaturesForTesting(containers, "camel-msv", "feautre-camel", "camel-msv");
                prepareFeaturesForTesting(containers, "camel-mvel", "feautre-camel", "camel-mvel");
                prepareFeaturesForTesting(containers, "camel-mybatis", "feautre-camel", "camel-mybatis");
                prepareFeaturesForTesting(containers, "camel-nagios", "feautre-camel", "camel-nagios");
                prepareFeaturesForTesting(containers, "camel-netty", "feautre-camel", "camel-netty");
                prepareFeaturesForTesting(containers, "camel-ognl", "feautre-camel", "camel-ognl");
                prepareFeaturesForTesting(containers, "camel-paxlogging", "feautre-camel", "camel-paxlogging");
                prepareFeaturesForTesting(containers, "camel-printer", "feautre-camel", "camel-printer");
                prepareFeaturesForTesting(containers, "camel-protobuf", "feautre-camel", "camel-protobuf");
                prepareFeaturesForTesting(containers, "camel-quartz", "feautre-camel", "camel-quartz");
                prepareFeaturesForTesting(containers, "camel-quickfix", "feautre-camel", "camel-quickfix");
                prepareFeaturesForTesting(containers, "camel-restlet", "feautre-camel", "camel-restlet");
                prepareFeaturesForTesting(containers, "camel-rmi", "feautre-camel", "camel-rmi");
                prepareFeaturesForTesting(containers, "camel-routebox", "feautre-camel", "camel-routebox");
                prepareFeaturesForTesting(containers, "camel-ruby", "feautre-camel", "org.jruby.jruby");
                prepareFeaturesForTesting(containers, "camel-rss", "feautre-camel", "camel-rss");
                prepareFeaturesForTesting(containers, "camel-saxon", "feautre-camel", "camel-saxon");
                prepareFeaturesForTesting(containers, "camel-scala", "feautre-camel", "camel-scala");
                prepareFeaturesForTesting(containers, "camel-script", "feautre-camel", "camel-script");
                prepareFeaturesForTesting(containers, "camel-sip", "feautre-camel", "camel-sip");
                prepareFeaturesForTesting(containers, "camel-shiro", "feautre-camel", "camel-shiro");
                prepareFeaturesForTesting(containers, "camel-smpp", "feautre-camel", "camel-smpp");
                prepareFeaturesForTesting(containers, "camel-snmp", "feautre-camel", "camel-snmp");
                prepareFeaturesForTesting(containers, "camel-soap", "feautre-camel", "camel-soap");
                prepareFeaturesForTesting(containers, "camel-solr", "feautre-camel", "camel-solr");
                prepareFeaturesForTesting(containers, "camel-spring-integration", "feautre-camel", "camel-spring-integration");
                prepareFeaturesForTesting(containers, "camel-spring-javaconfig", "feautre-camel", "camel-spring-javaconfig");
                prepareFeaturesForTesting(containers, "camel-spring-security", "feautre-camel", "camel-spring-security");
                prepareFeaturesForTesting(containers, "camel-spring-ws", "feautre-camel", "camel-spring-ws");
                prepareFeaturesForTesting(containers, "camel-sql", "feautre-camel", "camel-sql");
                prepareFeaturesForTesting(containers, "camel-stax", "feautre-camel", "camel-stax");
                prepareFeaturesForTesting(containers, "camel-stream", "feautre-camel", "camel-stream");
                prepareFeaturesForTesting(containers, "camel-string-template", "feautre-camel", "org.apache.servicemix.bundles.stringtemplate");
                prepareFeaturesForTesting(containers, "camel-syslog", "feautre-camel", "camel-syslog");
                prepareFeaturesForTesting(containers, "camel-tagsoup", "feautre-camel", "camel-tagsoup");
                prepareFeaturesForTesting(containers, "camel-velocity", "feautre-camel", "camel-velocity");
                prepareFeaturesForTesting(containers, "camel-xmlbeans", "feautre-camel", "camel-xmlbeans");
                prepareFeaturesForTesting(containers, "camel-xmlsecurity", "feautre-camel", "camel-xmlsecurity");
                prepareFeaturesForTesting(containers, "camel-xmpp", "feautre-camel", "camel-xmpp");
                prepareFeaturesForTesting(containers, "camel-xstream", "feautre-camel", "camel-xstream");
                prepareFeaturesForTesting(containers, "camel-zookeeper", "feautre-camel", "camel-zookeeper");

                //prepareFeaturesForTesting(containers, "camel-crypto", "feautre-camel", "camel-crypto");
                //prepareFeaturesForTesting(containers, "camel-script camel-script-jruby", "feautre-camel", "camel-script-jruby");
                //prepareFeaturesForTesting(containers, "camel-script camel-script-javascript", "feautre-camel", "camel-script-javascript");
                //prepareFeaturesForTesting(containers, "camel-script camel-script-groovy", "feautre-camel", "camel-script-groovy");

                assertFeatures(fabricService, curator);
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }
}
