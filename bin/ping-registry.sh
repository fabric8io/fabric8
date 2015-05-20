#!/bin/bash

echo "Ping docker registry at DOCKER_REGISTRY = $DOCKER_REGISTRY"
curl http://$DOCKER_REGISTRY/v2/
echo ""
