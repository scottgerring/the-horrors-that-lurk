# Compiler and flags
CC := gcc
CFLAGS := -shared -fPIC

# Set JAVA_HOME only if it is not already set in the environment
JAVA_HOME ?= /usr/lib/jvm/java-21-openjdk
JAVAC := javac

# Discover our JNI includes based on platform, then add to the CFLAGS
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S), Darwin)
    JNI_PLATFORM := darwin
    LDFLAGS := -dynamiclib
else
    JNI_PLATFORM := linux
    LDFLAGS := -shared
endif
CFLAGS += -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(JNI_PLATFORM)

# Directories
SRC_DIR := src/native-src
OBJ_DIR := build
JAVA_SRC := src/main/java/com/horror/TagLibrary.java
JNI_HEADER := $(SRC_DIR)/com_horror_TagLibrary.h
LIBRARY := $(OBJ_DIR)/libtag_library.so

# Find all C source files and corresponding object files
SOURCES := $(wildcard $(SRC_DIR)/*.c)
OBJECTS := $(patsubst $(SRC_DIR)/%.c, $(OBJ_DIR)/%.o, $(SOURCES))

# Default target: Build the shared library
all: $(LIBRARY)

# Generate JNI header
$(JNI_HEADER): $(JAVA_SRC)
	$(JAVAC) -h $(SRC_DIR) $(JAVA_SRC)

# Compile C
$(OBJ_DIR)/%.o: $(SRC_DIR)/%.c $(JNI_HEADER) | $(OBJ_DIR)
	$(CC) $(CFLAGS) -c $< -o $@

# Link C
$(LIBRARY): $(OBJECTS)
	$(CC) $(CFLAGS) $(LDFLAGS) -o $@ $^

# Create target dir
$(OBJ_DIR):
	mkdir -p $(OBJ_DIR)

# Clean build artifacts
clean:
	rm -rf $(OBJ_DIR) $(JNI_HEADER)

# fail:
# 	find /build
# 	echo Failed!
# 	exit 1

.PHONY: all clean 
