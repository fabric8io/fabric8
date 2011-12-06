A _Fuse Bundle_, or _FAB_, is any jar created using [Apache Maven](http://maven.apache.org/) or [similar build tools](faq.html#Which_build_tools_generate_FABs_) so that inside the jar there is a _pom.xml_ file at

{pygmentize:: text}
META-INF/maven/groupId/artifactId/pom.xml (and pom.properties file)
{pygmentize}

which contain the transitive dependency information for the jar.
