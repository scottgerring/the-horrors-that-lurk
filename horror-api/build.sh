#!/bin/bash

set -e

# Function to build, create, and extract a native build for a given architecture
extract_native_build() {
    local platform=$1
    local tag="native-build-${platform##*/}" # Use platform suffix for tag
    local container_name="extract-${tag}"
    local output_file="./output/libtag-${platform##*/}.so"

    # Ensure the output directory exists
    mkdir -p ./output

    echo "Building for platform: $platform"
    docker rm -f "$container_name" >/dev/null 2>&1 || true
    docker buildx build --platform "$platform" -t "$tag" .

    echo "Creating container for $platform"
    docker create --name "$container_name" "$tag"

    echo "Copying build artifact for $platform"
    docker cp "$container_name:/build/libtag_library.so" "$output_file"

    echo "Cleaning up container for $platform"
    docker rm -f "$container_name"
}

# Extract for each desired platform
extract_native_build "linux/arm64"
extract_native_build "linux/amd64"

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

