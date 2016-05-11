## Circuit Breakers

Adding circuit breakers via the [Hystrix](https://github.com/Netflix/Hystrix) library helps you provide a fallback if any dependent service either goes down or goes too slow.

If you are using Hystrix then you can run the **kubeflix** application which runs the Hystrix dashboard application to help you visualise the state of your hystrix circuit breakers.

For more background on using Hystrix with Kubernetes check out the [kubeflix project](https://github.com/fabric8io/kubeflix)