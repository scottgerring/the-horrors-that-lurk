#!/bin/bash

set -e

# Ensure the output directory exists
mkdir -p ./output

echo "Building for linux"
docker build --platform linux/amd64 -t native-build-amd64 .
docker create --name extract-native-build-amd64 native-build-amd64
docker cp extract-native-build-amd64:/app/libtag_library.so ./output/libtag-linux.so
docker rm -f extract-native-build-amd64

# MacOS - ARM64 build
. ~/.sdkman/bin/sdkman-init.sh
sdk use java 21-oracle
gcc -shared -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" src/native-src/TagLibrary.c -o output/libtag-darwin.so
