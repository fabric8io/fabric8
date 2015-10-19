#!/bin/bash

### this script is used by cicle-ci to publish the website
### https://github.com/fabric8io/fabric8/blob/master/circle.yml#L33

echo ============================================================================
echo Deploying fabric8 website
echo ============================================================================

cd website && \
npm install -g gitbook-cli && \
npm install && \
mvn clean && \
mkdir -p target && \
cd target && \
git clone -b gh-pages git@github.com:fabric8io/fabric8.git sitegen && \
cd .. && \
mvn scalate:sitegen && \
mkdir -p target/sitegen/guide && \
mkdir -p ../docs/_book && \
gitbook install ../docs  && \
gitbook build ../docs && \
echo "copying generated gitbook"
cp -rv ../docs/_book/* target/sitegen/guide && \
cd target/sitegen && \
git add * guide/* && \
git commit -m "generated website" && \
git push origin gh-pages

#gitbook install ../docs --output=target/sitegen/guide && \


echo ============================================================================
echo Deployed fabric8 website
echo ============================================================================

