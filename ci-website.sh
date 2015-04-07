#!/bin/sh

echo ============================================================================
echo Deploying fabric8 website
echo ============================================================================
#rm -rf fabric8

cd website && \
npm install && \
mvn clean && \
mkdir -p target && \
pushd target && \
git clone -b gh-pages git@github.com:fabric8io/fabric8.git sitegen && \
popd && \
mvn scalate:sitegen && \
mkdir -p target/sitegen/gitbook && \
gitbook build ../docs --output=target/sitegen/guide && \
pushd target/sitegen && \
git add *  && \
git commit -m "generated website" && \
git push

#gitbook build ../docs --output=target/sitegen/gitbook && \
#mvn scalate:deploy
