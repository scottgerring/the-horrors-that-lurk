# Base image for building
FROM ubuntu:oracular as builder

# Install build dependencies
RUN apt-get update && apt-get install -y build-essential openjdk-21-jdk-headless

# Set working directory
WORKDIR /build

# Copy source files
COPY src src
COPY Makefile .

# Build using Makefile
RUN make clean all VERBOSE=1

# Final stage: minimal runtime image
FROM alpine:latest

# Set working directory
WORKDIR /app

# Copy the built shared library from the builder stage
COPY --from=builder /build/build/libtag_library.so .

# Default command (modify as needed)
CMD ["sh"]
