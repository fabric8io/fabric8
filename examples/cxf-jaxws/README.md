# CXF JAX-RS EXAMPLE

## Overview
This example demonstrates how to create a web service with CXF and expose it through the OSGi HTTP Service.

## What You Will Learn
In studying this example you will learn:

* Configure the JAXWS web services by using the blueprint configuration file.

  The web service is implemented in the CustomerService.java file, which is
located in the src/main/java/org/fusesource/examples/cxf/jaxws directory of this example. It contains annotations indicating what URIs and HTTP methods to use when accessing the resource. 

  The blueprint file, located in the src/main/resources/OSGi-INF/blueprint/blueprint.xml
 directory:

  1. Imports the configuration files needed to enable CXF to support
   JAX-RS and to use the OSGi HTTP service.

   2. Configures the web service, as follows:

         <jaxws:endpoint id="helloWorld"
           implementor="org.fusesource.examples.cxf.jaxws.HelloWorldImpl"
           address="/HelloWorld"/>



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

1. If you have already run the example using the prebuilt version as described above, you must first uninstall the examples-cxf-jaxws feature by entering the following command in the ServiceMix console:

        features:uninstall examples-cxf-jaxws

2. Install the example by entering the following command in
   the Fuse ESB console:

        features:install examples-cxf-jaxws

   It makes use of the Fuse ESB features facility. For more information about the features facility, see the README.md file in the examples parent directory.
   
### Running a client 

To view the service WSDL, open your browser and go to the followingURL:

	http://localhost:8181/cxf/HelloWorld?wsdl
	
The latter URI can be used to see the desription of multiple root resource classes.

You can see the services listing at http://localhost:8181/cxf.

You can make invocations on the web service in several ways, including using a web client, using a Java client and using a command-line utility such a curl or Wget. See below for more details.

#### To run a web client:
1. Open the client.html, which is located in the same directory as this README file, in your favorite browser.

2. Click the Send button to send a request.

   Once the request has been successfully sent, a response similar to the following should appear in the right-hand panel of the web page:
   
         STATUS: 200
    
         <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
           <soap:Body><ns2:sayHiResponse xmlns:ns2="http://jaxws.cxf.examples.
            fusesource.org/"><return>Hello John Doe</return>
           </ns2:sayHiResponse>
          </soap:Body>
        </soap:Envelope>

Note, if you use Safari, right click the window and select 'Show Source'.

#### To run a Java client:
1. Change to the <esb_home>/examples/cxf-jaxws directory.

2. Run the following command:

        mvn compile exec:java
        
   If the client request is successful, a response similar to the following should appear in the  console:

        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
           <soap:Body><ns2:sayHiResponse xmlns:ns2="http://jaxws.cxf.examples.
            fusesource.org/"><return>Hello John Doe</return>
           </ns2:sayHiResponse>
          </soap:Body>
        </soap:Envelope>



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