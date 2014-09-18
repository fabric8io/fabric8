## HDFS

This profile lets you create an Hadoop File System 2.2 containers on your network with the role of NameNode.

Since the profile has to download ~80MB from the internet that it cannot cache, be aware that each installation might take a while.

Use `hdfs.datanode` profile to communicate with this `Namenode`

Notes:

- No specific confgiuration for MapReduce as been provided. Anyone expert in HDFS configuration is welcome to contribute to these templates.
- Possibly an intervention on `slaves` file could be required when adding new Datanode coming from different hosts/ips.
