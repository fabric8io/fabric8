Provides a HTTP based gateway between HTTP clients and web services, RESTful endpoints and web applications running on the fabric.

The gateway acts as a [reverse proxy server](http://en.wikipedia.org/wiki/Reverse_proxy); so that the clients think the gateway is the server implementing the various web applications and services.

To map the URIs inside the gateway to HTTP URLs on the underlying web services and web applications we use HTTP mapping rules which make use of a [URI template](http://en.wikipedia.org/wiki/URL_Template) syntax to allow flexible configuration and mapping options.

### Example

If you run the <a class="btn btn-primary" href="#/fabric/containers/createContainer?profileIds=example-quickstarts-rest&versionId={{versionId}}">REST Quickstart</a> in some container and run an instance of the gateway on the localhost, then you'll be able to invoke the REST Quickstart example using the URL [http://localhost:9000/cxf/crm/customerservice/customers/123](http://localhost:9000/cxf/crm/customerservice/customers/123) irrespective of which machines you are running the REST quickstart containers.

You can also see the available mappings in the HTTP gateway via the URL [http://localhost:9000/]([http://localhost:9000/) which returns JSON of all the available mappings of URIs to the underlying services.