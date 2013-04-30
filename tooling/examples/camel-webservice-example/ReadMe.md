Camel WebService - Customers Example
====================================

To build this project use

    mvn install

To run this project use

    mvn camel:run

Then start SOAP UI client (http://www.soapui.org/) and create a new WebService project using wsdl address

    http://0.0.0.0:9191/camel-example/WebService?wsdl

Next test the 3 operations exposed by the WebService : getAllCustomers, getCustomerByName, SaveCustomer