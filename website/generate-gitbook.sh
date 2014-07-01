#!/bin/sh

mkdir -p target/sitegen/gitbook
gitbook build ../docs --output=target/sitegen/gitbook
gitbook pdf ../docs --output=target/sitegen/fabric8.pdf