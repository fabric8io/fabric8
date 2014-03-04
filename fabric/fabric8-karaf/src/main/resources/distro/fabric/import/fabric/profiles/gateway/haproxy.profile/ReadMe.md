This profile generates the configuration file for [haproxy](http://en.wikipedia.org/wiki/HAProxy) to generate a [reverse proxy server](http://en.wikipedia.org/wiki/Reverse_proxy) for the web services and web applications running inside the fabric.

To map the URIs inside the gateway to HTTP URLs on the underlying web services and web applications we use HTTP mapping rules which make use of a [URI template](http://en.wikipedia.org/wiki/URL_Template) syntax to allow flexible configuration and mapping options.

Using this profile watches the contents of the fabric8 registry and auto-regenerates the haproxy configuration file based on the services running and the mapping rules. The default location for the generated haproxy configuration file is in **data/gateway/haproxy.conf** in the container's install directory.

### Configuring this profile

Once you have at least one instance of this profile running in a container you will need to make sure that you <a class="btn" href="#/wiki/branch/{{versionId}}/configuration/io.fabric8.gateway.haproxy/fabric/profiles/gateway/haproxy.profile">configure the gateway</a> so that:

* you have specified the generated configuration file's output file name
* optionally specify a command and directory for the command so that haproxy can be gracefully reloaded. This may be something like:

    sudo haproxy -p /var/run/haproxy.pid -sf $(cat /var/run/haproxy.pid)

Then the haproxy configuration file will get regenerated whenever web applications, web services or servlets are added or removed and haproxy reloaded.

### Changing the haproxy configuration

This profile uses an MVEL template file to generate the haproxy configuration file from the dynamic URI templates of the services being exposed. So please don't edit the generated file as your changes will get lost.

To change the haproxy configuration file please edit the <a class="btn" href="/fabric/profiles/gateway/haproxy.profile/io.fabric8.gateway.haproxy.config.mvel">MVEL Template</a>