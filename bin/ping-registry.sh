#!/bin/bash

echo "Ping docker registry at DOCKER_REGISTRY = $DOCKER_REGISTRY"
curl http://$DOCKER_REGISTRY/v1/_ping
echo ""
