package org.gadgetbridge.kaitai.aux;

/**
 * Utility functions to manipulate names.
 */
public final class KaitaiNames {
    private KaitaiNames() {
    }

    public static String pascalCase(final String snakeCase) {
        final StringBuilder sb = new StringBuilder();
        for (final String part : snakeCase.split("_")) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    public static String stripPrefix(final String s, final String prefix) {
        return s.startsWith(prefix) ? s.substring(prefix.length()) : s;
    }

    public static String stripSuffix(final String s, final String suffix) {
        return s.endsWith(suffix) ? s.substring(0, s.length() - suffix.length()) : s;
    }
}
