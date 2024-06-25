#!/bin/bash

mvn clean package -Dmaven.test.skip=true

if [[ "$?" -ne 0 ]] ; then
  echo 'could not run maven package'; exit 1
fi

# start docker
docker compose up --build --detach
