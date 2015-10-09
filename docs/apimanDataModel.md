## Apiman Datamodel

It is perhaps most important to understand the various entities used by the API Manager, as well as their relationships with each other.

### Organizations

The top level container concept within the API management project its called the organization. All other entities are managed within the scope of an organization.

When users log into the API management system they must be affiliated with one or more organization. Users can have different roles within that organization allowing them to perform different actions and manage different entities. Please see the User Management section below for more details on this topic.

What an organization actually represents will depend upon who is using API management. When installed within a large enterprise, an organization may represent an internal group within IT (for example the HR group). If installed in the cloud, an organization might represent an external company or organization.

In any case, an organization is required before the end user can create or consume services.

### Policies

The most important concept in API management is the policy. The policy is the unit of work executed at runtime in order to implement API governance. All other entities within the API Manager exist in support of configuring policies and sensibly applying them at runtime.

When a request for a Service is made at runtime, a policy chain is created and applied to the inbound request, prior to proxying that request to the back-end API implementation. This policy chain consists of policies configured in the API Manager.

An individual policy consists of a type (e.g. authentication or rate limiting) as well as configuration details specific to the type and instance of that policy. Multiple policies can be configured per service resulting in a policy chain that is applied at runtime.

It is very important to understand that policies can be configured at three different levels within API management. Policies can be configured on a service, on a plan, or on an application. For more details please see the sections below.

### Plans

A plan is a set of policies that define a level of service. Whenever a service is consumed it must be consumed through a plan. Please see the section on Service Contracts for more information.

An organization can have multiple plans associated with it. Typically each plan within an organization consists of the same set of policies but with different configuration details. For example, an organization might have a Gold plan with a rate limiting policy that restricts consumers to 1000 requests per day. The same organization may then have a Silver plan which is also configured with a rate limiting policy, but which restricts consumers to 500 requests per day.

Once a plan has been fully configured (all desired policies added and configured) it must be locked so that it can be used by services. This is done so that service providers can’t change the details of the plan out from underneath the application developers who are using it.

### Services

A service represents an external API that is being governed by the API management system. A service consists of a set of metadata including name and description as well as an external endpoint defining the API implementation. The external API implementation endpoint includes the type/protocol and the actual endpoint location so that the API can be properly proxied to at runtime.

In addition, policies can be configured on a service. Typically, the policies applied to services are things like authentication. Any policies configured on service will be applied at runtime regardless of the application and service contract. This is why authentication is a common policy to configure at the service level.

Services must be offered through a valid plan configured in the same organization. Service consumers must consume the service through one of those plans. Please see the section on Service Contracts for more information. Note that, alternatively, Services can be marked as "Public", which means they (once published) can be used by any client, without needing a Contract. To use a public service, simply send requests to the appropriate managed endpoint (through the API Gateway) without providing an API Key.

Only once a service is fully configured, including its policies, implementation, and plans can it be published to the runtime gateway for consumption by applications. Once this is done, the service cannot be changed. If changes are required, a new version of the service must be created and configured.

### Applications

An application represents a consumer of an API. Typical API consumers are things like mobile applications and B2B applications. Regardless of the actual application implementation, an application must be added to the API management system so that contracts can be created between it and the services it wishes to consume.

An application consists of basic metadata such as name and description. Policies can also be configured on an application, but are optional.

Finally, service contracts can be created between an application and the service(s) it wishes to consume. Once the service contracts are created, the application can be registered with the runtime gateway. Once this registration is complete, the application can no longer be altered. If changes are required, a new version of the application must be created and configured.

### Service Contracts

A service contract is simply a link between an application and a service through a plan offered by that service. This is the only way that an application can consume a service. If there are no applications that have created service contracts with a service, that service cannot be accessed through the API management runtime gateway.

When a service contract is created, the system generates a unique API key specific to that contract. All requests made to the service through the API Gateway must include this API key. The API key is used to create the runtime policy chain from the policies configured on the service, plan, and application.

Service Contracts can only be created between Applications and published Services which are offered through at least one Plan. A Service Contract cannot be created between an Application and a public Service.

### Policy Chain

A policy chain is an ordered sequence of policies that are applied when a request is made for a service through the API Gateway. The order that policies are applied is important and is as follows:

* Application
* Plan
* Service

Within these individual sections, the end user can specify the order of the policies.

When a request for a service is received by the API Gateway the policy chain is applied to the request in the order listed above. If none of the policies fail, the API Gateway will proxy the request to the backend API implementation. Once a response is received from the back end API implementation, the policy chain is then applied in reverse order to that response. This allows each policy to be applied twice, once to the inbound request and then again to the outbound response.
