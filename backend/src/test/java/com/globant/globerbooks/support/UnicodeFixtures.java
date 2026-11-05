package com.globant.globerbooks.support;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Code-point fixtures for the text policy tests. Every string is built from numeric code points so
 * that no source-file encoding or editor can alter a fixture. The pinned-data guard is independent
 * of the production text policy.
 */
public final class UnicodeFixtures {

    /** Combining acute accent, category Mn. */
    public static final int ACUTE = 0x0301;
    /** Zero width non-joiner, category Cf, allowed by BR-006. */
    public static final int ZWNJ = 0x200C;
    /** Zero width joiner, category Cf, allowed by BR-006. */
    public static final int ZWJ = 0x200D;
    /** Variation selector 16, category Mn. */
    public static final int VS16 = 0xFE0F;
    /** Hangul filler, category Lo, renders blank. */
    public static final int HANGUL_FILLER = 0x3164;

    private UnicodeFixtures() {
    }

    /**
     * Builds a string from code points.
     *
     * @param points the code points in order
     * @return the string
     */
    public static String of(int... points) {
        return new String(points, 0, points.length);
    }

    /**
     * Repeats one code point.
     *
     * @param point the code point
     * @param times the repetition count
     * @return the string
     */
    public static String repeat(int point, int times) {
        return new String(Character.toChars(point)).repeat(times);
    }

    /**
     * Builds the base "x" followed by combining acute accents.
     *
     * @param marks the number of U+0301 after the base
     * @return the string
     */
    public static String baseWithAcutes(int marks) {
        return "x" + repeat(ACUTE, marks);
    }

    /**
     * Fails fast when the JDK does not carry the pinned Unicode data. Rule tests call this in
     * their class setup so that a data mismatch stops them before any rule is judged.
     */
    public static void requirePinnedData() {
        assertThat(Runtime.version().feature()).isEqualTo(21);
        assertThat(Runtime.version().update()).isGreaterThanOrEqualTo(12);
        assertThat(Character.isDefined(0x1E030)).isTrue();
        assertThat(Character.isDefined(0x2FFC)).isFalse();
    }
}
