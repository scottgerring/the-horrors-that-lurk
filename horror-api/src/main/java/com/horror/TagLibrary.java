import java.util.HashMap;
import java.util.Map;

public class TagLibrary {
    static {
        System.loadLibrary("tag_library"); // Load the native library
    }

    // Declare the native method
    public native Map<String, String> getTags();

    public static void main(String[] args) {
        TagLibrary library = new TagLibrary();
        Map<String, String> tags = library.getTags();
        System.out.println("Tags: " + tags);
    }
}

