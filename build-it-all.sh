#!/bin/bash

set -e

#
# Builds everything, and to and including our runtime docker images.
#

# First the API
pushd horror-api
rm -rf output/*
./build-native.sh
./gradlew build publishToMavenLocal
popd

# Then the app
pushd dives-java
./mvnw clean
./mvnw package
docker build . -f src/main/docker/Dockerfile.alpine -t dive-api:latest 
popd
