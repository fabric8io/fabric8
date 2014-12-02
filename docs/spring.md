## Using Spring framework with Fabric8

This chapter contains guidelines for people willing to use Fabric8 in order to provision and manage Spring-based
applications.

### Spring Dynamic Modules (deprecated)

In the older versions of the Karaf, ServiceMix and Fuse we recommended to use 
[Spring Dynamic Modules](http://docs.spring.io/osgi/docs/1.2.1/reference/html) to deploy applications based on the
Spring framework. As Spring DM project is not actively maintained anymore, we currently don't recommend to use it. 
Moreover due to the fact that there is no other alternative for Spring deployments in the OSGi environment, we don't 
recommend provisioning Spring-based applications into the OSGi anymore.

