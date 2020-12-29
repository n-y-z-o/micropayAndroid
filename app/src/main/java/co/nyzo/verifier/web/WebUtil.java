package co.nyzo.verifier.web;

import java.util.HashMap;
import java.util.Map;

import co.nyzo.mobile.MapUtil;

public class WebUtil {

    private static final Map<Character, String> characterToPercentEncodingMap = new HashMap<>();
    private static final Map<String, Character> percentEncodingToCharacterMap = new HashMap<>();
    static {
        characterToPercentEncodingMap.put('!', "%21");
        characterToPercentEncodingMap.put('#', "%23");
        characterToPercentEncodingMap.put('$', "%24");
        characterToPercentEncodingMap.put('%', "%25");
        characterToPercentEncodingMap.put('&', "%26");
        characterToPercentEncodingMap.put('\'', "%27");
        characterToPercentEncodingMap.put('(', "%28");
        characterToPercentEncodingMap.put(')', "%29");
        characterToPercentEncodingMap.put('*', "%2A");
        characterToPercentEncodingMap.put('+', "%2B");
        characterToPercentEncodingMap.put(',', "%2C");
        characterToPercentEncodingMap.put('/', "%2F");
        characterToPercentEncodingMap.put(':', "%3A");
        characterToPercentEncodingMap.put(';', "%3B");
        characterToPercentEncodingMap.put('=', "%3D");
        characterToPercentEncodingMap.put('?', "%3F");
        characterToPercentEncodingMap.put('@', "%40");
        characterToPercentEncodingMap.put('[', "%5B");
        characterToPercentEncodingMap.put(']', "%5D");
        characterToPercentEncodingMap.put('~', "%7E");

        for (Character character : characterToPercentEncodingMap.keySet()) {
            percentEncodingToCharacterMap.put(characterToPercentEncodingMap.get(character), character);
        }
    }

    public static String applyPercentEncoding(String value) {
        StringBuilder result = new StringBuilder();
        for (char character : value.toCharArray()) {
            result.append(MapUtil.getOrDefault(characterToPercentEncodingMap, character,
                    character + ""));
        }

        return result.toString();
    }

    public static String removePercentEncoding(String value) {
        StringBuilder result = new StringBuilder();
        char[] characters = value.toCharArray();
        for (int i = 0; i < characters.length; i++) {
            char character = characters[i];
            if (character == '+') {
                result.append(' ');
            } else if (character == '%') {
                String encoding = character + "";
                if (i < characters.length - 1) {
                    encoding += characters[i + 1];
                }
                if (i < characters.length - 2) {
                    encoding += characters[i + 2];
                }
                result.append(MapUtil.getOrDefault(percentEncodingToCharacterMap, encoding, ' '));
                i += 2;
            } else {
                result.append(character);
            }
        }

        return result.toString();
    }
}

