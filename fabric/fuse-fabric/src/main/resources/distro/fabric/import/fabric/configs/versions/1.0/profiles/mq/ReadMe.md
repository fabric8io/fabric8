## JBoss A-MQ

These profiles all create [JBoss A-MQ message brokers](http://www.jboss.org/products/amq)

* [amq](/fabric/profiles/mq/amq.profile) the full JBoss A-MQ distribution including the JBoss A-MQ web console. Though as of JBoss Fuse 6.1 we recommend using the Fuse Management Console now instead
* [base](/fabric/profiles/mq/base.profile) the base profile all other profiles inherit from
* [default](/fabric/profiles/mq/default.profile) a default message broker profile if you don't need the A-MQ web console
* [replicated](/fabric/profiles/mq/replicated.profile) a message broker using message replication
* [webconsole](/fabric/profiles/mq/webconsole.profile) the JBoss A-MQ web console
