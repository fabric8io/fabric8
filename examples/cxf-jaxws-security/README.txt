/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

CXF JAXWS WS-SECURITY EXAMPLE
=========================

Purpose
-------
Create a web service with CXF using WS-SECURITY and blueprint configuration,
and expose it through the OSGi HTTP
Service, then it will leverage cxf JAASLoginInterceptor to authenticate against karaf
default jaas configuration.


Explanation
-----------
The web service is a simple JAX-WS web service with ws-security UsernameToken action called HelloWorldSecurity. The 
interface and the implementation are located in the src/main/java/org/
fusesource/examples/cxf/jaxws/security directory of this example.

The blueprint.xml file, located in the src/main/resources/OSGI-INF/blueprint
directory:


1. Configures the web service endpoint as follows:

    <jaxws:endpoint id="helloWorld"
        implementor="org.fusessource.examples.cxf.jaxws.security.HelloWorldImpl"
        address="/HelloWorldSecurity">
        <jaxws:inInterceptors>
            <bean class="org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor">
                 <property name="properties">
                     <map>
                         <entry key="action" value="UsernameToken"/>
                         <entry key="passwordType" value="PasswordTest"/>
                     </map>
                 </property>
             </bean>          
            <ref component-id="authenticationInterceptor"/>
        </jaxws:inInterceptors>
        <jaxws:properties>
            <entry key="ws-security.validate.token" value="false"/>
        </jaxws:properties>
    </jaxws:endpoint>
    <bean id="authenticationInterceptor" class="org.apache.cxf.interceptor.security.JAASLoginInterceptor">
       <property name="contextName" value="karaf"/>
    </bean>

This will leverage cxf JAASLoginInterceptor to authenticate against karaf default jaas configuration through property contextName, which store username/password/role in ESB_HOME/etc/users.properties, to run this example, need add joe=password in etc/users.properties. Users can easily change to use other jaas context(JDBC,LDAP etc) as described from http://karaf.apache.org/manual/2.2.4/developers-guide/security-framework.html.


Prerequisites for Running the Example
-------------------------------------
1. You must have the following installed on your machine:

   - JDK 1.6 or higher
   
   - Maven 2.2.1 or higher
   
  For more information, see the README in the top-level examples
  directory.


2. Start Fuse ESB by running the following command:

  <esb_home>/bin/fuseesb          (on UNIX)
  <esb_home>\bin\fuseesb          (on Windows)


Running the Example
-------------------
You can run the example in two ways:

- A. Using a Prebuilt Deployment Bundle: Quick and Easy
This option is useful if you want to see the example up and
running as quickly as possible.

- B. Building the Example Bundle Yourself
This option is useful if you want to change the example in any
way. It tells you how to build and deploy the example. This
option might be slower than option A because, if you do not
already have the required bundles in your local Maven
repository, Maven will have to download the bundles it needs.

A. Using a Prebuilt Deployment Bundle: Quick and Easy
-----------------------------------------------------
To install and run a prebuilt version of this example, enter
the following command in the ServiceMix console:

  features:install examples-cxf-jaxws-security
  
This command makes use of the ServiceMix features facility. For
more information about the features facility, see the README.txt
file in the examples parent directory.

To view the service WSDL, open your browser and go to the following
URL:

  http://localhost:8181/cxf/HelloWorldSecurity?wsdl

Note, if you use Safari, right click the window and select
'Show Source'.

Running a Client
----------------
To run the web client:

1. Open the client.html, which is located in the same directory as
   this README file, in your favorite browser.

2. Click the Send button to send a request.

   Once the request has been successfully sent, a response similar
   to the following should appear in the right-hand panel of the
   web page:
   
   STATUS: 200
   <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
     <soap:Body><ns2:sayHiResponse xmlns:ns2="http://cxf.examples.
     servicemix.apache.org/"><return>Hello John Doe</return>
     </ns2:sayHiResponse>
     </soap:Body>
   </soap:Envelope>

To run the java code client:

1. Change to the <esb_home>/examples/cxf-jaxws-security
   directory.

2. Run the following command:

     mvn compile exec:java

   If the client request is successful, 
   it will print out
       Hello ffang
   in the ServiceMix console:


Changing /cxf servlet alias
---------------------------
By default CXF Servlet is assigned a '/cxf' alias. You can
change it in a couple of ways

a. Add org.apache.cxf.osgi.cfg to the /etc directory and set
   the 'org.apache.cxf.servlet.context' property, for example:
   
     org.apache.cxf.servlet.context=/custom
   
b. Use shell config commands, for example :
   
     config:edit org.apache.cxf.osgi   
     config:propset org.apache.cxf.servlet.context /super
     config:update


B. Building the Example Bundle Yourself
---------------------------------------
To install and run the example where you build the example bundle
yourself, complete the following steps:

1. If you have already run the example using the prebuilt version as
   described above, you must first uninstall the examples-cxf-jaxws-security
   feature by entering the following command in the ServiceMix console:

     features:uninstall examples-cxf-jaxws-security

   
2. Build the example by opening a command prompt, changing directory to
   examples/cxf-jaxws-security (this example) and entering the following Maven
   command:

     mvn install
   
   If all of the required OSGi bundles are available in your local
   Maven repository, the example will build very quickly. Otherwise
   it may take some time for Maven to download everything it needs.
   
   The mvn install command builds the example deployment bundle and
   copies it to your local Maven repository and to the target directory
   of this example.
     
3. Install the example by entering the following command in
   the ServiceMix console:
   
     features:install examples-cxf-jaxws-security
       
   It makes use of the ServiceMix features facility. For more
   information about the features facility, see the README.txt file
   in the examples parent directory.

To view the service WSDL, open your browser and go to the following
URL:

  http://localhost:8181/cxf/HelloWorldSecurity?wsdl

Note, if you use Safari, right click the window and select
'Show Source'.

You can try running a client against your service by following the
instructions in the "Running a Client" section above.


Stopping and Uninstalling the Example
-------------------------------------
To stop the example, you must first know the bundle ID that ServiceMix
has assigned to it. To get the bundle ID, enter the following command
at the ServiceMix console:

  osgi:list

At the end of the listing, you should see an entry similar to the
following:

  [170] [Active     ] [Started] [  60] Apache ServiceMix Example :: CXF WS SECURITY OSGI (4.2.0.0)

In this case, the bundle ID is 170.

To stop the example, enter the following command at the ServiceMix
console:

  osgi:stop <bundle_id>

For example:

  osgi:stop 170

To uninstall the example, enter one of the following commands in
the ServiceMix console:

  features:uninstall examples-cxf-jaxws-security
 
or
 
  osgi:uninstall <bundle_id>
  

Viewing the Log Entries
-----------------------
You can view the entries in the log file in the data/log
directory of your ServiceMix installation, or by typing
the following command in the ServiceMix console:

  log:display
