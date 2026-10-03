package org.dslul.openboard.inputmethod.latin.ciphers;

/** Reciprocal ROT13 substitution for Latin letters. */
public final class Rot13Cipher implements MessageCipher {
    @Override
    public String encrypt(final String input) {
        return transform(input);
    }

    @Override
    public String decrypt(final String input) {
        return transform(input);
    }

    private static String transform(final String input) {
        final StringBuilder output = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            final char value = input.charAt(i);
            if (value >= 'a' && value <= 'z') {
                output.append((char)('a' + (value - 'a' + 13) % 26));
            } else if (value >= 'A' && value <= 'Z') {
                output.append((char)('A' + (value - 'A' + 13) % 26));
            } else {
                output.append(value);
            }
        }
        return output.toString();
    }
}
