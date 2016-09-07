#!/bin/bash

echo ============================================================================
echo Copying the index.yaml to the website
echo ============================================================================

cd target && \
rm -f website/helm/index.* && \    
git clone -b gh-pages git@github.com:fabric8io/fabric8.git website && \
cd website && \
mkdir -p helm && \
cp ../fabric8/helm-index.yaml helm/index.yaml && \
cp ../fabric8/helm-index.html helm/index.html && \
git add helm/* && \
git commit -m "updated helm index" && \
git push origin gh-pages

echo ============================================================================
echo Deployed new helm index to the website
echo ============================================================================

