## Fabric8 MQ Auto Scaler

The Fabric8 MQ Auto Scaler [App](apps.html) monitors and scales the [Apache ActiveMQ](http://activemq.apache.org/) brokers running on Kubernetes.

The MQ autoscaler uses [kubernetes servicse](services.html) to monitor a group of MQ brokers, matching a group of labels defined by ENV variables **AMQ_SERVICE_ID** (default is _fabricMQ_), and **AMQ_GROUP_NAME** (default is _default_).

The MQ Auto Scaler will examine all MQ brokers run in the group defined by the **AMQ_GROUP_NAME** and spin up additional brokers, or remove them.

After changing the number of running MQ brokers, the auto scaler will request some clients reconnect, to balance the load.
