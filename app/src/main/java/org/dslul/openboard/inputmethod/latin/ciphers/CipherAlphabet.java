package org.dslul.openboard.inputmethod.latin.ciphers;

/** Shared alphabets for classical substitutions across several keyboard scripts. */
final class CipherAlphabet {
    private static final String[] ALPHABETS = {
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            "abcdefghijklmnopqrstuvwxyz",
            "ΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ",
            "αβγδεζηθικλμνξοπρστυφχψω",
            "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ",
            "абвгдеёжзийклмнопрстуфхцчшщъыьэюя",
            "ԱԲԳԴԵԶԷԸԹԺԻԼԽԾԿՀՁՂՃՄՅՆՇՈՉՊՋՌՍՎՏՐՑՒՓՔՕՖ",
            "աբգդեզէըթժիլխծկհձղճմյնշոչպջռսվտրցւփքօֆ",
            "აბგდევზთიკლმნოპჟრსტუფქღყშჩცძწჭხჯჰ",
            "אבגדהוזחטיכלמנסעפצקרשת",
            "ابتثجحخدذرزسشصضطظعغفقكلمنهوي"
    };

    private CipherAlphabet() {
    }

    static boolean contains(final int codePoint) {
        if (!Character.isBmpCodePoint(codePoint)) {
            return false;
        }
        final char character = (char)codePoint;
        if (character == 'ς') {
            return true;
        }
        return findAlphabet(character) != null;
    }

    static char shift(final char character, final int amount) {
        final char normalized = character == 'ς' ? 'σ' : character;
        final String alphabet = findAlphabet(normalized);
        if (alphabet == null) {
            return character;
        }
        final int index = alphabet.indexOf(normalized);
        final int shiftedIndex = positiveMod(index + amount, alphabet.length());
        return alphabet.charAt(shiftedIndex);
    }

    static char reverse(final char character) {
        final char normalized = character == 'ς' ? 'σ' : character;
        final String alphabet = findAlphabet(normalized);
        if (alphabet == null) {
            return character;
        }
        return alphabet.charAt(alphabet.length() - 1 - alphabet.indexOf(normalized));
    }

    private static String findAlphabet(final char character) {
        for (String alphabet : ALPHABETS) {
            if (alphabet.indexOf(character) >= 0) {
                return alphabet;
            }
        }
        return null;
    }

    private static int positiveMod(final int value, final int modulus) {
        return ((value % modulus) + modulus) % modulus;
    }
}
