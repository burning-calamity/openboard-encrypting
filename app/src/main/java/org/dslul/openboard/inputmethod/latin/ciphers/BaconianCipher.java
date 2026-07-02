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
            } else if (Character.isWhitespace(c)) {
                if (output.length() > 0) {
                    output.append(' ');
                }
                output.append('/');
            } else {
                if (output.length() > 0) {
                    output.append(' ');
                }
                output.append(c);
            }
        }
        return output.toString();
    }

    @Override
    public String decrypt(final String input) {
        final StringBuilder output = new StringBuilder(input.length() / 5);
        final String[] tokens = input.toUpperCase(Locale.US).trim().split("\\s+");
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            if ("/".equals(token)) {
                output.append(' ');
                continue;
            }
            token = token.replaceAll("[^AB]", "");
            if (token.length() != 5) {
                continue;
            }
            for (int j = 0; j < CODES.length; j++) {
                if (CODES[j].equals(token)) {
                    output.append((char)('A' + j));
                    break;
                }
            }
        }
        return output.toString();
    }
}
