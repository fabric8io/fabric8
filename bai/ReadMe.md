## Fuse BAI

The **Fuse BAI** (or Business Activity Insight) module is is designed to give insight into the underlying business processes by capturing business events into an audit log that can then be stored somewhere (e.g. a database, NoSQL or hadoop) and then analysed and queried offline without impacting the integration flows.

To use Fuse BAI you define the audit points at which to capture events in your Camel routes either by:

* explicit use of an audit endpoint in routes to specifically route to an audit endpoint, for example using a [wire tap](http://camel.apache.org/wire-tap.html)
* using an **[AuditEventNotifier](https://github.com/fusesource/fuse/blob/master/bai/bai-core/src/main/java/org/fusesource/bai/AuditEventNotifier.java#L71)** to configure rules to define when to capture events in your camel routes without modifying your camel routes directly

We prefer the AuditEventNotifier as it leaves auditing completely separate from your business level integration flows; which should solve the common 80% of audit requirements. If ever you have some really complex requirements feel free to use explicit routing to an audit endpoint using the Camel DSL.

The AuditEventNotifier implementation is currently based on the [PublishEventNotifier](http://camel.apache.org/maven/current/camel-core/apidocs/org/apache/camel/management/PublishEventNotifier.html) plugin in Camel which filters events and then writes the AuditEvents to the audit endpoint (which is a regular Camel Endpoint and so can then use the various available [Camel Endpoints](http://camel.apache.org/components.html).

### Underlying event types

There are different kind of exchange events raised by Camel:

* created: an exchange has been created
* completed: an exchange has been completed succesfully. (We can use this event to capture how long an exchange took to process)
* sending: an endpoint is about to be invoked
* sent: an endpoint has been invoked and for InOut exchange patterns, we have the response
* failure: an exchange failed
* redelivery: we had to redeliver an exchange due retry failures

Each of these kinds of events can be filtered by configuring the AuditEventNotifier as follows:

* setting the include flag to false
* specifying a Prediate using a [Camel expression language](http://camel.apache.org/languages.html)
* specifying one or more regular expressions on the [URI name to filter]()

You can configure an instance of AuditEventNotifier using Java or your dependency injection framework like [spring](https://github.com/fusesource/fuse/blob/master/bai/bai-sample-camel/src/main/resources/META-INF/spring/context.xml#L8) or CDI.

For example see this [sample spring XML](https://github.com/fusesource/fuse/blob/master/bai/bai-sample-camel/src/test/resources/org/fusesource/bai/sample/FilterExpressionTest-context.xml#L27) where the **sentFilter** predicate is set and various events are disabled by setting the related include flag to false and the [sentRegex](https://github.com/fusesource/fuse/blob/master/bai/bai-sample-camel/src/test/resources/org/fusesource/bai/sample/FilterExpressionTest-context.xml#L34) is specified to filter on the endpoint URI.

Events are then sent to an *audit endpoint* by the AuditEventNotifier using its [endpointUri property](https://github.com/fusesource/fuse/blob/master/bai/bai-sample-camel/src/test/resources/org/fusesource/bai/sample/FilterExpressionTest-context.xml#L45).

You can create multiple AuditEventNotifier instances with different configurations (e.g. to filter different things) and writing to different audit endpoints. Another approach would be to create a single AuditEventNotifier which generates all possible audit events you are interested; then use content based routing on the audit endpoint to write events to different back end components.

### Asynchronous delivery of audit events

Typically we expect audit events to be informational and to have minimal impact on the runtime performance of the system. So a common configuration is to send to an endpoint like **vm:audit?waitForTaskToComplete=Never** so that there is minimal impact on the business level routing routes.

Then asynchronously you consume from this endpoint and write them to some back end; or use the MongoDb back end for example.

If you want you could invoke the audit back end directly in your routes without using a vm:audit intermediary; this has the benefit of being transactional and atomic if you are using say, JMS to process messages and the same JMS endpoint as the audit endpoint; at the cost of a little more activity in the business routes. However if you're using ActiveMQ in transactional mode then this will have minimal effect as the send to the audit queue would be mostly asynchronous but would add some latency.

A word of warning on asynchronous delivery; if your JVM terminates you can loose any in process audit messages; if losing an audit message is show stopper you must use a synchronous dispatch; for example send to an audit JMS queue inside the same JMS transaction as your other integration route processing.

### Back ends

We have a MongoDbBackend that can be used to consume the [AuditEvent](https://github.com/fusesource/fuse/blob/master/bai/bai-core/src/main/java/org/fusesource/bai/AuditEvent.java#L30 objects that the AuditEventNotifier emits to store things in a [MongoDb](http://www.mongodb.org/) database.

Back ends are completely optional; you could just use a regular camel route to consume from your *audit endpoint* and use the usual EIPs to content based route them, transform them and write them to some queue / database / noqsl etc.

However the back end implementations try and provide common solutions to auditing such as correlating exchanges based on breadcrumb IDs etc.

Also most back ends also support the use of an [expression to calculate the payload](https://github.com/fusesource/fuse/blob/master/bai/bai-sample-camel/src/test/resources/org/fusesource/bai/sample/ConfigurableBodyExpressionTest-context.xml#L43) written to the storage system (such as MongoDb).



### Running the sample

Here is the [sample spring XML](https://github.com/fusesource/fuse/blob/master/bai/bai-sample-camel/src/main/resources/META-INF/spring/context.xml#L8) - we define the **AuditEventNotifier** first; then the MongoDb back end which asynchronously consumes events from the *audit endpoint* and writes them to MongoDb.

Start a local [MongoDb](http://www.mongodb.org/).
Then run the following commands:

    cd bai
    mvn install
    cd bai-sample-camel
    mvn camel:run

A sample camel route should now be running which should have configured the auditing of exchanges to MongoDb.

You can now browse the **bai** database in MongoDb as follows:

### Browsing the events

to use the Mongo shell type:

    use bai
    show collections
    db.baievents.findAll()

Or you could install [mViewer](https://github.com/Imaginea/mViewer) and browse the **bai** database in MongoDb using the web client

### MongoDb collections

* **baievents** contains all the events in a flat easy to query collection
* **$contextId.$routeId** contains all the exchanges on this route
* **exchangeXray** contains a list of all the context & route collections that each breadcrumb has been through; so for a given bread crumb ID you can find what collections to filter to find details of all its exchanges


### Running the BAI Agent in OSGi

First you will need a Fuse container (e.g. Fuse Fabric, Fuse MQ, Fuse ESB). e.g. to build a local Fuse Fabric container try the following:

    cd fabric
    mvn install
    cd fuse-fabric/target
    tar xf fuse-fabric-99-master-SNAPSHOT.tar.gz
    cd fuse-fabric-99-master-SNAPSHOT
    bin/fusefabric

If you don't have one but have an Apache Karaf then please install the fuse-features.xml in your container before continuing.

Then to install the Fuse BAI agent with the MongoDb back end try:

    features:install bai-mongodb

The above installs the BAI features along with the default MongoDb back end (which requires a MongoDb database on localhost).

If you want to try just the BAI agent without the MongoDb back end, then just install the **bai** feature and add your own route to consume from endpoint **vm:audit**. Please use a CamelContext ID of audit-* to ensure your audit route is not audited too by the agent! :)

Now install a sample camel route which will then be audited

    install mvn:org.fusesource.bai/sample-camel-blueprint/99-master-SNAPSHOT

Now start the bundle.

You should see this route audited to the MongoDb **bai** database as the BAI agent will auto-detect the CamelContext starting and attach the AuditEventNotifier.

### Configuring the BAI Agent

We use the OSGi Config Admin service to let you enable and disable the BAI Agent on different bundles and CamelContext IDs together with allowing you to filter out event types with flags, predicates and regular expressions on endpoint URIs.

We use OSGi Config admin as it means you can then use the Karaf command line shell, use Karaf config files in the Fuse container or using Fuse Fabric profiles (with profile based overriding).

The configuration is created in the config admin PID **org.fusesource.bai.agent**

#### Excluding CamelContexts from audit

You may want to exclude certain camel contexts from being audited completely. That is to say no auditing is performed at all.
To do this define a property of this format:

    camelContext.exclude = camelContextPatterns

Where *camelContextPatterns* is a space separated list of camelContextPattern instances. A camelContextPatterns is of the form

* *bundleSymbolicNamePattern\[:camelContextIdPattern\]*

where both *bundleSymbolicNamePattern* and *camelContextIdPattern* are text patterns using * to indicate matching zero to many characters.

For example to match all bundle symbolic names and camelContext IDs you could use * or \*:\*

To match a specific bundle symbolic name 'com.acme.foo' you could use

* com.acme.foo
* com.acme.foo:*
* com.acme*
* com.acme\*:\*

To match all of the Camel Context's with IDs 'cheese' in all bundles you could use

* *:cheese

For example the default rule below will exclude all camel contexts in any bundle which have a CamelContext ID which starts with *"audit-"*

    camelContext.exclude = *:audit-*


#### Excluding Events

All kinds of events are raised by default. You may wish to include or exclude kinds of events on a per bundle symbolic name or camelContextID. To do this use this form of key/value

    event.$eventName.$camelContextPattern = (true|false)

Where *$eventName* can be one of

* created
* completed
* sending
* sent
* failure
* failureHandled
* redelivery

e.g. to exclude the create events in all bundles which begin with "foo" for all CamelContext IDs then use:

    event.create.foo* = false

or to exclude all event types for bundles which begin with foo* try:

    event.*.foo* = false

#### Filtering Exchanges via Predicates

To filter individual exchanges from being audited you may wish to use a [Camel expression language](http://camel.apache.org/languages.html).

    exchange.filter.$eventType.$language.$camelContextPattern = expression

For example to use a header using xpath on 'my-bundle' only:

    exchange.filter.sent.xpath.my-bundle = in:header("foo") = 'bar'

Or to apply a filter for all bundles:

    exchange.filter.sending.simple.* = ${in.header.foo} == 'cheese'

Note that you can only have 1 filter for a given event type, language and bundleID and/or camelContextID expression


#### Filtering Endpoint URIs

To filter specific endpoints when being invoked in a route you can use regular expressions on the endpoint URI itself. If no regexs are specified then all of them are included.

Again you can restrict these filters to specific bundleIDs and/or camelContextIDs

    endpoint.regex.$camelContextPattern = $endpointUriRegex

For example to include all activemq endpoints on all bundleIDs and camelContextIDs

    endpoint.regex.* = activemq:.*

To only include activemq endpoints in the bundleID "foo" for camelContextID "bar" it would be

    endpoint.regex.foo:bar = activem:q.*

