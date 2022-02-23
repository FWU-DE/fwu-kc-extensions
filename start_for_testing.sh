#!/bin/bash

function cleanup {
  printf '\U1F433 %s\n' "Stopping Docker containers"
  docker-compose -f test/docker-compose.yaml down --volumes
}

trap cleanup EXIT

# start docker
docker-compose -f test/docker-compose.yaml up --build
