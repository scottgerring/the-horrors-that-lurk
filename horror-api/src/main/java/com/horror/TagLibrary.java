package com.horror;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class TagLibrary {
    static {
        loadNativeLibrary(); // Dynamically load the native library
    }

    // Declare the native method
    public native Map<String, String> getTags();

    public static void main(String[] args) {
        TagLibrary library = new TagLibrary();
        Map<String, String> tags = library.getTags();
        System.out.println("Tags: " + tags);
    }

    /**
     * Dynamically determines the correct library name based on the platform.
     *
     * @return the name of the library to load
     */
    private static void loadNativeLibrary() {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();

        // Determine the library name based on the OS and architecture
        String libraryName = "libtag-";
        if (osName.contains("win")) {
            throw new UnsupportedOperationException("Windows is not supported.");
        } else if (osName.contains("mac")) {
            libraryName += "darwin-";
        } else if (osName.contains("linux")) {
            libraryName += "linux-";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }

        if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            libraryName += "arm64.so";
        } else if (osArch.contains("x86_64") || osArch.contains("amd64")) {
            libraryName += "amd64.so";
        } else {
            throw new UnsupportedOperationException("Unsupported architecture: " + osArch);
        }

        // Load the library from the JAR's resources
        try {
            loadFromJar(libraryName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load native library: " + libraryName, e);
        }
    }

        public static void loadFromJar(String libraryName) throws IOException {
        // Locate the library in the JAR
        InputStream libraryStream = TagLibrary.class.getResourceAsStream("/" + libraryName);
        if (libraryStream == null) {
            throw new IOException("Native library not found in JAR: " + libraryName);
        }

        // Create a temporary file to extract the library
        File tempFile = File.createTempFile(libraryName, null);
        tempFile.deleteOnExit();

        // Copy the library from the JAR to the temporary file
        Files.copy(libraryStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        libraryStream.close();

        // Load the extracted library
        System.load(tempFile.getAbsolutePath());
    }

}
