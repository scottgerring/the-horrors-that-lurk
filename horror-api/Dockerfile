# Base image for building
FROM alpine:latest as builder

# Install build dependencies
RUN apk add --no-cache \
    gcc \
    g++ \
    make \
    musl-dev \
    openjdk21

# Set JAVA_HOME explicitly
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk

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
