# Camel LoanBroker Example using ActiveMQ

This is the well known loan broker example from the [EIP patterns](http://www.eaipatterns.com/SystemManagementExample.html) book.

This example requires to run the following profiles on any given number of containers:

* A container with ActiveMQ (comes out of the box for JBoss Fuse)
* [mq.bank1](/fabric/profiles/example/camel/loanbroker/mq.bank1.profile) an example of a bank giving rates for loan requests.
* [mq.bank2](/fabric/profiles/example/camel/loanbroker/mq.bank2.profile) an example of another bank giving rates for loan requests.
* [mq.bank3](/fabric/profiles/example/camel/loanbroker/mq.bank3.profile) an example of another bank giving rates for loan requests.
* [mq.loanBroker](/fabric/profiles/example/camel/loanbroker/mq.loanBroker.profile) acts as the loan broker in communication with the banks to retrieve the best rate for a given client request.

For example you can run each bank in a seperate container, or combine the banks into a single container.

When all the containers has been deployed and started, then you can connect to the container running the loan broker application, and from its logs, you can see which bank offers the best rate (the lowest).

You can at any time stop one or more banks which influences which rate becomes the best.
