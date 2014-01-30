This profile generates the configuration file for [haproxy](http://en.wikipedia.org/wiki/HAProxy) to generate a [reverse proxy server](http://en.wikipedia.org/wiki/Reverse_proxy) for the web services and web applications running inside the fabric.

To map the URIs inside the gateway to HTTP URLs on the underlying web services and web applications we use HTTP mapping rules which make use of a [URI template](http://en.wikipedia.org/wiki/URL_Template) syntax to allow flexible configuration and mapping options.

Using this profile watches the contents of the fabric8 registry and auto-regenerates the haproxy configuration file based on the services running and the mapping rules. The default location for the generated haproxy configuration file is in **data/gateway/haproxy.conf** in the container's install directory.
