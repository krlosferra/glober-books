package com.globant.globerbooks.service;

import java.text.Normalizer;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The text policy of BR-006 for titles and authors: trim (Unicode White_Space), NFC, length in code
 * points, then the character rules. All functions are pure, hold no framework types and never log or
 * return the text they judge. Invalid text is rejected, never repaired.
 */
public final class TextPolicy {

    /** Maximum length of a title in code points (BR-006 item 5). */
    public static final int TITLE_MAX = 500;

    /** Maximum length of an author in code points (BR-006 item 5). */
    public static final int AUTHOR_MAX = 200;

    private static final Pattern WHITE_SPACE = Pattern.compile("\\p{IsWhite_Space}");
    private static final int ZERO_WIDTH_NON_JOINER = 0x200C;
    private static final int ZERO_WIDTH_JOINER = 0x200D;
    private static final int FIRST_TAG = 0xE0000;
    private static final int LAST_TAG = 0xE007F;

    private TextPolicy() {
    }

    /** The first failing check, in the order of BR-006: required, empty, length, characters. */
    public enum Problem {
        /** The value is null or missing. */
        REQUIRED,
        /** The value is empty after trimming. */
        EMPTY,
        /** The value is longer than the limit in code points after NFC. */
        TOO_LONG,
        /** The value contains characters that BR-006 items 6 to 6d do not allow. */
        FORBIDDEN_CHARACTERS
    }

    /**
     * Returns the stored form of a text: white space removed at both ends, then NFC.
     *
     * @param raw the text, not null
     * @return the trimmed NFC text
     */
    public static String normalize(String raw) {
        return Normalizer.normalize(trim(raw), Normalizer.Form.NFC);
    }

    /**
     * Checks a text against BR-006.
     *
     * @param raw the text as received, or null when missing
     * @param maxCodePoints the limit in code points, {@link #TITLE_MAX} or {@link #AUTHOR_MAX}
     * @return the first failing check, or empty when the text is acceptable
     */
    public static Optional<Problem> check(String raw, int maxCodePoints) {
        if (raw == null) {
            return Optional.of(Problem.REQUIRED);
        }
        String text = normalize(raw);
        if (text.isEmpty()) {
            return Optional.of(Problem.EMPTY);
        }
        if (text.codePointCount(0, text.length()) > maxCodePoints) {
            return Optional.of(Problem.TOO_LONG);
        }
        return charactersAllowed(text) ? Optional.empty() : Optional.of(Problem.FORBIDDEN_CHARACTERS);
    }

    private static String trim(String raw) {
        int start = 0;
        int end = raw.length();
        while (start < end) {
            int point = raw.codePointAt(start);
            if (!isWhiteSpace(point)) {
                break;
            }
            start += Character.charCount(point);
        }
        while (end > start) {
            int point = raw.codePointBefore(end);
            if (!isWhiteSpace(point)) {
                break;
            }
            end -= Character.charCount(point);
        }
        return raw.substring(start, end);
    }

    private static boolean isWhiteSpace(int point) {
        return WHITE_SPACE.matcher(new String(Character.toChars(point))).matches();
    }

    private static boolean charactersAllowed(String text) {
        return text.codePoints().noneMatch(TextPolicy::isForbidden);
    }

    private static boolean isForbidden(int point) {
        if (point >= FIRST_TAG && point <= LAST_TAG) {
            return true;
        }
        if (isNoncharacter(point)) {
            return true;
        }
        return switch (Character.getType(point)) {
            case Character.CONTROL, Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR,
                    Character.PRIVATE_USE, Character.SURROGATE, Character.UNASSIGNED -> true;
            case Character.FORMAT -> point != ZERO_WIDTH_NON_JOINER && point != ZERO_WIDTH_JOINER;
            default -> false;
        };
    }

    private static boolean isNoncharacter(int point) {
        return (point >= 0xFDD0 && point <= 0xFDEF) || (point & 0xFFFE) == 0xFFFE;
    }
}
