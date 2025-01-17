#!/bin/bash

set -e

#
# Linux - AMD64 and ARM64 builds
# 

# Build our amd64 and arm64 linux builds 
docker buildx build --platform linux/arm64,linux/amd64 -t native-build .

# Extract the libraries
docker create --name extract-native-build native-build
docker cp extract-native-build:/output ./output
docker rm extract-native-build

#
# MacOS - ARM64 build
#
. ~/.sdkman/bin/sdkman-init.sh
sdk use java 21-oracle
gcc -shared -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" src/native-src/TagLibrary.c -o output/libtag-darwin-arm64.so

#
# Copy everything into the JARs tree for staging
# 
mkdir -p src/main/resources/native
cp output/* src/main/resources/native/

