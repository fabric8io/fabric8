## fabric8:recreate

The maven `fabric8:recreate` goal takes the JSON file generated via [mvn fabric8:json](mavenFabric8Json.html) located at `target/classes/kubernetes.json` and applies it to the current Kubernetes environment and namespace creating all resources which don't exist and **recreates** any resources which do exist. 

This goal is short hand for 

```
mvn fabric8:apply -Dfabric8.recreate=true
```

See the  [fabric8:apply](mavenFabric8Apply.html) goal for more details on the available [properties](mavenFabric8Apply.html#maven-properties)
