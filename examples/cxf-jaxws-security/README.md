# CXF JAXWS WS-SECURITY EXAMPLE

## Overview
This example demonstrates how to Create a web service with CXF using WS-SECURITY and blueprint configuration, and expose it through the OSGi HTTP Service.

## What You Will Learn
In studying this example you will learn:

* Configure the JAXWS web services by using the blueprint configuration file.

  The web service is implemented in the CustomerService.java file, which is
located in the src/main/java/org/fusesource/examples/jaxws/security directory of this example.

  The blueprint file, located in the src/main/resources/OSGi-INF/blueprint directory:


  Configures the web service, as follows:
     
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

* Generating the FAB file by configuring the FAB plugin like this 

        <plugin>
            <groupId>org.fusesource.mvnplugins</groupId>
            <artifactId>maven-fab-plugin</artifactId>
            <version>${maven-fab-plugin-version}</version>
            <configuration>
                <descriptor>
                    <Long-Description />
                </descriptor>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>


## Prerequisites
Before building and running this example you need:

1.You must have the following installed on your machine:

* JDK 1.6 or higher
* Maven 2.2.1 or higher

2.Start Fuse ESB by running the following command:

* <esb_home>/bin/fuseesb          (on UNIX) 
* <esb_home>\bin\fuseesb          (on Windows)

## Building the Example
To Build the example by opening a command prompt, changing directory to examples/cxf-jaxws-security (this example) and entering the following Maven command:

        mvn install

   If all of the required OSGi bundles are available in your local Maven repository, the example will build very quickly. Otherwise it may take some time for Maven to download everything it needs.
   
   The mvn install command builds the example deployment bundle and copies it to your local Maven repository and to the target directory of this example.

## Running the Example

You can run the example:

1. If you have already run the example using the prebuilt version as described above, you must first uninstall the examples-cxf-jaxws-security feature by entering the following command in the ServiceMix console:

        features:uninstall examples-cxf-jaxws-security

2. Install the example by entering the following command in
   the ServiceMix console:

        features:install examples-cxf-jaxws-security

   It makes use of the fuse ESB features facility. For more information about the features facility, see the README.md file in the examples parent directory.
   
### Running a client 

To view the service WSDL, open your browser and go to the following
URL:

    http://localhost:8181/cxf/HelloWorldSecurity?wsdl

Note, if you use Safari, right click the window and select 'Show Source'.

The latter URI can be used to see the desription of multiple root resource classes.

You can see the services listing at http://localhost:8181/cxf.

You can make invocations on the web service through a web client, using a Java client. See below for more details.


#### To run a web client:
To run the web client: 

Open the client.html, which is located in the same directory as this README file, in your favorite browser.

Click the Send button to send a request.

Once the request has been successfully sent, a response similar to the following should appear in the right-hand panel of the web page:
   
    STATUS: 200
    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
      <soap:Body><ns2:sayHiResponse xmlns:ns2="http://security.jaxws.cxf.examples.fusesource.org/"><return>Hello John Doe</return>
      </ns2:sayHiResponse>
      </soap:Body>
    </soap:Envelope>

Note, if you use Safari, right click the window and select 'Show Source'.

#### To run a Java client:
1. Change to the <esb_home>/examples/cxf-jaxrs
  directory.

2. Run the following command:

        mvn compile exec:java
        
   It makes a sequence of JAXWS invocations and displays the results.


### Changing /cxf servlet alias

By default CXF Servlet is assigned a '/cxf' alias. You can
change it in a couple of ways

1. Add org.apache.cxf.osgi.cfg to the /etc directory and set the 'org.apache.cxf.servlet.context' property, for example:

        org.apache.cxf.servlet.context=/custom

2. Use shell config commands, for example:

        config:edit org.apache.cxf.osgi
     
        config:propset org.apache.cxf.servlet.context /super
     
        config:update



## More information
For more information see:
* link to ref 1
* link to ref 2