## API Management

### Introduction

With a successful adaption of a micro services architecture you will soon have many services, and each of these services publishes an API that can be consumed by applications or other services. This API Management functionality is provided by integrating the [apiman](http://apiman.io) project into fabric8. The apiman application lets you import your service endpoints and their API description documents. It is in the apiman console that a service can be decorated with policies and plans. Once you are ready the service can be published to a gateway to make it available for consumption. The gateway is a standalone, highly available, application that sits in between your clients and your APIs that adds governance and metrics to any micro service.

Some common API Management use-cases are:

* *Quotas/Rate Limiting* : limit the number of requests users or clients are allowed to send to the service.
* *Security* : add security (e.g. BASIC authentication, IP Address white/blacklisting) to your services.
* *Metrics* : track who is using your services (number of requests), when they use them, and how.

### Overview of the components shipped by apiman

The bottom half of the apiman component diagram below shows the apiman gateway. The gateway is a standalone application that functions as proxy between service consumers and producers. Requests are routed through a policy engine that applies a preconfigured chain of policies both on the way in (think of authentication policies) as well as on the way out (think of usage policies). Policies are plugins that if needed can connect to external systems to lookup authentication credentials such as LDAP, a JDBC store or a REST based call to an external system. The gateway component services runtime requests. 

The top half of the diagram show the apiman application itself. This application is used at design time by service producers to configure their service with policies, and or plans. Service consumers then agree to a certain plan and a contract can be set up between the two parties. More on this later. Once the desired policies, plans and contracts are in place, the service can be published to the gateway. The publish process is a REST based call from apiman to the gateway. At this point the service is live for consumption by application developers (service consumers).

![apiman overview diagram](images/apiman1.png).

_Overview of the apiman components_


### Apiman on Fabric8

Apiman is deployed on top of Fabric8. This means that both apiman and the gateway are deployed as a microservice. This means that each app is a Kubernetes Service backed by one of more Kubernetes Pod instances. In each namespace you can run an apiman service and a gateway. Persistence is provided by using elasticsearch as a NoSQL datastore. You can run more then one pod for both apiman and the gateway to increase performance of these apps if needed.


### Getting started

Getting started is easy, you must run the **apiman** app group:

* Click on the **Run** button on the top right to run **apiman** 

This app group starts a three applications: 'apiman', 'apiman-gateway' and 'elasticsearch'. The apiman application provides the Management configuration layer, which consists of a REST layer and a User Interface, the apiman gateway application is the gateway. For the configuration layer and the Gateway to work properly, it will start an elasticsearch service if one wasn't already running. Elasticsearch is used for persistence by both apiman management app as well as the apiman Gateway, the latter only if you use policies that need to share request count state between pod instances. 

When all these applications are running you can see the console at the apiman url 

* In the [Console](console.html) click on **API Management**.

and if you run in vagrant this url would be http://apiman.vagrant, but there are also entries in the menu navigation to get to 'API Management'. The apiman dashboard will show. If this is the first time you launch it, please go into the Admin section and check that the bootstrap process loaded the Roles, Policy Definitions and the apiman gateway.

![Roles](images/apiman-roles.png).

_Roles_

![Policy Definitions](images/apiman-policydefinitions.png).

_Policy Definitions_

![APIManGateway](images/apiman-gateway.png).

_APIManGateway_

In the apiman gateway screen you can click the **Test Gateway** button to check that apiman can interact with the apiman gateway.

Note that there is a known issue where apiman can come up before elasticsearch which can lead to the apiman not being able to find elasticsearch. This leads to the apiman console not being available. Killing the apiman pod(s) so they restart will usually clear this.


### Apiman Datamodel

It is perhaps most important to understand the various entities used by the API Manager, as well as their relationships with each other.

#### Organizations

The top level container concept within the API management project its called the organization. All other entities are managed within the scope of an organization.

When users log into the API management system they must be affiliated with one or more organization. Users can have different roles within that organization allowing them to perform different actions and manage different entities. Please see the User Management section below for more details on this topic.

What an organization actually represents will depend upon who is using API management. When installed within a large enterprise, an organization may represent an internal group within IT (for example the HR group). If installed in the cloud, an organization might represent an external company or organization.

In any case, an organization is required before the end user can create or consume services.

#### Policies

The most important concept in API management is the policy. The policy is the unit of work executed at runtime in order to implement API governance. All other entities within the API Manager exist in support of configuring policies and sensibly applying them at runtime.

When a request for a Service is made at runtime, a policy chain is created and applied to the inbound request, prior to proxying that request to the back-end API implementation. This policy chain consists of policies configured in the API Manager.

An individual policy consists of a type (e.g. authentication or rate limiting) as well as configuration details specific to the type and instance of that policy. Multiple policies can be configured per service resulting in a policy chain that is applied at runtime.

It is very important to understand that policies can be configured at three different levels within API management. Policies can be configured on a service, on a plan, or on an application. For more details please see the sections below.

#### Plans

A plan is a set of policies that define a level of service. Whenever a service is consumed it must be consumed through a plan. Please see the section on Service Contracts for more information.

An organization can have multiple plans associated with it. Typically each plan within an organization consists of the same set of policies but with different configuration details. For example, an organization might have a Gold plan with a rate limiting policy that restricts consumers to 1000 requests per day. The same organization may then have a Silver plan which is also configured with a rate limiting policy, but which restricts consumers to 500 requests per day.

Once a plan has been fully configured (all desired policies added and configured) it must be locked so that it can be used by services. This is done so that service providers can’t change the details of the plan out from underneath the application developers who are using it.

#### Services

A service represents an external API that is being governed by the API management system. A service consists of a set of metadata including name and description as well as an external endpoint defining the API implementation. The external API implementation endpoint includes the type/protocol and the actual endpoint location so that the API can be properly proxied to at runtime.

In addition, policies can be configured on a service. Typically, the policies applied to services are things like authentication. Any policies configured on service will be applied at runtime regardless of the application and service contract. This is why authentication is a common policy to configure at the service level.

Services must be offered through a valid plan configured in the same organization. Service consumers must consume the service through one of those plans. Please see the section on Service Contracts for more information. Note that, alternatively, Services can be marked as "Public", which means they (once published) can be used by any client, without needing a Contract. To use a public service, simply send requests to the appropriate managed endpoint (through the API Gateway) without providing an API Key.

Only once a service is fully configured, including its policies, implementation, and plans can it be published to the runtime gateway for consumption by applications. Once this is done, the service cannot be changed. If changes are required, a new version of the service must be created and configured.

#### Applications

An application represents a consumer of an API. Typical API consumers are things like mobile applications and B2B applications. Regardless of the actual application implementation, an application must be added to the API management system so that contracts can be created between it and the services it wishes to consume.

An application consists of basic metadata such as name and description. Policies can also be configured on an application, but are optional.

Finally, service contracts can be created between an application and the service(s) it wishes to consume. Once the service contracts are created, the application can be registered with the runtime gateway. Once this registration is complete, the application can no longer be altered. If changes are required, a new version of the application must be created and configured.

#### Service Contracts

A service contract is simply a link between an application and a service through a plan offered by that service. This is the only way that an application can consume a service. If there are no applications that have created service contracts with a service, that service cannot be accessed through the API management runtime gateway.

When a service contract is created, the system generates a unique API key specific to that contract. All requests made to the service through the API Gateway must include this API key. The API key is used to create the runtime policy chain from the policies configured on the service, plan, and application.

Service Contracts can only be created between Applications and published Services which are offered through at least one Plan. A Service Contract cannot be created between an Application and a public Service.

#### Policy Chain

A policy chain is an ordered sequence of policies that are applied when a request is made for a service through the API Gateway. The order that policies are applied is important and is as follows:

* Application
* Plan
* Service

Within these individual sections, the end user can specify the order of the policies.

When a request for a service is received by the API Gateway the policy chain is applied to the request in the order listed above. If none of the policies fail, the API Gateway will proxy the request to the backend API implementation. Once a response is received from the back end API implementation, the policy chain is then applied in reverse order to that response. This allows each policy to be applied twice, once to the inbound request and then again to the outbound response.


### Importing a service from Fabric8 and publishing it to the gateway

Let's assume you have deployed the CxfCdi quickstart in Fabric8 and that you created your organization in apiman. You can now navigate to the Organization/Service page to import this service into apiman. Apiman will contact Kubernetes to obtain list of services in your namespace that match your search string. Use '*' to match all service. 

!TODO add Image

Select import as 'Public Service' as we don't yet have any Plans or Policies set up. We can add those later. Please notice that after the import is complete, the service has discovered the serviceUrl, serviceType, descriptionUrl and descriptionType. When the service was published to Kubernetes the service developer added this information using Kubernetes Service Annotations.

#### Kubernetes Service Annotations

Kubernetes allows the creation of Service Annotations. Here we propose the use of the following annotations

* 'fabric8.io/servicepath' - the path part of the service endpoint url. An example value could be 'cxfcdi',

* 'fabric8.io/servicetype' - the protocol of the service. Example values can be 'SOAP' or 'REST',

* 'fabric8.io/descriptionpath' - the path part of the service description document’s endpoint. It is a pretty safe assumption that the service self documents. An example value for a swagger 2.0 document can be 'cxfcdi/swagger.json',

* 'fabric8.io/descriptiontype' - the type of Description Language used. Example values can be 'WSDL', 'WADL', 'SwaggerJSON', 'SwaggerYAML'.

The fragment below is taken from the service section of the kubernetes.json were these annotations are used

    ...
    "objects" : [ {
      "apiVersion" : "v1",
      "kind" : "Service",
      "metadata" : {
        "annotations" : {
          "apiman.io/servicetype" : "REST",
          "apiman.io/servicepath" : "cxfcdi",
          "apiman.io/descriptionpath" : "cxfcdi/swagger.json",
          "apiman.io/descriptiontype" : "SwaggerJSON"
      },
    ...

#### Applying a Policy to a 'Public Service'

! TODO Image

### Publishing the service to the gateway

### 


  To learn more about its capabilities and how to use it, please
refer to the [apiman User Guide](http://www.apiman.io/latest/user-guide.html) and other
[tutorial](http://www.apiman.io/latest/tutorials.html) resources on the apiman project site.
