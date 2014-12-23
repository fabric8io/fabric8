## Kubernetes Template

This library provides a simple template mechanism to generate Kuberetes JSON files using MVEL templates (with a default template included).

Here is an [example of generating the JSON](https://github.com/fabric8io/fabric8/blob/master/components/kubernetes-template/src/test/java/io/fabric8/kubernetes/TemplateGeneratorTest.java#L51).

You create a [TemplateGenerator](https://github.com/fabric8io/fabric8/blob/master/components/kubernetes-template/src/main/java/io/fabric8/kubernetes/template/TemplateGenerator.java#L37) class using a [GenerateTemplateDTO](https://github.com/fabric8io/fabric8/blob/master/components/kubernetes-template/src/main/java/io/fabric8/kubernetes/template/GenerateTemplateDTO.java#L30) which has the various properties like the name, labels, ports, environment variables etc.

### Add it to your Maven pom.xml

To be able to use this library add this to your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8</groupId>
                <artifactId>kubernetes-template</artifactId>
                <version>2.0.18</version>
            </dependency>


