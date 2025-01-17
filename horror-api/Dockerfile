# Base image for building
FROM alpine:latest as builder

# Install build dependencies
RUN apk add --no-cache \
    gcc \
    g++ \
    make \
    musl-dev \
    openjdk21

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk

# Set working directory
WORKDIR /build

# Copy the native source code
COPY src/native-src .

# Build for linux-arm64
FROM --platform=linux/arm64 builder AS build-arm64
RUN gcc -shared -fPIC -I"$JAVA_HOME/include" \
    -I"$JAVA_HOME/include/linux" \
    -o /build/libtag_library.so TagLibrary.c

# Build for linux-amd64
FROM --platform=linux/amd64 builder AS build-amd64
RUN gcc -shared -fPIC -I"$JAVA_HOME/include" \
    -I"$JAVA_HOME/include/linux" \
    -o /build/libtag_library.so TagLibrary.c


# Final stage to collect binaries
FROM alpine:latest as output

# Copy ARM64 binary
COPY --from=build-arm64 /build/libtag_library.so output/libtag-linux-arm64.so

# Copy AMD64 binary
COPY --from=build-amd64 /build/libtag_library.so output/libtag-linux-amd64.so


