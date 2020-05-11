#!/bin/sh

docker build `dirname $0` -t protoc-gen-grpc-web:latest

mkdir -p $2
for file in `\find $1 -name '*.proto'`; do
    docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v "${PWD}:/${PWD}" -w="/${PWD}" protoc-gen-grpc-web \
    protoc \
    -I$1 \
    -I/usr/local/include/google \
    --plugin=protoc-gen-grpc=/usr/local/bin/grpc_csharp_plugin \
    --csharp_out=$2 \
    --grpc_out=$2 \
    $file
done
