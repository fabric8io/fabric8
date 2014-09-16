## Kubernetes Guestbook example

This example shows how you can run controllers, pods and services on [Kubernetes](http://fabric8.io/gitbook/kubernetes.html) using a profile to represent each pod, controller and/or service.

To run this demo run the profiles in this order:

* [redis.master](/fabric/profiles/example/kubernetes/guestbook/redis.master.profile) runs the redis master
* [redis.slave](/fabric/profiles/example/kubernetes/guestbook/redis.slave.profile) runs the redis slave controller
* [frontend](/fabric/profiles/example/kubernetes/guestbook/frontend.profile) runs the frontend