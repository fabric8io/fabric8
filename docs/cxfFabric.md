## CXF Fabric

Like the [Camel Fabric](camelFabric.md) the **CXF Fabric** makes it easy to expose physical web services into the Runtime Registry so that clients can just bind to the logical name of the service and at runtime locate and load balance across the running implementations.

Basically, your CXF endpoint will know nothing about the Fabric runtime, and you just need to configure the FabricLoadBalanceFeature with the ZKClientFactoryBean and set the feature into the bus, the FabricLoadBalanceFeature will interact with Fabric runtime automatically.

You can configure the fabricPath of the FabricLoadBalancerFeature for the CXF server and CXF client to use.

    <beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns:cxfcore="http://cxf.apache.org/core"
           xsi:schemaLocation="
             http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
             http://cxf.apache.org/core http://cxf.apache.org/schemas/core.xsd">

      <bean id="zkClient" class="io.fabric8.zookeeper.spring.ZKClientFactoryBean">
        <property name="timeoutText" value="30s"/>
        <property name="connectString" value="localhost:2181"/>
      </bean>

      <bean id="fabicLoadBalancerFeature" class="io.fabric8.cxf.FabricLoadBalancerFeature">
          <property name="zkClient" ref="zkClient" />
          <property name="fabricPath" value="simple" />
      </bean>
      <!-- configure the feature on the bus -->
      <cxfcore:bus>
        <cxfcore:features>
          <ref bean="fabicLoadBalancerFeature" />
        </cxfcore:features>
      </cxfcore:bus>

    </beans>

### Exposing a CXF endpoint into the fabric

    // You just need to make sure the bus is configured with the FabricLoadBalanceFeature
    ServerFactoryBean factory = new ServerFactoryBean();
    factory.setServiceBean(new HelloImpl());
    factory.setAddress("http://localhost:9000/simple/server");
    factory.setBus(bus);
    factory.create();

Here you can use the CXF normal code to publish the CXF service, but you need to make sure the FabricLoadBalancerFeature is registered into the bus that you service will be published.

### Invoking a CXF fabric endpoint

    ClientProxyFactoryBean clientFactory = new ClientProxyFactoryBean();
    clientFactory.setServiceClass(Hello.class);
    // The address is not the actual address that the client will access
    clientFactory.setAddress("http://someotherplace");
    List<AbstractFeature> features = new ArrayList<AbstractFeature>();
    Feature feature = new FabricFailOverFeature();
    // you need setting the feature with zkClient and fabricPath to load the server addresses
    ......
    // add the instance of FabricLoadBalancerFeature into features list
    features.add(feature);
    // we need to setup the feature on the clientfactory
    clientFactory.setFeatures(features);
    Hello hello = clientFactory.create(Hello.class);
    String response = hello.sayHello();
    System.out.println(response);

For the CXF client, you just need to set the FabricLoadBalancerFeature directly into the clientFactory. The FabricLoadBalancerFeature will pick a real service physical address for the Fabric service registry for the client to use.