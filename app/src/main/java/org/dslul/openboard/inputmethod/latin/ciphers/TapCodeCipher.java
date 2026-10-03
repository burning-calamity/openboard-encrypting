package org.dslul.openboard.inputmethod.latin.ciphers;

import java.util.Locale;

/** Classical 5-by-5 tap code. C and K share a cell; slash separates words. */
public final class TapCodeCipher implements MessageCipher {
    private static final String ALPHABET = "ABCDEFGHIJLMNOPQRSTUVWXYZ";

    @Override
    public String encrypt(final String input) {
        final StringBuilder output = new StringBuilder();
        final String upper = input.toUpperCase(Locale.ROOT);
        for (int i = 0; i < upper.length(); i++) {
            char value = upper.charAt(i);
            if (value == 'K') {
                value = 'C';
            }
            if (Character.isWhitespace(value)) {
                appendToken(output, "/");
                continue;
            }
            final int index = ALPHABET.indexOf(value);
            if (index < 0) {
                appendToken(output, String.valueOf(value));
                continue;
            }
            appendToken(output, dots(index / 5 + 1) + " " + dots(index % 5 + 1));
        }
        return output.toString();
    }

    @Override
    public String decrypt(final String input) {
        final StringBuilder output = new StringBuilder();
        final String[] tokens = input.trim().split("\\s+");
        for (int i = 0; i < tokens.length;) {
            if ("/".equals(tokens[i])) {
                output.append(' ');
                i++;
            } else if (i + 1 < tokens.length && isDots(tokens[i]) && isDots(tokens[i + 1])) {
                final int index = (tokens[i].length() - 1) * 5 + tokens[i + 1].length() - 1;
                if (index >= ALPHABET.length()) {
                    throw new IllegalArgumentException("Tap groups must contain one to five dots");
                }
                output.append(ALPHABET.charAt(index));
                i += 2;
            } else {
                output.append(tokens[i]);
                i++;
            }
        }
        return output.toString();
    }

    private static void appendToken(final StringBuilder output, final String token) {
        if (output.length() > 0) {
            output.append(' ');
        }
        output.append(token);
    }

    private static String dots(final int count) {
        return ".....".substring(0, count);
    }

    private static boolean isDots(final String token) {
        return token.matches("\\.{1,5}");
    }
}
