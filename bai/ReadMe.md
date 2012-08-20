## Fuse BAI

The **Fuse BAI** (or Business Activity Insight) module is all about capturing business events into an audit log that can then be stored somewhere (e.g. a NoSQL) and then analysed and queried offline without impacting the integration flows.

You can then define the audit points at which to capture events in your Camel routes either by:

* using a **AuditEventNotifier** to configure generic rules to capture all events in all camel routes without modifying your camel routes directly
* explicit use of an audit endpoint in routes to specifically routing to an audit endpoint

We prefer the former as it leaves auditing completely separate from your business level integration flows; which should solve the common 80% of audit requirements. If ever you have some really complex requirements feel free to use explicit routing to an audit endpoint to solve really complex routing rules.

### Specifying generic audit rules

To avoid having to explicitly add audit rules to your camel routes we recommend using the **AuditEventNotifier** which automatically filters out Camel events and creates an AuditEvent for each exchange you are interested in which is then routed to an *audit endpoint*.

The AuditEventNotifier is then a bean configured in your application (e.g. in a spring XML like this [example spring XML](https://github.com/fusesource/fuse/blob/master/bai/bai-sample-camel/src/main/resources/META-INF/spring/context.xml#L8)).

The AuditEventNotifier implementation is currently based on the [PublishEventNotifier](http://camel.apache.org/maven/current/camel-core/apidocs/org/apache/camel/management/PublishEventNotifier.html) plugin in Camel which filters events and then writes the AuditEvents to the audit endpoint (which is a regular Camel Endpoint and so can then use the various available [Camel Endpoints](http://camel.apache.org/components.html).

There is nothing to stop you creating multiple AuditEventNotifier instances with different configurations (e.g. to filter different things) and writing to different audit endpoints. Another approach would be to create a single AuditEventNotifier which generates all possible audit events you are interested; then use content based routing on the audit endpoint to write events to different back end components.

### Asynchronous delivery of audit events

Typically we expect audit events to be informational and to have minimal impact on the runtime performance of the system. So a common configuration is to send to an endpoint like **vm:audit?waitForTaskToComplete=Never** so that there is minimal impact on the business level routing routes.

Then asynchronously you consume from this endpoint and write them to some back end; or use the MongoDb back end for example.

If you want you could invoke the audit back end directly in your routes without using a vm:audit intermediary; this has the benefit of being transactional and atomic if you are using say, JMS to process messages and the same JMS endpoint as the audit endpoint; at the cost of a little more activity in the business routes. However if you're using ActiveMQ in transactional mode then this will have minimal effect as the send to the audit queue would be mostly asynchronous but would add some latency.

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

### Mongo collections

* exchangeXray contains a list of all the context and route IDs which are beinbg audited; so querying this collection allows tools to render the various event streams
* baievents contains all the events in a flat easy to query collection
* $contextId.$routeId contains all the exchanges on this route
