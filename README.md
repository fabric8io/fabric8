# fabric8-helm-index-build
CI build to generate the helm index for fabric8 projects

To test out local builds of the index.yaml run:

    mvn install jetty:run
    helm repo add fabric8 http://localhost:8080/helm/
    helm search chaos
    
     