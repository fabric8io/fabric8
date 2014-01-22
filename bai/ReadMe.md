# Fuse BAI

The **Fuse BAI** (or Business Activity Insight) module is designed to give insight into your underlying business processes by capturing business events in an audit log that can then be stored somewhere (e.g. a database, NoSQL and/or hadoop) and then analysed and queried offline without impacting your integration flows.

To use Fuse BAI you define the audit points at which to capture events in your Camel routes either by:

* explicitly route to an audit endpoint in your camel routes, for example using a [wire tap](http://camel.apache.org/wire-tap.html)
* using an **[AuditEventNotifier](https://github.com/fusesource/fuse/blob/master/bai/bai-core/src/main/java/org/fusesource/bai/AuditEventNotifier.java#L29)** to configure rules to define which bundles and/or CamelContexts are auditted and when to capture events in your camel routes without modifying your camel routes directly

We prefer the AuditEventNotifier approach as it leaves auditing completely separate from your business level integration flows. If ever you have some really complex requirements feel free to use explicit routing to an audit endpoint using the full power of the Camel DSL; otherwise keeping your auditing policies separate is a good thing.

The AuditEventNotifier implementation is currently based on the [PublishEventNotifier](http://camel.apache.org/maven/current/camel-core/apidocs/org/apache/camel/management/PublishEventNotifier.html) plugin in Camel. 

## BAI Policies

To configure BAI you configure a number of Policy objects in a PolicySet.

Each CamelContext then gets its own AuditEventNotifier with the Policy objects which apply to it. At any time the Policies can be changed and the auditing is updated in real time; so its easy to enable/disable different forms of auditing.

A Policy has a unique ID, can be enabled and disabled, has a camel endpoint URI it sends audit events to, an optional expression to define what payload is sent to the audit endpoint and can have various filters to decide when its used and when events are captured:

* bundle ID and/or CamelContext name the policy applies to
* endpoint URIs 
* event types
* message payload predicates (using a [Camel expression language](http://camel.apache.org/languages.html))
* the payload expression to decide what data to send to the audit endpoint (again using a [Camel expression language](http://camel.apache.org/languages.html))

If no payload expression is specified then the AuditEventNotifier writes matching [AuditEvents](https://github.com/fusesource/fuse/blob/master/bai/bai-core/src/main/java/org/fusesource/bai/AuditEvent.java#L36) to the audit endpoint for the policy - which is a regular Camel Endpoint and so can then use the various available [Camel Endpoints](http://camel.apache.org/components.html). 

Filters typically use text wildcards to match ids/names/URIs, with '*' meaning any characters rather like file name patterns. Also filters usually have a list of includes (matching anything matching an include) or excludes which match anything apart from things matching an exclude.

### Underlying event types

There are different kind of exchange events raised by Camel:

* created: an exchange has been created
* completed: an exchange has been completed succesfully. (We can use this event to capture how long an exchange took to process)
* sending: an endpoint is about to be invoked
* sent: an endpoint has been invoked and for InOut exchange patterns, we have the response
* failure: an exchange failed
* redelivery: we had to redeliver an exchange due retry failures

In the Policy we can include or exclude one or more of the above event types. By default they are all captured and sent to the audit endpoint for an applicable enabled Policy.

## Configuring Policies

You can configure a PolicySet using a Java DSL, an XML document (which has an XSD defined to help editing with XML aware editors) or using a Properties file notation (described below).


For example see this [sample spring XML]https://github.com/fusesource/fuse/blob/master/bai/sample-spring-bai/src/test/resources/org/fusesource/bai/sample/FilterExpressionTest-context.xml#L27) where the auditPolicy is created from a [properties file](https://github.com/fusesource/fuse/blob/master/bai/sample-spring-bai/src/test/resources/filterExpressionPolicySet.properties#L18).

Events are then sent to an *audit endpoint* by the AuditEventNotifier using its [endpointUri property](https://github.com/fusesource/fuse/blob/master/bai/sample-spring-bai/src/test/resources/filterExpressionPolicySet.properties#L21).

You can create multiple AuditEventNotifier instances with different PolicySet configurations if you like; though we expect a single AuditEventNotifier with a collection of Policy configurations is probably enough.

For example you could use different Policy configurations to filter different things and write to different audit endpoints. (e.g. to filter different things) and writing to different audit endpoints. Another approach would be to create a single Policy which generates all possible audit events you are interested; then asynchronously use content based routing on the audit endpoint to write events to different back end components.

### Configuring the BAI Agent in OSGi

We use the OSGi Config Admin service to let you enable and disable the BAI Agent on different bundles and CamelContext IDs together with allowing you to filter out event types with flags, predicates and regular expressions on endpoint URIs.

We use OSGi Config admin as it means you can then use the Karaf command line shell, use Karaf config files in the Fuse container or using Fuse Fabric profiles (with profile based overriding).

The configuration is created in the config admin PID **org.fusesource.bai.agent**

This file uses the Properties notation (described below), or you can use the following entry to use the XML file format

	# lets use an XML file to configure hte policies
	bai.xml = fuse-bai.xml

### XML configuration format

Using the XML format lets you use XSD aware editors perform validation and smart completion for you. Here is [are some examples](https://github.com/fusesource/fuse/blob/master/bai/bai-core/src/test/resources/simpleConfig.xml#L1) of the [XML configuration file](https://github.com/fusesource/fuse/blob/master/bai/bai-core/src/test/resources/conciseAndVerboseExample.xml#L1) hopefully its pretty easy to read and understand.

### Properties configuration format

The Properties notation configures the same underlying PolicySet and Policy objects but uses a simple [Properties notation](https://github.com/fusesource/fuse/blob/master/bai/bai-core/src/test/resources/policySet.properties#L1).

For example this file defines 2 contexts foo and bar with bar being disabled:

	# bai.$ID.context.(include|exclude) = (bundlePattern:namePattern)+ ...
	bai.foo.context.include = com.fusesource.mybundle.one:context1
	bai.bar.context.exclude = *:audit-*
	
	# bai.$ID.to = endpointUri
	bai.foo.to = mock:audit
	
	# bai.$ID.enabled = (true|false)
	bai.bar.enabled = false
	
	# bai.$ID.endpoint.(include|exclude) = (endpointUriPattern)+
	bai.foo.endpoint.include = activemq:*
	
	# bai.$ID.event.(include|exclude) = (created | completed | 
	#   sending | sent | failure | failureHandled | redelivery)
	bai.foo.event.exclude = failureHandled
	
	# bai.$ID.filter.language = expression
	bai.foo.filter.xpath = /foo/@id = 'bar'

Notice the use of wildcard inclusions of endpoint URIs (e.g. foo includes endpoints starting with "activemq:" and the foo policy excludes failureHandled event types). Also notice the use of an XPath expression on the foo policy for filtering events by the message payload.

## Asynchronous delivery of audit events

Typically we expect audit events to be informational and to have minimal impact on the runtime performance of the system. So a common configuration is to send to an endpoint like **vm:audit?waitForTaskToComplete=Never** so that there is minimal impact on the business level routing routes; there is no waiting for it to be written.

Then asynchronously you consume from this endpoint and write them to some back end; or use the MongoDb back end for example.

If you want you could invoke the audit back end directly in your routes without using a vm:audit intermediary; this has the benefit of being transactional and atomic if you are using say, JMS to process messages and the same JMS endpoint as the audit endpoint; at the cost of more activity in the business routes - which could slow things down. However if you're using ActiveMQ in transactional mode then this will have minimal effect as the send to the audit queue would be mostly asynchronous but would add some latency.

A word of warning on asynchronous delivery; if your JVM terminates you can loose any in process audit messages; if losing an audit message is show stopper you must use a synchronous dispatch; for example send to an audit JMS queue inside the same JMS transaction as your other integration route processing.

### Expensive Transforms and Predicates

If you decide to use relatively slow filters and expressions for audit events (e.g. parsing message payloads to XML and doing XPath/XQuery); they will all be evaluated synchronously as part of the route. This may have too much of a latency or performance impact of your system.

So another option is to just send messages to some endpoint and asynchronously perform the relatively slow filters and transformation expressions in a separate thread to avoid impacting the underlying routes.

### Mutable payloads

If you are sending references to domain objects in your Camel payloads which can be mutated after code sends them into Camel, you may wish to force BAI to explicitly take a deep copy of your object so that any asynchronous changes to your domain objects will not be lost.

The easiest way to do that is use a direct endpoint for audit, say **direct:audit** then have a camel route consuming from that, performing whatever transformations are required, then sending to the underlying endpoint.

e.g.

	from("direct:audit").
		marshal().json(JsonLibrary.Jackson).
		to("vm:audit");

## Back ends

We have a MongoDbBackend that can be used to consume the [AuditEvent](https://github.com/fusesource/fuse/blob/master/bai/bai-core/src/main/java/org/fusesource/bai/AuditEvent.java#L30) objects that the AuditEventNotifier emits to store things in a [MongoDb](http://www.mongodb.org/) database.

Back ends are completely optional; you could just use a regular camel route to consume from your *audit endpoint* and use the usual EIPs to content based route them, transform them and write them to some queue / database / noqsl etc.

However the back end implementations try and provide common solutions to auditing such as correlating exchanges based on breadcrumb IDs etc.

Also most back ends also support the use of an [expression to calculate the payload](https://github.com/fusesource/fuse/blob/master/bai/bai-sample-camel/src/test/resources/org/fusesource/bai/sample/ConfigurableBodyExpressionTest-context.xml#L43) written to the storage system (such as MongoDb).

# Examples

There are various examples you can try out depending on your requirements:

## Running the BAI Agent in OSGi

First you will need a Fuse container (e.g. Fuse Fabric, Fuse MQ, Fuse ESB). e.g. to build a local Fuse Fabric container try the following:

    cd fabric
    mvn install
    cd fabric8-karaf/target
    tar xf fabric8-karaf-99-master-SNAPSHOT.tar.gz
    cd fabric8-karaf-99-master-SNAPSHOT
    bin/karaf

If you don't have one but have an Apache Karaf then please install the fuse-features.xml in your container before continuing.

Then to install the Fuse BAI agent with the MongoDb back end try:

    features:install bai-mongodb

The above installs the BAI features along with the default MongoDb back end (which requires a MongoDb database on localhost).

If you want to try just the BAI agent without the MongoDb back end, then just install the **bai** feature and add your own route to consume from endpoint **vm:audit**. Please use a CamelContext ID of audit-* to ensure your audit route is not audited too by the agent! :)

Now install a sample camel route which will then be audited

    install mvn:org.fusesource.bai/sample-camel-blueprint/99-master-SNAPSHOT

Now start the bundle.

You should see this route audited to the MongoDb **bai** database as the BAI agent will auto-detect the CamelContext starting and attach the AuditEventNotifier.


## Running the spring sample

Here is the [sample spring XML](https://github.com/fusesource/fuse/blob/master/bai/sample-spring-bai/src/main/resources/META-INF/spring/context.xml#L8) - we define the **AuditEventNotifier** first; then the MongoDb back end which asynchronously consumes events from the *audit endpoint* and writes them to MongoDb.

Start a local [MongoDb](http://www.mongodb.org/).
Then run the following commands:

    cd bai
    mvn install
    cd bai-sample-camel
    mvn camel:run

A sample camel route should now be running which should have configured the auditing of exchanges to MongoDb.

You can now browse the **bai** database in MongoDb as follows:

## Browsing the events

to use the Mongo shell type:

    use bai
    show collections
    db.baievents.findAll()

Or you could install [mViewer](https://github.com/Imaginea/mViewer) and browse the **bai** database in MongoDb using the web client

### MongoDb collections

* **baievents** contains all the events in a flat easy to query collection
* **$contextId.$routeId** contains all the exchanges on this route
* **exchangeXray** contains a list of all the context & route collections that each breadcrumb has been through; so for a given bread crumb ID you can find what collections to filter to find details of all its exchanges


