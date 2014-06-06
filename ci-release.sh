#!/bin/sh

export MAVEN_OPTS="-Xmx986m -XX:MaxPermSize=350m"
echo ============================================================================
echo Building version ${release_version}
echo ============================================================================

$dev_version=1.1.0-SNAPSHOT
rm -rf fabric8

git clone git@github.com:fabric8io/fabric8.git fabric8 && \
cd fabric8 && \
mvn --batch-mode \
  org.apache.maven.plugins:maven-release-plugin:2.5:prepare \
  -P all,release \
  -DpreparationGoals=&quot;clean verify -DskipTests=true&quot; \
  -Dtag=fabric8-${release_version} \
  -DreleaseVersion=${release_version} \
  -DdevelopmentVersion=${dev_version} && \
mvn --batch-mode org.apache.maven.plugins:maven-release-plugin:2.5::perform -P all,release \
 -DpreparationGoals=&quot;clean verify -DskipTests=true&quot; && \
git push --tags
