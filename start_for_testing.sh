#!/bin/bash

which docker-compose
if [ $? != 0 ]; then
  compose="docker compose"
else
  compose="docker-compose"
fi

function cleanup {
  printf '\U1F433 %s\n' "Stopping Docker containers"
  $compose -f test/docker-compose.yaml down --volumes
}

trap cleanup EXIT

# build extension without SNAPSHOT suffix
mvn clean package -DskipTests -DprojectVersion=docker
if [[ "$?" -ne 0 ]] ; then
  echo 'could not run maven package'; exit $rc
fi

# start docker
$compose -f test/docker-compose.yaml up --build
