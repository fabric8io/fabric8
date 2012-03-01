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

CXF JAX-RS Security EXAMPLE
==================

Purpose
-------
Create a RESTful JAX-RS web service using CXF and expose it with the 
OSGi HTTP Service. The Services is protected with the HTTP Basic
Authentication.


Explanation
-----------
The web service is implemented in the CustomerService.java file, which is
located in the src/main/java/org/fusesource/examples/cxf/jaxrs/security
directory of this example. It contains annotations indicating what URIs
and HTTP methods to use when accessing the resource. For information
on how to write RESTful web services, please refer to the Apache CXF
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

    <bean id="customerSvc" class="org.fusesource.examples.cxf.jaxrs.security.CustomerService"/>
    
    <bean id="authenticationInterceptor" class="org.apache.cxf.interceptor.security.JAASLoginInterceptor">
       <property name="contextName" value="karaf"/>
    </bean>
   This will leverage cxf JAASLoginInterceptor to authenticate against karaf default jaas configuration
    through property contextName, which store username/password/role in ESB_HOME/etc/users.properties, 
    to run this example, need add joe=password in etc/users.properties. Users can easily change to 
    use other jaas context(JDBC,LDAP etc) as described from 
    http://karaf.apache.org/manual/2.2.4/developers-guide/security-framework.html.

   

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

  features:install examples-cxf-jaxrs
  
This command makes use of the ServiceMix features facility. For
more information about the features facility, see the README.txt
file in the examples parent directory.

Running a Client
----------------

You can browse WSDL at:

http://localhost:8181/cxf/crm/customerservice?_wadl&_type=xml

or

http://localhost:8181/cxf/crm?_wadl&_type=xml

The latter URI can be used to see the desription of multiple root
resource classes.

You can see the services listing at http://localhost:8181/cxf.

You can make invocations on the web service in several ways, including
using a web client, using a Java client and using a command-line
utility such a curl or Wget. See below for more details.

(a) To run a web client:
    -------------------
Open a browser and go to the following URL:

   http://localhost:8181/cxf/crm/customerservice/customers/123

It should display an XML representation about the SecurityException as the 
service needs basic authentication information.

Note, if you use Safari, right click the window and select 'Show Source'.

(b) To run a Java client:
    --------------------
- Change to the <esb_home>/examples/cxf-jaxrs
  directory.

- Run the following command:
    
    mvn compile exec:java
 
  It makes a sequence of RESTful invocations and displays the
  results.

(c) To run a command-line utility:
    -----------------------------
You can use a command-line utility, such as curl or Wget, to make 
the invocations. For example, try using curl as follows:

- Open a command prompt and change directory to
  <esb_home>/examples/cxf-jaxrs-security.
  
- Run the following curl commands:

  # Create a customer
  # 
  #
  curl --basic -u joe:password -X POST -T src/main/resources/org/fusesource/examples/cxf/jaxrs/client/add_customer.xml -H "Content-Type: text/xml" http://localhost:8181/cxf/crm/customerservice/customers

  # Retrieve the customer instance with id 123
  #
  curl --basic -u joe:password http://localhost:8181/cxf/crm/customerservice/customers/123
 
  # Update the customer instance with id 123
  #
  curl --basic -u joe:password -X PUT -T src/main/resources/org/fusesource/examples/cxf/jaxrs/client/update_customer.xml -H "Content-Type: text/xml" http://localhost:8181/cxf/crm/customerservice/customers
  
  # Delete the customer instance with id 123
  #
  curl --basic -u joe:password -X DELETE http://localhost:8181/cxf/crm/customerservice/customers/123


Changing /cxf servlet alias
---------------------------
By default CXF Servlet is assigned a '/cxf' alias. You can
change it in a couple of ways

a. Add org.apache.cxf.osgi.cfg to the /etc directory and set the
   'org.apache.cxf.servlet.context' property, for example:
   
   org.apache.cxf.servlet.context=/custom

b. Use shell config commands, for example:

     config:edit org.apache.cxf.osgi   
     config:propset org.apache.cxf.servlet.context /super
     config:update
  

B. Building the Example Bundle Yourself
---------------------------------------
To install and run the example where you build the example bundle
yourself, complete the following steps:

1. If you have already run the example using the prebuilt version as
   described above, you must first uninstall the examples-cxf-jaxrs-security
   feature by entering the following command in the ServiceMix console:

     features:uninstall examples-cxf-jaxrs-security

2. Build the example by opening a command prompt, changing directory to
   examples/cxf-jaxrs-security (this example) and entering the following Maven
   command:

     mvn install
   
   If all of the required OSGi bundles are available in your local Maven
   repository, the example will build very quickly. Otherwise it may
   take some time for Maven to download everything it needs.
   
   The mvn install command builds the example deployment bundle and
   copies it to your local Maven repository and to the target directory
   of this example.
     
3. Install the example by entering the following command in
   the ServiceMix console:
   
     features:install examples-cxf-jaxrs-security
       
   It makes use of the ServiceMix features facility. For more information
   about the features facility, see the README.txt file in the examples
   parent directory.
     
See "Running a Client" above for information on how to make invocations
on the web service.


Stopping and Uninstalling the Example
-------------------------------------
To stop the example, you must first know the bundle ID that ServiceMix
has assigned to it. To get the bundle ID, enter the following command
in the ServiceMix console (Note, the text you are typing will intermingle
with the output being logged. This is nothing to worry about.):

  osgi:list

At the end of the listing, you should see an entry similar to the
following:

  [159] [Active     ] [Started] [  60] FuseSource :: Examples :: CXF JAX-RS Security (7.0.0)

In this case, the bundle ID is 159.

To stop the example, enter the following command in the ServiceMix
console:

  osgi:stop <bundle_id>

For example:

  osgi:stop 159

To uninstall the example, enter one of the following commands at
the ServiceMix console:

  features:uninstall examples-cxf-jaxrs-security
 
or
 
  osgi:uninstall <bundle_id>
  

Viewing the Log Entries
-----------------------
You can view the entries in the log file in the data/log
directory of your Fuse ESB installation, or by typing
the following command in the Fuse ESB console:

  log:display
