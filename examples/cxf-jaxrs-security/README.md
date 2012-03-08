# CXF JAX-RS Security EXAMPLE

## Overview
This example demonstrates how to create a RESTful JAX-RS web service using CXF and expose it with the OSGi HTTP Service. The Services is protected with the HTTP Basic Authentication.


## What You Will Learn
In studying this example you will learn:

* Configure the JAX-RS web services by using the blueprint configuration file.

  The web service is implemented in the CustomerService.java file, which is
located in the src/main/java/org/fusesource/examples/cxf/jaxrs directory of this example. It contains annotations indicating what URIs and HTTP methods to use when accessing the resource. For information on how to write RESTful web services, please refer to the Apache CXF
documentation.

  The blueprint file, located in the src/main/resources/OSGi-INF/blueprint
 directory:

  1. Imports the configuration files needed to enable CXF to support
   JAX-RS and to use the OSGi HTTP service.

   2. Configures the web service, as follows:

        <jaxrs:server id="customerService" address="/crm">
            <jaxrs:serviceBeans>
                <ref component-id="customerSvc"/>
            </jaxrs:serviceBeans>
            <jaxrs:inInterceptors>
                <ref component-id="authenticationInterceptor"/>
            </jaxrs:inInterceptors>
        </jaxrs:server>

        <bean id="customerSvc" class="org.fusesource.examples.cxf.jaxrs.CustomerService"/>
        
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
To Build the example by opening a command prompt, changing directory to examples/cxf-jaxrs (this example) and entering the following Maven command:

        mvn install

   If all of the required OSGi bundles are available in your local Maven repository, the example will build very quickly. Otherwise it may take some time for Maven to download everything it needs.
   
   The mvn install command builds the example deployment bundle and copies it to your local Maven repository and to the target directory of this example.

## Running the Example

You can run the example:

1. If you have already run the example using the prebuilt version as described above, you must first uninstall the examples-cxf-jaxrs feature by entering the following command in the ServiceMix console:

        features:uninstall examples-cxf-jaxrs-security

2. Install the example by entering the following command in
   the ServiceMix console:

        features:install examples-cxf-jaxrs-security

   It makes use of the ServiceMix features facility. For more information about the features facility, see the README.md file in the examples parent directory.
   
### Running a client 

You can browse WSDL at:

	http://localhost:8181/cxf/crm/customerservice?_wadl&_type=xml

or

	http://localhost:8181/cxf/crm?_wadl&_type=xml

The latter URI can be used to see the desription of multiple root resource classes.

You can see the services listing at http://localhost:8181/cxf.

You can make invocations on the web service in several ways, including using a web client, using a Java client and using a command-line utility such a curl or Wget. See below for more details.

#### To run a web client:
Open a browser and go to the following URL:

    http://localhost:8181/cxf/crm/customerservice/customers/123

It should display an XML representation for authication failed.

Note, if you use Safari, right click the window and select 'Show Source'.

#### To run a Java client:
1. Change to the <esb_home>/examples/cxf-jaxrs-security
  directory.

2. Run the following command:

        mvn compile exec:java
        
   It makes a sequence of RESTful invocations and displays the results.

#### To run a command-line utility:

You can use a command-line utility, such as curl or Wget, to make the invocations. For example, try using curl as follows:

1. Open a command prompt and change directory to
  
        <esb_home>/examples/cxf-jaxrs-security

2. Run the following curl commands:
    
    * Create a customer
 
            curl --basic -u joe:password -X POST -T src/main/resources/org/fusesource/examples/cxf/jaxrs/client/add_customer.xml -H "Content-Type: text/xml" http://localhost:8181/cxf/crm/customerservice/customers
  
    * Retrieve the customer instance with id 123
    
            curl --basic -u joe:password http://localhost:8181/cxf/crm/customerservice/customers/123

    * Update the customer instance with id 123
  
            curl --basic -u joe:password -X PUT -T src/main/resources/org/fusesource/examples/cxf/jaxrs/client/update_customer.xml -H "Content-Type: text/xml" http://localhost:8181/cxf/crm/customerservice/customers

    * Delete the customer instance with id 123
  
             curl --basic -u joe:password -X DELETE http://localhost:8181/cxf/crm/customerservice/customers/123

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