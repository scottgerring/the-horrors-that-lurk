#!/bin/bash

set -e

#
# Linux - AMD64 and ARM64 builds
# 

# Build our amd64 and arm64 linux builds 
docker rm -f extract-native-build-arm64
docker buildx build --platform linux/arm64 -t native-build-arm64 .
docker create --name extract-native-build-arm64 native-build-arm64
docker cp extract-native-build-arm64:/build/libtag_library.so ./output/libtag-linux-arm64.so
docker rm -f extract-native-build-arm64

docker rm -f extract-native-build-amd64
docker buildx build --platform linux/amd64 -t native-build-amd64 .
docker create --name extract-native-build-amd64 native-build-amd64
docker cp extract-native-build-amd64:/build/libtag_library.so ./output/libtag-linux-amd64.so
docker rm -f extract-native-build-amd64

#
# MacOS - ARM64 build
#
. ~/.sdkman/bin/sdkman-init.sh
sdk use java 21-oracle
gcc -shared -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin"  src/native-src/TagLibrary.c -o output/libtag-darwin-arm64.so

#
# Copy everything into the JARs tree for staging
# 
mkdir -p src/main/resources/native
cp output/* src/main/resources/native/

