#!/bin/sh

echo ============================================================================
echo Deploying fabric8 website
echo ============================================================================
rm -rf fabric8

git clone git@github.com:fabric8io/fabric8.git fabric8 && \
cd fabric8 && \
cd website && \
mvn clean scalate:sitegen && \
mvn scalate:deploy