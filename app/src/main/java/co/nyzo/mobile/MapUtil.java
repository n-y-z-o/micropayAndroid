package co.nyzo.mobile;

import java.util.Map;

public class MapUtil {

    // This class is a simple solution to provide an easy-to-read getOrDefault solution for maps
    // that does not rely on API version 24 or higher. When a sufficient share of the Android
    // population is at API 24 or higher, this class will be eliminated in favor of using the
    // getOrDefault() method of the Map class.

    public static char getOrDefault(Map<String, Character> map, String key, char defaultValue) {
        Character value = map.get(key);
        if (value == null) {
            value = defaultValue;
        }

        return value;
    }

    public static String getOrDefault(Map<Character, String> map, Character key, String defaultValue) {
        String value = map.get(key);
        if (value == null) {
            value = defaultValue;
        }

        return value;
    }

    public static int getOrDefault(Map<Character, Integer> map, Character key, int defaultValue) {
        Integer value = map.get(key);
        if (value == null) {
            value = defaultValue;
        }

        return value;
    }

    public static String getOrDefault(Map<String, String> map, String key, String defaultValue) {
        String value = map.get(key);
        if (value == null) {
            value = defaultValue;
        }

        return value;
    }
}
