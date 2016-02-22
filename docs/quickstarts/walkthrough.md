### Walk through a simple quickstart

This is a more detailed guide showing step by step how to try the fabric8 quickstarts.

A [video of this walkthrough](https://vimeo.com/142658441) is also available.

First, make sure you have followed the [Get Started Guide](../getStarted/index.md)
so you should have things running and you have
[setup your local machine](../getStarted/local.md).

Please also check out
[how to develop applications locally](../getStarted/develop.md) that you have your
local tools available.

You can run any of the [quickstarts](index.md) either directly out of
a git checked out repository or from a project created by an
quickstart [archetype](archetype.md).

In this guide we will start with one of the simplest which is the `java-simple-fatjar` quickstart.

To get started you can checkout the quickstart source code using the following git command:

    git clone https://github.com/fabric8io/ipaas-quickstarts.git
    cd ipaas-quickstarts

And then change directory to 

    cd quickstart
    cd java
    cd simple-fatjar
    
 
#### Check your environment

In order to build docker images you must have access to a Docker
daemon. The easiest way is to setup the environment variable
`DOCKER_HOST`. 

Also for applying to OpenShift / Kubernetes, you need to login into OpenShift with `oc login`. See the documentation of [`fabric8:apply`](../mavenFabric8Apply.md) for
details. 

In this guide we assume you have setup the `DOCKER_HOST` environment, such as: 

    DOCKER_HOST="tcp://vagrant.f8:2375"

.. and using the OpenShift Client we can login to OpenShift with

    # Use admin/admin if using the default credentials
    oc login      
    

#### Build the application and run locally

Almost every quickstart can run locally, which allows you as a developer to quickly try it. First we need to build the project with Maven

    mvn clean install
    
We can then run the quickstart locally using:

    mvn exec:java
    
This quickstart is a simple Java standalone application that prints a random string to the console, as shown below:

```
[INFO] --- exec-maven-plugin:1.4.0:java (default-cli) @ quickstart-java-simple-fatjar ---
Hello Fabric8! Here's your random string: q83b6
Hello Fabric8! Here's your random string: 3ZyNE
Hello Fabric8! Here's your random string: B6uV4
Hello Fabric8! Here's your random string: hpcmf
```
You can use `ctrl + c` to stop the application.

As this quickstart is a _fat jar_ type of application you can also run it using `java -jar` as shown:

    java -jar target/quickstart-java-simple-fatjar-2.2.96.jar
    
Now lets try to build and deploy this application to docker and kubernetes.

#### Build the application and the Docker image

The Docker image can be easily created by using the following goals:

    mvn clean install docker:build

For you convenience, these goals are combined by using a
pre-configured goal:

    mvn -Pf8-build

Please note that by default the Docker username is "fabric8" and the
default registry is "docker.io". This works by default when you are
not pushing to a registry with `docker:push`. Please see
[changing Docker user and registry](http://fabric8.io/guide/quickstarts/running.html#changing-docker-user-and-registry) for
details how to push to a custom registry.

#### Deploy the application on Kubernetes / OpenShift

Now let's deploy the image into the Kubernetes environment:

    mvn fabric8:json fabric8:apply

Alternatively you can use the shortcut

    mvn -Pf8-local-deploy

which will include the install and `docker:build` steps. 

During the docker the docker daemon may need to pull docker images during the build which will show on the console. Have a bit patience as that can take a little time.

The application will be deployed and running on Kubernetes as a pod.


#### Showing the state of the deployment and view logs

After you have deployed the application, you often would like to see if everything is as expected. We can do this by listing all the running pods and their state:

    oc get pods
    
What you should see in the list, is a pod with the name of `quickstart-java-simple-fatjar` and a random suffix as shown below:

```
ipaas-quickstarts/quickstart/java/simple-fatjar/$ oc get pods
NAME                                  READY     STATUS    RESTARTS   AGE
docker-registry-1-68nz6               1/1       Running   0          6m
fabric8-jccyj                         1/1       Running   0          7m
fluentd-elasticsearch-172.28.128.4    1/1       Running   0          5m
quickstart-java-simple-fatjar-o8hxg   1/1       Running   0          9s
router-1-lz02h                        1/1       Running   0          6m
```

We can see our example is running with the pod name `quickstart-java-simple-fatjar-o8hxg`

To see the logs of the running pod we can type `oc logs <name>` such as:

    oc logs quickstart-java-simple-fatjar-o8hxg
    
That will dump the logs which can be _massive_. What you may want to do is to follow the logs using the `-f` flag, by doing:

    oc logs -f quickstart-java-simple-fatjar-o8hxg
    
And then the console is updated in real time, you can use `ctrl + c` to exit the logging.

#### Updating the source code

We would now like to update the source code so the application prints a different message to the log, and then redeploy the changed application.

To do that we start by changing the source code which you can load into your favorite Java editor, or use a plain text editor as the source code is so simple we can change it without a full blown Java editor.

The source code is in the `src/main/java/io/fabric8/quickstarts/java/simple/fatjar/Main.java` java file, which we change as follows:

```
public class Main {

    public static void main(String[] args) throws InterruptedException {
        while(true) {
            System.out.println("I was here: " + randomAlphanumeric(5));
            SECONDS.sleep(1);
        }
    }

}
```

Notice we changed the text to be `I was here: `

Then we can just deploy the application using the following goal:

    mvn -Pf8-local-deploy
    
Which will clean and compile the source, do a docker image build, and re-deploy to kubernetes.

When the application is de-deployed then kubernetes will shutdown the old pod, and start a new pod, so we will list all the running pods with `oc get pods`:

```
ipaas-quickstarts/quickstart/java/simple-fatjar/$ oc get pods
NAME                                  READY     STATUS    RESTARTS   AGE
docker-registry-1-68nz6               1/1       Running   0          15m
fabric8-jccyj                         1/1       Running   0          16m
fluentd-elasticsearch-172.28.128.4    1/1       Running   0          14m
quickstart-java-simple-fatjar-2nnx7   1/1       Running   0          42s
router-1-lz02h                        1/1       Running   0          15m
```

And as you can see the pod name has changed to `quickstart-java-simple-fatjar-2nnx7`. To see our code change we can show the logs of the pods with:

    oc logs -f quickstart-java-simple-fatjar-2nnx7
    
And you should see our changed logging message:

```
ipaas-quickstarts/quickstart/java/simple-fatjar/$ oc logs -f quickstart-java-simple-fatjar-2nnx7
I> No access restrictor found, access to all MBean is allowed
Jolokia: Agent started with URL http://172.17.0.7:8778/jolokia/
I was here: PorTM
I was here: GT7rh
I was here: gYQu0
I was here: hzEoO
I was here: YEk5a
```

#### Deleting the running quickstart

Kubernetes is designed as a resilliant platform, so for example if for some reason a pod is killed (a node crashes, or something), then the platform is self healing and will spin up a new pod.

So if you want to delete the running quickstart, you would either need to scale it down to 0, or delete the _replication controlller_ that is monitoring the pod state and would scale up or down pods depending on your _desired state_. 

So what we can do is to delete the controller using

    oc delete rc quickstart-java-simple-fatjar
    
Then the pod will automatic be shutdown and deleted as well.

You can get a list of all running _replication controllers_ with:

    oc get rc
   
Another easy way would be to use the fabric8 web console to delete the quickstat.


#### Closing remarks

Yay you made through this first walk through of the most simple quickstart we have. For the next walk lets up the game a bit and use Camel and Kubernetes services.