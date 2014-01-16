## JBoss A-MQ

These profiles all create [JBoss A-MQ message brokers](http://www.jboss.org/products/amq).

Rather than using these profiles directly you maybe want to try using the [MQ Console](#/fabric/mq/brokers) which lets you easily create groups of brokers along with replicated or master/slave clusters.

Here are the broker profiles in detail:

* [amq](/fabric/profiles/mq/amq.profile) the full JBoss A-MQ distribution, which starts the broker at 61616 port
* [base](/fabric/profiles/mq/base.profile) the base profile all other profiles inherit from
* [default](/fabric/profiles/mq/default.profile) a default message broker profile
* [replicated](/fabric/profiles/mq/replicated.profile) a message broker using message replication
* [webconsole](/fabric/profiles/mq/webconsole.profile) the JBoss A-MQ web console

### Examples

Once you have a broker running, try the messaging examples

* [jms quickstart](/fabric/profiles/example/quickstarts/jms.profile) demonstrates how to send and receive messages with JMS using [Apache ActiveMQ](http://activemq.apache.org/)
* [mq example](/fabric/profiles/example/mq.profile) an example for working with [Apache ActiveMQ](http://activemq.apache.org/) and Fuse
* [mq and camel example](/fabric/profiles/example/camel/mq.profile) demonstrates using Camel with an [A-MQ message broker profile](/fabric/profiles/mq)
