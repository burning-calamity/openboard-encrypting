package org.dslul.openboard.inputmethod.latin.ciphers;

import java.util.Locale;

/** Vigenere polyalphabetic cipher. */
public final class VigenereCipher implements MessageCipher {
    private final String mKeyword;

    public VigenereCipher(final String keyword) {
        final String cleaned = keyword == null ? "" : keyword.toUpperCase(Locale.US).replaceAll("[^A-Z]", "");
        mKeyword = cleaned.isEmpty() ? "KEY" : cleaned;
    }

    @Override
    public String encrypt(final String input) {
        return transform(input, false);
    }

    @Override
    public String decrypt(final String input) {
        return transform(input, true);
    }

    private String transform(final String input, final boolean decrypt) {
        final StringBuilder output = new StringBuilder(input.length());
        int letters = 0;
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            final char upper = Character.toUpperCase(c);
            if (upper < 'A' || upper > 'Z') {
                output.append(c);
                continue;
            }
            final int shift = mKeyword.charAt(letters % mKeyword.length()) - 'A';
            final int base = upper - 'A';
            final int transformed = decrypt ? (base - shift + 26) % 26 : (base + shift) % 26;
            final char result = (char)('A' + transformed);
            output.append(Character.isLowerCase(c) ? Character.toLowerCase(result) : result);
            letters++;
        }
        return output.toString();
    }
}
