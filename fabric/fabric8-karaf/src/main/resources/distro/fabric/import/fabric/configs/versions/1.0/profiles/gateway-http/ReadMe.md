Provides a HTTP based gateway between HTTP clients and web services, RESTful endpoints and web applications running on the fabric.

The gateway acts as a [reverse proxy server](http://en.wikipedia.org/wiki/Reverse_proxy); so that the clients think the gateway is the server implementing the various web applications and services.

To map the URIs inside the gateway to HTTP URLs on the underlying web services and web applications we use mapping rules which make use of a URI template syntax to allow flexible configuration and mapping options.