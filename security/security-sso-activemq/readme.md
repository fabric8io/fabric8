#fabric-security-sso-activemq

## Description

A single sign-on JAAS module and plugin for ActiveMQ 5.x that delegates authentication and authorization to an OpenAM Server.

## Usage

SSO support for ActiveMQ provided by this module is in two parts, a JAAS login module that performs the initial authentication with OpenAM and an ActiveMQ broker plugin that performs authorization on the returned token for various actions.  So a client connection is authenticated and authorized much like in the JAAS modules shipped with ActiveMQ, however the LoginModule simply delegates this to OpenAM, creating a security Subject that contains the returned token.  That token is then used to perform subsequent authorization of various actions such as creating a producer or consumer on a destination or sending a message.  When an authorized producer sends a message to the broker the broker will add that producer's security token to the message, allowing that message to be authorized by downsteam consumers and passed on to other applications.  A producer can also choose to supply a different token than it's own, in which case the broker will just authorize the token and pass it along.

To use this module you need to copy the jar from this project and fabric-security-sso-client into `$ACTIVEMQ_HOME/lib`.  The LoginModule needs an appropriate login.config, such as (these are the defaults):

    RestSSOLogin {
        io.fabric8.security.sso.activemq.OpenAMLoginModule required
          OpenAMRealm="/"
          OpenAMService="activemq"
          OpenAMHostName="localhost"
          OpenAMPort="8080"
          OpenAMMethod="http"
          OpenAMURLPrefix="opensso"
          ServicePrefix="http://localhost:61616/activemq"
          debug=true;
    };

This should go in your classpath.  Then in your activemq.xml configuration you need to configure the REST client and broker plugin:

    <!-- REST client configuration, used by the broker plugin -->
    <bean id="OpenAMRestClient" class="io.fabric8.security.sso.client.OpenAMRestClient">
        <property name="OpenAMRealm value="/"/>
        <property name="OpenAMService" value="activemq"
        <property name="OpenAMHostName" value="localhost"
        <property name="OpenAMPort" value="8080"
        <property name="OpenAMMethod" value="http"
        <property name="OpenAMURLPrefix" value="opensso"
        <property name="ServicePrefix" value="http://localhost:61616/activemq"
        <property name="debug" value="true"/>
    </bean>


    <!-- This would go in the "broker" section of the configuration -->

    <plugins>
        <bean id="openAMAuthPlugin" class="io.fabric8.security.sso.activemq.OpenAMAuthenticationPlugin" xmlns="http://www.springframework.org/schema/beans">
            <property name="configuration" value="RestSSOLogin"/> <!-- configuration name in the login.config file -->
            <property name="client" ref="OpenAMRestClient"/> <!-- reference to the REST client bean -->
            <property name="authorizeSend" value="true"/> <!-- whether or not to authorize every incoming message or not -->
        </bean>
    </plugins>

The plugin authorizes stuff with OpenAM based on URLs, for example:

* Login          `http://localhost:61616/activemq/login`
* Add Consumer	 `http://localhost:61616/activemq/addConsumer*`
* Add Producer	 `http://localhost:61616/activemq/addProducer*`
* Send	         `http://localhost:61616/activemq/send*`
* Advisories	   `http://localhost:61616/activemq/addConsumer/Topic/Activemq.Advisory*`
*                `http://localhost:61616/activemq/addProducer/Topic/ActiveMQ.Advisory*`

So you create URL policies in OpenAM for the above base URLs, in all cases the plugin will be checking for "GET" access.  For addConsumer, addProducer and send the plugin checks the destination type and name, so for a queue called FOO.BAR the full URL would look like:

`http://localhost:61616/activemq/addProducer/Queue/FOO.BAR`

composite destinations are also supported, however the producer or consumer must be authorized for every destination in the composite.

This all of course requires a working OpenAM setup --> https://wikis.forgerock.org/confluence/display/openam/OpenAM+Installation+Guide

And of course plain HTTP should not be used in any kind of environment other than development, HTTPS with mutual authentication should be used to ensure trust between the plugin and the OpenAM server.

