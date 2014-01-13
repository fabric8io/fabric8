AutoScale
=========

Adding the AutoScale Profile will enable _auto-scaling_.

Auto-scaling means that any new requirements for profiles which are not currently running are automatically created. For example if you create a logical pair of message brokers via the MQ tooling, the pair of brokers are automatically created when auto-scaling is enabled.

Typically this is most useful when working with a PaaS such as [OpenShift](http://openshift.com) but can be useful when trying things out in development as well.


