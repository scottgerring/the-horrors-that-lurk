#!/bin/bash

set -e

# Ensure the output directory exists
mkdir -p ./output

echo "Building for linux"
docker build --platform linux/amd64 -t extract-native-build .
docker create --name extract-extract-native-build extract-native-build
docker cp extract-extract-native-build:/app/libtag_library.so ./output/libtag.so
docker rm -f extract-extract-native-build

