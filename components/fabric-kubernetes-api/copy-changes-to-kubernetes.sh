#!/bin/sh
echo "Copying local changes back to the kubernetes fork"
cp -r src/main/kubernetes/api ../../../kubernetes
