package io.fabric8.utils;

/**
 * Helper class for working with override strings for patches in Fabric.
 *
 * The format for these strings allows appending a version range to the URL, to specify which bundles are eligible for
 * update by the override.
 */
public class PatchUtils {

    /*
     * Directive used when appending a version range
     */
    private static final String OVERRIDE_RANGE = ";range=";

    private PatchUtils() {
        // utility class
    }

    /**
     * Appends a version range to a url
     *
     * @param url the original url
     * @param range the version range
     * @return override string with the version range appended to the original url
     */
    public static String appendVersionRange(String url, String range) {
        return url + OVERRIDE_RANGE + range;
    }

    /**
     * Extract the URL from an override string
     */
    public static String extractUrl(String override) {
        return override.split(OVERRIDE_RANGE)[0];
    }

    /**
     * Extract the version range from an override string - returns <code>null</code> if no version range is available
     */
    public static String extractVersionRange(String override) {
        return override.contains(OVERRIDE_RANGE) ? override.split(OVERRIDE_RANGE)[1] : null;
    }

}
