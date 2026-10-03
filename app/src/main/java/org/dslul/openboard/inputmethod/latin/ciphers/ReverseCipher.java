package org.dslul.openboard.inputmethod.latin.ciphers;

/** Reciprocal Unicode code-point reversal utility. */
public final class ReverseCipher implements MessageCipher {
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
        for (int offset = input.length(); offset > 0;) {
            final int codePoint = Character.codePointBefore(input, offset);
            output.appendCodePoint(codePoint);
            offset -= Character.charCount(codePoint);
        }
        return output.toString();
    }
}
