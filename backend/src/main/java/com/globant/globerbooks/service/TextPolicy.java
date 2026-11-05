package com.globant.globerbooks.service;

import java.text.Normalizer;
import java.util.Optional;
import java.util.Set;
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
    private static final int MAX_RUN = 8;
    private static final Set<Integer> BLANK_RENDERING = Set.of(0x115F, 0x1160, 0x3164, 0xFFA0, 0x2800);
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

    /** One linear pass over the code points; no backtracking and no regular expression. */
    private static boolean charactersAllowed(String text) {
        int[] points = text.codePoints().toArray();
        boolean visible = false;
        int runLength = 0;
        int selectors = 0;
        for (int index = 0; index < points.length; index++) {
            int point = points[index];
            if (isForbidden(point) || (index == 0 && isMark(point))) {
                return false;
            }
            if (isJoiner(point) && !joinerPlacementAllowed(points, index)) {
                return false;
            }
            visible = visible || isVisible(point);
            if (isRunMember(point)) {
                runLength++;
                selectors += isVariationSelector(point) ? 1 : 0;
                if (runLength > MAX_RUN || selectors > 1) {
                    return false;
                }
            } else {
                runLength = 0;
                selectors = 0;
            }
        }
        return visible;
    }

    private static boolean joinerPlacementAllowed(int[] points, int index) {
        if (index == 0 || index == points.length - 1) {
            return false;
        }
        return !isJoinerOrSpace(points[index - 1]) && !isJoinerOrSpace(points[index + 1]);
    }

    private static boolean isJoinerOrSpace(int point) {
        return isJoiner(point) || Character.getType(point) == Character.SPACE_SEPARATOR;
    }

    private static boolean isJoiner(int point) {
        return point == ZERO_WIDTH_NON_JOINER || point == ZERO_WIDTH_JOINER;
    }

    private static boolean isMark(int point) {
        return switch (Character.getType(point)) {
            case Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK,
                    Character.ENCLOSING_MARK -> true;
            default -> false;
        };
    }

    private static boolean isRunMember(int point) {
        return isMark(point) || isJoiner(point);
    }

    private static boolean isVariationSelector(int point) {
        return (point >= 0xFE00 && point <= 0xFE0F) || (point >= 0xE0100 && point <= 0xE01EF);
    }

    private static boolean isVisible(int point) {
        if (BLANK_RENDERING.contains(point)) {
            return false;
        }
        return switch (Character.getType(point)) {
            case Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER, Character.TITLECASE_LETTER,
                    Character.MODIFIER_LETTER, Character.OTHER_LETTER, Character.DECIMAL_DIGIT_NUMBER,
                    Character.LETTER_NUMBER, Character.OTHER_NUMBER, Character.CONNECTOR_PUNCTUATION,
                    Character.DASH_PUNCTUATION, Character.START_PUNCTUATION, Character.END_PUNCTUATION,
                    Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION,
                    Character.OTHER_PUNCTUATION, Character.MATH_SYMBOL, Character.CURRENCY_SYMBOL,
                    Character.MODIFIER_SYMBOL, Character.OTHER_SYMBOL -> true;
            default -> false;
        };
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
