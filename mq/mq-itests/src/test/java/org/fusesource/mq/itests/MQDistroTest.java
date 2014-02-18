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
package org.fusesource.mq.itests;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.keepRuntimeFolder;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.fabric8.api.ServiceLocator;

import java.io.File;

import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.karaf.tooling.exam.options.LogLevelOption;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.BundleContext;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class MQDistroTest extends MQTestSupport {

    static final String JOLOKIA_URL = "http://localhost:8181/hawtio/jolokia/";
    static final String BROKER_MBEAN = "org.apache.activemq:type=Broker,brokerName=amq";

    public static final String USER_NAME_ND_PASSWORD = "admin";

    @Inject
    BundleContext bundleContext;

    @Test
    public void testWebConsoleAndClient() throws Exception {
        // send message via webconsole, consume from jms openwire
        HttpClient client = new HttpClient();

        // set credentials
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(USER_NAME_ND_PASSWORD, USER_NAME_ND_PASSWORD)
         );

        GetMethod get = new GetMethod(JOLOKIA_URL + "exec/" + BROKER_MBEAN + "/addQueue/FOO");
        get.setDoAuthentication(true);
        client.executeMethod(get);
        assertEquals("destination created", 200, get.getStatusCode());

        get = new GetMethod(JOLOKIA_URL + "exec/" + BROKER_MBEAN + ",destinationType=Queue,destinationName=FOO/sendTextMessage(java.lang.String,java.lang.String,java.lang.String)/Hello/admin/admin");
        client.executeMethod(get);
        assertEquals("message sent", 200, get.getStatusCode());

        // consume what we sent
        ActiveMQConnection connection = (ActiveMQConnection) new ActiveMQConnectionFactory().createConnection(USER_NAME_ND_PASSWORD, USER_NAME_ND_PASSWORD);
        connection.start();
        try {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            TextMessage textMessage = (TextMessage) session.createConsumer(new ActiveMQQueue("FOO")).receive(10*1000);
            assertNotNull("got a message", textMessage);
            assertEquals("it is ours", "Hello", textMessage.getText());
        } finally {
            connection.close();
        }

        // verify osgi registration of cf
        ConnectionFactory connectionFactory = ServiceLocator.awaitService(bundleContext, ConnectionFactory.class);
        assertTrue(connectionFactory instanceof ActiveMQConnectionFactory);
        ActiveMQConnection connectionFromOsgiFactory = (ActiveMQConnection) connectionFactory.createConnection(USER_NAME_ND_PASSWORD, USER_NAME_ND_PASSWORD);
        connectionFromOsgiFactory.start();
        try {
            assertEquals("same broker", connection.getBrokerName(), connectionFromOsgiFactory.getBrokerName());
        } finally {
            connectionFromOsgiFactory.close();
        }

        // verify mq-client
        Process process = Runtime.getRuntime().exec("java -jar extras" + File.separator + "mq-client.jar producer --count 1 --user " + USER_NAME_ND_PASSWORD + " --password " + USER_NAME_ND_PASSWORD,
                null, // env
                new File(System.getProperty("user.dir")));
        process.waitFor();
        assertEquals("producer worked, exit(0)?", 0, process.exitValue());

        process = Runtime.getRuntime().exec("java -jar extras" + File.separator + "mq-client.jar consumer --count 1 --user " + USER_NAME_ND_PASSWORD + " --password " + USER_NAME_ND_PASSWORD,
                null, // env
                new File(System.getProperty("user.dir")));
        process.waitFor();
        assertEquals("consumer worked, exit(0)?", 0, process.exitValue());

        System.out.println(executeCommand("activemq:bstat"));
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(mqDistributionConfiguration()), keepRuntimeFolder(),
                CoreOptions.wrappedBundle(mavenBundle("commons-httpclient", "commons-httpclient").versionAsInProject().type("jar")),
                logLevel(LogLevelOption.LogLevel.INFO)
        };
    }
}
