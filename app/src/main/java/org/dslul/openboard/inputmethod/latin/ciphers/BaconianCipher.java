package org.dslul.openboard.inputmethod.latin.ciphers;

import java.util.Locale;

/** Baconian cipher using five-character A/B groups. */
public final class BaconianCipher implements MessageCipher {
    private static final String[] CODES = {
            "AAAAA", "AAAAB", "AAABA", "AAABB", "AABAA", "AABAB", "AABBA", "AABBB",
            "ABAAA", "ABAAB", "ABABA", "ABABB", "ABBAA", "ABBAB", "ABBBA", "ABBBB",
            "BAAAA", "BAAAB", "BAABA", "BAABB", "BABAA", "BABAB", "BABBA", "BABBB",
            "BBAAA", "BBAAB"
    };

    @Override
    public String encrypt(final String input) {
        final StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            final int index = Character.toUpperCase(c) - 'A';
            if (index >= 0 && index < CODES.length) {
                if (output.length() > 0) {
                    output.append(' ');
                }
                output.append(CODES[index]);
            } else if (!Character.isWhitespace(c)) {
                output.append(c);
            }
        }
        return output.toString();
    }

    @Override
    public String decrypt(final String input) {
        final String normalized = input.toUpperCase(Locale.US).replaceAll("[^AB]", "");
        final StringBuilder output = new StringBuilder(normalized.length() / 5);
        for (int i = 0; i + 4 < normalized.length(); i += 5) {
            final String code = normalized.substring(i, i + 5);
            for (int j = 0; j < CODES.length; j++) {
                if (CODES[j].equals(code)) {
                    output.append((char)('A' + j));
                    break;
                }
            }
        }
        return output.toString();
    }
}
