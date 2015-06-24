## API Management

The API Management component provides a highly available Gateway that sits in between your
clients and your APIs.  This Gateway adds governance and metrics to any integration service
you choose.

This API Management functionality is provided by integrating the [apiman](http://apiman.io)
project into fabric8.  To learn more about its capabilities and how to use it, please
refer to the [apiman User Guide](http://www.apiman.io/latest/user-guide.html) and other
[tutorial](http://www.apiman.io/latest/tutorials.html) resources on the apiman project site.

Some common use-cases addressed by API Management are:

* *Quotas/Rate Limiting* : limit the number of requests users or clients are allowed to send to the service.
* *Security* : add security (e.g. BASIC authentication, IP Address white/blacklisting) to your services.
* *Metrics* : track who is using your services (number of requests), when they use them, and how.


### Requirements

For API Management to function, you must run the **apiman** app.  This app provides the API
Management configuration layer, which consists of a REST layer and a User Interface.

In addition, for the configuration layer and the Gateway to work properly, an elasticsearch
app must also be running.  The API Management component stores configuration information in
elasticsearch.


### Using API Management

* In the [Console](console.html) click on the **Library** tab and then navigate into **apps** and then click on **API Management**
* Click on the **Run** button on the top right to run the **API Management** service
