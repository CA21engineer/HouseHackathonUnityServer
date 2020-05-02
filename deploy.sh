#!/bin/bash

cd `dirname $0`

# 第一引数はgitリポジトリを指定(以下例)
# sh deploy.sh https://github.com/dockersamples/example-voting-app

# 初回のみClone、以降はPullする
if cd app; then
  git pull;
else
  git clone $1 app
  cd app
fi

# 全て削除
#docker run \
#--rm -v /var/run/docker.sock:/var/run/docker.sock \
#-v "$PWD:/$PWD" -w="/$PWD" \
#docker/compose:1.22.0 \
#down --rmi all --volumes

if type "sbt" > /dev/null 2>&1; then
  sbt clean docker:stage
else
  if type "docker image inspect hseeberger/scala-sbt:8u222_1.3.10_2.12.8" > /dev/null 2>&1; then
    # あった場合は何もしない
  eles
    docker build \
      --build-arg BASE_IMAGE_TAG="8u212-b04-jdk-stretch" \
      --build-arg SBT_VERSION="1.3.10" \
      --build-arg SCALA_VERSION="2.12.8" \
      --build-arg USER_ID=1001 \
      --build-arg GROUP_ID=1001 \
      -t hseeberger/scala-sbt:8u222_1.3.10_2.12.8 \
      github.com/hseeberger/scala-sbt.git#:debian
  fi

  docker run \
  --rm -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$PWD:/$PWD" -w="/$PWD" \
  hseeberger/scala-sbt:8u222_1.3.10_2.12.8 \
  sbt clean docker:stage
fi


if type "docker-compose" > /dev/null 2>&1; then
  docker-compose -f docker-compose.yaml up --build -d
else
  docker run \
  --rm -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$PWD:/$PWD" -w="/$PWD" \
  docker/compose:1.22.0 \
  -f docker-compose.yaml up --build -d
fi
