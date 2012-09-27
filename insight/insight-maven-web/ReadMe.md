Fuse Insight Maven
==================

This project aims to give you insight into your dependencies along with deiferences between versions.

Start by seeing the version dependencies, dependency deltas between product releases
and even generating Legal reports.

To run type

  cd insight-maven-web
  mvn jetty:run

then open: http://localhost:8080/insight/projects

Note that the example projects are really just URLs; you can make up your own URI of the form

 * http://localhost:8080/insight/projects/project/GROUPID/ARTIFACTID/jar//VERSION

for a product information, or for a comparison of 2 versions...

* http://localhost:8080/insight/projects/compare/GROUPID/ARTIFACTID/jar//VERSION1/VERSION2

Note that the /jar// part is really /EXTENSION/CLASSIFIER/. Typically the EXTENSION is "jar" and CLASSIFIER is ""