#!/bin/bash

provider_dir=test/providers
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

mvn clean package -DskipTests
if [ -d $provider_dir ];then
  rm $provider_dir/* || true
else
  mkdir $provider_dir
fi

for jar in $(find . -name "*.jar" |grep target); do
  echo "cp $jar $provider_dir/"
  cp $jar $provider_dir/
done

cp test/lib/*.jar $provider_dir/

if [[ "$?" -ne 0 ]] ; then
  echo 'could not run maven package'; exit $rc
fi

# start docker
$compose -f test/docker-compose.yaml up --build
