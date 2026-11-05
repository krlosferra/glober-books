package com.globant.globerbooks.service;

import static com.globant.globerbooks.support.UnicodeFixtures.ACUTE;
import static com.globant.globerbooks.support.UnicodeFixtures.ZWJ;
import static com.globant.globerbooks.support.UnicodeFixtures.baseWithAcutes;
import static com.globant.globerbooks.support.UnicodeFixtures.of;
import static com.globant.globerbooks.support.UnicodeFixtures.repeat;
import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Proves, independently of {@code TextPolicy}, that the pinned JDK carries the Unicode data the
 * spec relies on (plan-appendix, UnicodeFixtureCheckTest; BR-006 "The Unicode version is 15.0").
 * The expected values are the categories and compositions named in the spec, not observed output.
 */
class UnicodeFixtureCheckTest {

    private static final Map<Integer, Byte> CATEGORIES = Map.ofEntries(
            Map.entry(0x0000, Character.CONTROL), Map.entry(0x0009, Character.CONTROL),
            Map.entry(0x000A, Character.CONTROL), Map.entry(0x0085, Character.CONTROL),
            Map.entry(0x00A0, Character.SPACE_SEPARATOR), Map.entry(0x3000, Character.SPACE_SEPARATOR),
            Map.entry(0x2028, Character.LINE_SEPARATOR), Map.entry(0x2029, Character.PARAGRAPH_SEPARATOR),
            Map.entry(0x200B, Character.FORMAT), Map.entry(0x200C, Character.FORMAT),
            Map.entry(0x200D, Character.FORMAT), Map.entry(0x202E, Character.FORMAT),
            Map.entry(0xE0041, Character.FORMAT), Map.entry(0xE000, Character.PRIVATE_USE),
            Map.entry(0x0378, Character.UNASSIGNED), Map.entry(0xFFFE, Character.UNASSIGNED),
            Map.entry(0xFDD0, Character.UNASSIGNED), Map.entry(0x2FFC, Character.UNASSIGNED),
            Map.entry(0x3164, Character.OTHER_LETTER), Map.entry(0x115F, Character.OTHER_LETTER),
            Map.entry(0x1160, Character.OTHER_LETTER), Map.entry(0xFFA0, Character.OTHER_LETTER),
            Map.entry(0x2800, Character.OTHER_SYMBOL), Map.entry(0xFFFD, Character.OTHER_SYMBOL),
            Map.entry(0x1F600, Character.OTHER_SYMBOL), Map.entry(0x2764, Character.OTHER_SYMBOL),
            Map.entry(0x1F525, Character.OTHER_SYMBOL), Map.entry(0x1D15E, Character.OTHER_SYMBOL),
            Map.entry(0x0301, Character.NON_SPACING_MARK), Map.entry(0xFE0F, Character.NON_SPACING_MARK),
            Map.entry(0xE0100, Character.NON_SPACING_MARK), Map.entry(0xE0101, Character.NON_SPACING_MARK),
            Map.entry(0x1E030, Character.MODIFIER_LETTER));

    @Test
    void runsOnJdk21Update12OrLater() {
        Runtime.Version version = Runtime.version();
        assertThat(version.feature()).isEqualTo(21);
        assertThat(version.update()).isGreaterThanOrEqualTo(12);
    }

    @Test
    void carriesUnicode15Data() {
        assertThat(Character.isDefined(0x1E030)).as("U+1E030 is assigned in 15.0").isTrue();
        assertThat(Character.isDefined(0x2FFC)).as("U+2FFC is assigned only in 15.1").isFalse();
    }

    @Test
    void reportsTheCategoryOfEveryCodePointTheSpecNames() {
        CATEGORIES.forEach((point, expected) -> assertThat(Character.getType(point))
                .as("category of U+%04X", point).isEqualTo(expected.intValue()));
    }

    @Test
    void composesEWithAcuteButNotXWithAcute() {
        String composed = Normalizer.normalize(of('e', ACUTE), Normalizer.Form.NFC);
        String untouched = Normalizer.normalize(of('x', ACUTE), Normalizer.Form.NFC);

        assertThat(composed).isEqualTo("é");
        assertThat(untouched).isEqualTo(of('x', ACUTE));
    }

    @Test
    void decomposesU1D15EIntoTwoCodePoints() {
        String result = Normalizer.normalize(repeat(0x1D15E, 300), Normalizer.Form.NFC);

        assertThat(result.codePointCount(0, result.length())).isEqualTo(600);
    }

    @Test
    void leavesTheT24bAndT25InputsUnchangedByNfc() {
        String persian = "می‌خواهم";
        String run10 = baseWithAcutes(8) + of(ZWJ, ACUTE);
        String fifty = "x" + (repeat(ACUTE, 8) + of(ZWJ)).repeat(50) + "y";

        assertThat(Normalizer.normalize(persian, Normalizer.Form.NFC)).isEqualTo(persian);
        assertThat(Normalizer.normalize(run10, Normalizer.Form.NFC)).isEqualTo(run10);
        assertThat(Normalizer.normalize(fifty, Normalizer.Form.NFC)).isEqualTo(fifty);
        assertThat(fifty.codePointCount(0, fifty.length())).isEqualTo(1 + 50 * 9 + 1);
    }
}
