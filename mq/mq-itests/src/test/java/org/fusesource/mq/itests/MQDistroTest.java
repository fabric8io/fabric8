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

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.karaf.tooling.exam.options.LogLevelOption;
import org.fusesource.tooling.testing.pax.exam.karaf.FuseTestSupport;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import javax.jms.ConnectionFactory;
import javax.jms.Session;
import javax.jms.TextMessage;

import java.io.File;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.*;
import static org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator.getOsgiService;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
@Ignore("[FABRIC-524] Fix mq-itests")
public class MQDistroTest extends FuseTestSupport {

    static final String MQ_GROUP_ID = "org.jboss.amq";
    static final String MQ_ARTIFACT_ID = "jboss-a-mq";
    static final String WEB_CONSOLE_URL = "http://localhost:8181/activemqweb/";
    public static final String USER_NAME_ND_PASSWORD = "fusemq";

    @Test
    public void testMQ() throws Exception {

        // send message via webconsole, consume from jms openwire
        HttpClient client = new HttpClient();

        // set credentials
        client.getState().setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(USER_NAME_ND_PASSWORD, USER_NAME_ND_PASSWORD)
         );

        // need to first get the secret
        GetMethod get = new GetMethod(WEB_CONSOLE_URL + "send.jsp");
        get.setDoAuthentication(true);

        // Give console some time to start
        for (int i=0; i<20; i++) {
            Thread.currentThread().sleep(1000);
            try {
                i = client.executeMethod(get);
            } catch (java.net.ConnectException ignored) {}
        }
        assertEquals("get succeeded on " + get, 200, get.getStatusCode());

        String response = get.getResponseBodyAsString();
        final String secretMarker = "<input type=\"hidden\" name=\"secret\" value=\"";
        String secret = response.substring(response.indexOf(secretMarker) + secretMarker.length());
        secret = secret.substring(0, secret.indexOf("\"/>"));

        final String destination = "validate.console.send";
        final String content = "Hi for the " + Math.random() + "' time";

        PostMethod post = new PostMethod(WEB_CONSOLE_URL + "sendMessage.action");
        post.setDoAuthentication(true);
        post.addParameter("secret", secret);

        post.addParameter("JMSText", content);
        post.addParameter("JMSDestination", destination);
        post.addParameter("JMSDestinationType", "queue");

        // execute the send
        assertEquals("post succeeded, " + post, 302, client.executeMethod(post));

        // consume what we sent
        ActiveMQConnection connection = (ActiveMQConnection) new ActiveMQConnectionFactory().createConnection(USER_NAME_ND_PASSWORD, USER_NAME_ND_PASSWORD);
        connection.start();
        try {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            TextMessage textMessage = (TextMessage) session.createConsumer(new ActiveMQQueue(destination)).receive(10*1000);
            assertNotNull("got a message", textMessage);
            assertEquals("it is ours", content, textMessage.getText());
        } finally {
            connection.close();
        }

        // verify osgi registration of cf
        ConnectionFactory connectionFactory = getOsgiService(ConnectionFactory.class);
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

    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                mqDistributionConfiguration(), keepRuntimeFolder(),
                mavenBundle("commons-httpclient", "commons-httpclient").versionAsInProject().type("jar"),
                logLevel(LogLevelOption.LogLevel.INFO)
        };
    }

    protected Option mqDistributionConfiguration() {
        return new DefaultCompositeOption(
                new Option[]{karafDistributionConfiguration().frameworkUrl(
                maven().groupId(MQ_GROUP_ID).artifactId(MQ_ARTIFACT_ID).versionAsInProject().type("zip"))
                        .karafVersion("2.2.2").name("Fabric MQ Distro").unpackDirectory(new File("target/paxexam/unpack/")),
                mavenBundle("org.fusesource.tooling.testing","pax-exam-karaf", MavenUtils.getArtifactVersion("org.fusesource.tooling.testing", "pax-exam-karaf")),
                mavenBundle("org.fusesource.fabric.itests", "fabric-pax-exam", MavenUtils.getArtifactVersion("org.fusesource.fabric.itests", "fabric-pax-exam")),
                useOwnExamBundlesStartLevel(50),
                editConfigurationFilePut("etc/users.properties", "fusemq", "fusemq,admin"),
                editConfigurationFilePut("etc/system.properties", "activemq.jmx.user", "fusemq"),
                editConfigurationFilePut("etc/system.properties", "activemq.jmx.password", "fusemq"),
                editConfigurationFilePut("etc/config.properties", "karaf.startlevel.bundle", "50")});
    }

}
