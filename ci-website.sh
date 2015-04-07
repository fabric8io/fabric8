#!/bin/bash

echo ============================================================================
echo Deploying fabric8 website
echo ============================================================================

cd website && \
npm install && \
mvn clean && \
mkdir -p target && \
cd target && \
git clone -b gh-pages git@github.com:fabric8io/fabric8.git sitegen && \
cd .. && \
mvn scalate:sitegen && \
mkdir -p target/sitegen/gitbook && \
gitbook build ../docs --output=target/sitegen/guide && \
cd target/sitegen && \
git add *  && \
git commit -m "generated website" && \
git push

echo ============================================================================
echo Deployed fabric8 website
echo ============================================================================

