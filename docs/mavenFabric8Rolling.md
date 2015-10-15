## fabric8:rolling

The maven `fabric8:rolling` goal takes the JSON file generated via [mvn fabric8:json](mavenFabric8Json.html) located at `target/classes/kubernetes.json` and applies it to the current Kubernetes environment and namespace creating all resources which don't exist and performing a **rolling update** of any [replication controllers](replicationControllers.html) which already  exist. 

This goal is short hand for 

```
mvn fabric8:apply -Dfabric8.rolling=true
```

See the  [fabric8:apply](mavenFabric8Apply.html) goal for more details on the available [properties](mavenFabric8Apply.html#maven-properties)
