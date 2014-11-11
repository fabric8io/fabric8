#!/bin/bash

test -d ~/go || mkdir ~/go
export GOPATH=~/go

export PATH=$GOPATH/bin:$PATH

echo "This may take a while - getting & building latest cadvisor & dependencies locally"
echo "Be patient - it's worth it ;)"
go get -d github.com/google/cadvisor
go get github.com/tools/godep

cd $GOPATH/src/github.com/google/cadvisor/deploy
./build.sh

