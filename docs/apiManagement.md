## API Management

With a successful adaption of a micro services architecture you will soon have many services, and each of these services publishes an API that can be consumed by applications or other services. This API Management functionality is provided by integrating the [apiman](http://apiman.io) project into fabric8. The apiman application lets you import your service endpoints and their API description documents. It is in the apiman console that a service can be decorated with policies and plans. Once you are ready the service can be published to a gateway to make it available for consumption. The gateway is a standalone, highly available, application that sits in between your clients and your APIs that adds governance and metrics to any micro service.

Some common API Management use-cases are:

* *Quotas/Rate Limiting* : limit the number of requests users or clients are allowed to send to the service.
* *Security* : add security (e.g. BASIC authentication, IP Address white/blacklisting) to your services.
* *Metrics* : track who is using your services (number of requests), when they use them, and how.

### Get started with Apiman

* [Apiman Components](apimanComponents.html) - High level components; the apiman management console and the gateway
* [Getting Started](apimanGettingStarted.html) - Deploying Apiman to Fabric8
* [Data Model](apimanDataModel.html) - Introducing the Apiman Data model
* [Publish Service](apimanImportService.html) - Importing and publishing services to the Apiman gateway
* [Consume Service](apimanConsumeService.html) - Consuming services in your Application

To learn more about its capabilities and how to use it, please
refer to the [apiman User Guide](http://www.apiman.io/latest/user-guide.html) and other
[tutorial](http://www.apiman.io/latest/tutorials.html) resources on the apiman project site.
