## Kubernetes Integration Tests

To run these integration tests against an Kubernetes environment make sure you have set **$KUBERNETES_MASTER** to point to your environment.

Then run:

    mvn test -Dtest=BrokerProducerConsumerIT

When an Arquillian Fbaric8 integration test runs its creates a new namespace in Kubernetes for each test instance.
