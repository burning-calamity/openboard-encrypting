package org.dslul.openboard.inputmethod.latin.ciphers;

/** Educational stepping substitution inspired by the historical Japanese Purple machine. */
public final class PurpleCipher implements MessageCipher {
    private static final String[] ALPHABETS = {
            "QWERTYUIOPASDFGHJKLZXCVBNM",
            "MNBVCXZLKJHGFDSAPOIUYTREWQ",
            "PHQGIUMEAYLNOFDXJKRCVSTZWB"
    };
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Override
    public String encrypt(final String input) {
        return transform(input, false);
    }

    @Override
    public String decrypt(final String input) {
        return transform(input, true);
    }

    private static String transform(final String input, final boolean decrypt) {
        final StringBuilder output = new StringBuilder(input.length());
        int letters = 0;
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            final char upper = Character.toUpperCase(c);
            final int index = ALPHABET.indexOf(upper);
            if (index < 0) {
                output.append(c);
                continue;
            }
            final String alphabet = ALPHABETS[letters % ALPHABETS.length];
            final char result = decrypt ? ALPHABET.charAt(alphabet.indexOf(upper)) : alphabet.charAt(index);
            output.append(Character.isLowerCase(c) ? Character.toLowerCase(result) : result);
            letters++;
        }
        return output.toString();
    }
}
