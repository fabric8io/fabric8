## Annotation Processors

Fabric8 provides annotations and processors for manipulating Kubernetes resources at compile time using the Fabric8 Kubernetes API.
The processors are meant to be used as a supplement or an alternative to the [Maven Plugin](mavenPlugin.html).

Using the annotation processors one can easily use Java to:

- Use Java Fluent Builders to generate Kubernetes resources
- Use Java to modify existing Kubernetes resources


Both features are provided by the *kubernetes-generator* module.

    <dependency>
        <groupId>io.fabric8</groupId>
        <artifactId>kubernetes-generator</artifactId>
    </dependency>

### Generating Kubernetes Resources

To generate a kubernetes json file all you need to do is to add the *@KubernetesProvider* on a method that returns a KubernetesList or Template.
For example:

    @KubernetesProvider
    public KubernetesList create() {
        return new KubernetesListBuilder()
                .addNewReplicationControllerItem()
                    .withNewMetadata()
                        .withName("Hello-Controller")
                    .endMetadata()
                    .withNewSpec()
                        .withReplicas(1)
                        .addToSelector("component", "my-component")
                        .withNewTemplate()
                            .withNewSpec()
                                .addNewContainer()
                                    .withName("my-container")
                                    .withImage("my/image")
                                .endContainer()
                            .endSpec()
                        .endTemplate()
                    .endSpec()
                .endReplicationControllerItem()
                .build();
    }

When a class that contains a method like this gets compiled, it will generate a kubernetes.json file in the build output folder.
The particular method will generate a json file that will look like:

        {
          "apiVersion": "v1",
          "kind": "List",
          "items": [
            {
              "apiVersion": "v1",
              "kind": "ReplicationController",
              "metadata": {
                "annotations": {},
                "labels": {
                  "component": "jenkins",
                  "provider": "fabric8"
                },
                "name": "my-controller"
              },
              "spec": {
                "replicas": 1,
                "selector": {
                  "component": "my-component",
                },
                "template": {
                  "metadata": {
                    "labels": {
                      "component": "my-component",
                    }
                  },
                  "spec": {
                    "containers": [
                      {
                        "image": "my/container",
                        "name": "my-container"
                      }
                    ]
                  }
                }
              }
            }
          ]
        }
        
You can also have multiple methods annotated with `@KubernetesProvider` and specify specific kubernetes.json files you
wish to generate. For example, `@KubernetesProvider("foo.json")` will generate the previous manifest as `target/classes/foo.json`:

    @KubernetesProvider("foo.json")
    public KubernetesList create() {
        return new KubernetesListBuilder()
                .addNewReplicationControllerItem()
                    .withNewMetadata()
                        .withName("Hello-Controller")
                    .endMetadata()
                    .withNewSpec()
                        .withReplicas(1)
                        .addToSelector("component", "my-component")
                        .withNewTemplate()
                            .withNewSpec()
                                .addNewContainer()
                                    .withName("my-container")
                                    .withImage("my/image")
                                .endContainer()
                            .endSpec()
                        .endTemplate()
                    .endSpec()
                .endReplicationControllerItem()
                .build();
    }    
    
`@KubernetesProvider` annotation can also generate YAML files. When you give the file a name as in above, and you use an
extension of `.yaml` or `.yml`, the processor will automatically generate YAML for you. 

So what is the benefit of using java instead of json? The most noteworthy are:

- Type Safety
- Dynamic generation of JSON
- Tooling support (IDEs etc)

And what are the benefits over using the [Maven Plugin](mavenPlugin.html)?

- No limitation to the structure or the number of the generated items.
- Doesn't require maven. Can be used with gradle or even directly with the java compiler.

### Manipulating existing Kubernetes Resources
There are cases, where you need to modify an existing Kubernetes resource instead of generating one from scratch. Cases like this can be:

- Adding a service
- Adding/Removing labels to selectors
- Adding/Removing a container to a pod

Due to the deep nesting of those objects, any kind of manipulation is not trivial and requires to know exactly how to navigate the object/json graph.

To make this task simpler all the builders objects that are used to model the Kubernetes resources support the *"Visitor Pattern"* and Fabric8 offers annotations that
make it very easy to use them.

The @KubernetesModelProcessor can be added to any Java Class that is intended to manipulate an existing kubernetes json file.
Upon compilation the annotation processor will look for all methods in the class that accept as argument a builder object of a Kubernetes resource (e.g. A Pod, a Service, a ReplicationController etc).
Then it will navigate through all the elements to the existing json file and for each element will invoke all matching methods.

For example:

"We want to add a key/pair to all replica selectors"

    @KubernetesModelProcessor
    public class ManipulationExample {
        public void on(ReplicationControllerSpecBuilder builder) {
            builder.addToSelector("mykey","myvalue");
        }
    }

An other example can be "Add a container to all pods"

    @KubernetesModelProcessor
    public class ManipulationExample {
        public void on(PodTemplateSpecBuilder builder) {
            builder.withNewSpec()
                    .addNewContainer()
                        .withName("my-container)
                        .withImage("my/image")
                    .endContainer()
            .endSpec();
        }
    }
    
You can also manipulate existing kubernetes resources with a specific file name by passing in the file name to the `@KubernetesModelProcessor`
annotation like this:

    @KubernetesModelProcessor("foo.json")
    public class ManipulationExample {
        public void on(PodTemplateSpecBuilder builder) {
            builder.withNewSpec()
                    .addNewContainer()
                        .withName("my-container)
                        .withImage("my/image")
                    .endContainer()
            .endSpec();
        }
    }


In many cases we would want to apply a change not to *"ALL"* items of a particular type, but to some or to a specific one.
For this purpose the annotation processor used the @Named annotation (javax.inject.Named) to specify the name of element we want to manipulate.

The last example could use the @Named annotation to specify the name of the replication controller.


    @KubernetesModelProcessor
    public class ManipulationExample {

        @Named("my-controller")
        public void on(ReplicationController builder) {
            builder.withNewSpec()
                        .withNewTemplate(builder.getSpec().getTemplate())
                            .addNewContainer()
                                .withName("my-container)
                                .withImage("my/image")
                            .endContainer()
                        .endTemplate()
                    .endSpec();
            }
        }

Using the @Named annotation we specified that we want this method to be applied to all replication controllers that are named "my-controller".
Then starting from the replication controller of our choice we performed the desired manipulation (added a new container).

#### Performing manipulation on resources generated by the maven plugin.

In some cases it may be handy to generate the skeleton of the json file using the [Maven Plugin](mavenPlugin.html) and then apply minor modifications (for fine tuning) using the annotation processors.

It is important to remember that the maven plugin is using as default maven phase the "generate-resources", while the annotation processors are using the compile phase.
This makes possible doing something like:

        mvn clean fabric8:json compile

This will cause the maven plugin to generate the kubernetes.json file during the "generated-sources" and if a valid @KubernetesModelProcessor is found in the sources, it will be used modify the generated kubernetes.json file.