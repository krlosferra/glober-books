package com.globant.globerbooks.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * The duplicate key of a (title, author) pair (BR-003, DEC-001). A text key is computed from the already
 * trimmed, NFC-normalized text: NFKC, lower case with the default Unicode mapping ({@code Locale.ROOT},
 * not case folding), every run of Unicode White_Space inside collapsed to one space, then trimmed.
 * Keys are application logic only: they are never part of the API, never logged and never in a body, so
 * {@link #toString()} hides them.
 *
 * @param titleKey key of the title
 * @param authorKey key of the author
 */
public record DuplicateKey(String titleKey, String authorKey) {

    private static final int ASCII_LIMIT = 0x80;
    private static final int CASE_OFFSET = 'a' - 'A';

    /**
     * Computes the pair key.
     *
     * @param title stored (trimmed, NFC) title
     * @param author stored (trimmed, NFC) author
     * @return the pair key
     */
    public static DuplicateKey of(String title, String author) {
        return new DuplicateKey(textKey(title), textKey(author));
    }

    /**
     * Computes the key of one text. Pure ASCII text takes a fast path that gives the same result as
     * {@link #generalKey(String)}.
     *
     * @param text the stored text, not null
     * @return the key
     */
    public static String textKey(String text) {
        return isAscii(text) ? asciiKey(text) : generalKey(text);
    }

    /**
     * Computes the key with the general path (NFKC, lower case, collapse, trim) for any text.
     *
     * @param text the text, not null
     * @return the key
     */
    static String generalKey(String text) {
        String lower = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(lower.length());
        boolean pendingSpace = false;
        int index = 0;
        while (index < lower.length()) {
            int point = lower.codePointAt(index);
            index += Character.charCount(point);
            if (isWhiteSpace(point)) {
                pendingSpace = result.length() > 0;
            } else {
                if (pendingSpace) {
                    result.append(' ');
                    pendingSpace = false;
                }
                result.appendCodePoint(point);
            }
        }
        return result.toString();
    }

    /**
     * Computes the key of pure ASCII text without the normalizer.
     *
     * @param text ASCII text, not null
     * @return the key
     */
    static String asciiKey(String text) {
        StringBuilder result = new StringBuilder(text.length());
        boolean pendingSpace = false;
        for (int index = 0; index < text.length(); index++) {
            char letter = text.charAt(index);
            if (isWhiteSpace(letter)) {
                pendingSpace = result.length() > 0;
            } else {
                if (pendingSpace) {
                    result.append(' ');
                    pendingSpace = false;
                }
                result.append(letter >= 'A' && letter <= 'Z' ? (char) (letter + CASE_OFFSET) : letter);
            }
        }
        return result.toString();
    }

    private static boolean isAscii(String text) {
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) >= ASCII_LIMIT) {
                return false;
            }
        }
        return true;
    }

    /** The Unicode White_Space property, listed by code point (UCD PropList). */
    private static boolean isWhiteSpace(int point) {
        return (point >= 0x09 && point <= 0x0D) || point == 0x20 || point == 0x85 || point == 0xA0
                || point == 0x1680 || (point >= 0x2000 && point <= 0x200A) || point == 0x2028
                || point == 0x2029 || point == 0x202F || point == 0x205F || point == 0x3000;
    }

    /**
     * Hides the keys.
     *
     * @return a fixed text
     */
    @Override
    public String toString() {
        return "DuplicateKey[hidden]";
    }
}
