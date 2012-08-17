## Fuse BAI

The **Fuse BAI** (or Business Activity Insight) module is all about capturing business events into an audit log that can then be analysed and queried.

You can then define the audit points at which to capture events either by:

* using generic rules to capture all events in all camel routes
* specifically routing to an audit endpoint (e.g. **vm:audit**) explicitly in your routes when you choose to do so

### Specifying generic audit rules

To avoid having to explicitly add audit rules to your camel routes we recommend using the **Auditor**.

The Auditor is one or more beans configured in your applcation. The Auditor currently works by intercepting Camel events and writing them to a Camel Endpoint which can then use the various available [Camel Endpoints](http://camel.apache.org/components.html)

The implementation is currently based on the [Camel PublishEventNotifier](http://camel.apache.org/maven/current/camel-core/apidocs/org/apache/camel/management/PublishEventNotifier.html) plugin in Camel.


### Running the sample

Start a local [MongoDb](http://www.mongodb.org/).
Then run the following commands:

    cd bai
    mvn install
    cd bai-sample-camel
    mvn camel:run

A sample camel route should now be running which should have configured the auditing of exchanges to MongoDb.

You can now browse the **bai** database in MongoDb to see the events being stored. In a Mongo shell type

    use bai
    show collections
    db.baievents.findAll()



