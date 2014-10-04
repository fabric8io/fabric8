#!/bin/sh
echo "Removing all docker containers"
docker kill $(docker ps -q)
docker rm $(docker ps -a -q)
