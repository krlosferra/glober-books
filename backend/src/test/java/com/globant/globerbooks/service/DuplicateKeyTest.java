package com.globant.globerbooks.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Tests of the BR-003 duplicate key (tasks.md Task 22 criteria 1 to 3; US-004.5, T-40). */
class DuplicateKeyTest {

    private static String fromCodePoints(int... points) {
        return new String(points, 0, points.length);
    }

    private static boolean matches(String titleA, String authorA, String titleB, String authorB) {
        return DuplicateKey.of(TextPolicy.normalize(titleA), TextPolicy.normalize(authorA))
                .equals(DuplicateKey.of(TextPolicy.normalize(titleB), TextPolicy.normalize(authorB)));
    }

    @Test
    void ignoresCaseSurroundingAndRepeatedWhiteSpace() {
        assertThat(matches("Clean Code", "Robert Martin", " clean   code ", "ROBERT MARTIN")).isTrue();
    }

    @Test
    void fullWidthFormMatchesThroughNfkc() {
        String fullWidth = fromCodePoints(0xFF23) + "lean Code";

        assertThat(fullWidth).isNotEqualTo("Clean Code");
        assertThat(matches("Clean Code", "Robert Martin", fullWidth, "Robert Martin")).isTrue();
    }

    @Test
    void sharpSDoesNotMatchDoubleS() {
        assertThat(matches("Stra" + fromCodePoints(0xDF) + "e", "a", "STRASSE", "a")).isFalse();
    }

    @Test
    void sameAuthorCaseVariantsOfOneTitleMatch() {
        assertThat(matches("Clean Code", "b", "CLEAN CODE", "b")).isTrue();
    }

    @Test
    void differentTitleOrDifferentAuthorDoesNotMatch() {
        assertThat(matches("Clean Code", "a", "Clean Code", "b")).isFalse();
        assertThat(matches("Clean Code", "a", "Clean Coder", "a")).isFalse();
    }

    @Test
    void pairKeyIsNotSwappable() {
        assertThat(matches("a", "b", "b", "a")).isFalse();
    }

    @Test
    void collapsesEveryUnicodeWhiteSpaceRunIncludingNoBreakAndIdeographicSpace() {
        String spaced = "a" + fromCodePoints(0xA0, 0x2003, 0x3000, 0x09, 0x0A, 0x2028, 0x202F, 0x85) + "b";

        assertThat(DuplicateKey.textKey(spaced)).isEqualTo("a b");
    }

    @Test
    void asciiControlsOutsideWhiteSpaceAreNotCollapsed() {
        assertThat(DuplicateKey.textKey("a" + fromCodePoints(0x1C) + "b")).hasSize(3);
    }

    @Test
    void lowerCasingIsLocaleIndependent() {
        java.util.Locale previous = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"));

            assertThat(DuplicateKey.textKey("TITLE")).isEqualTo("title");
            assertThat(DuplicateKey.generalKey("TITLE")).isEqualTo("title");
        } finally {
            java.util.Locale.setDefault(previous);
        }
    }

    @Test
    void dottedCapitalIUsesTheRootMapping() {
        assertThat(DuplicateKey.textKey(fromCodePoints(0x130))).isEqualTo(fromCodePoints('i', 0x307));
    }

    @Test
    void emptyAndWhiteSpaceOnlyTextsGiveAnEmptyKey() {
        assertThat(DuplicateKey.textKey("")).isEmpty();
        assertThat(DuplicateKey.textKey("  \t ")).isEmpty();
        assertThat(DuplicateKey.generalKey(fromCodePoints(0xA0, 0x3000))).isEmpty();
    }

    @Test
    void supplementaryCharactersSurviveTheGeneralPath() {
        String clef = fromCodePoints(0x1D11E);

        assertThat(DuplicateKey.textKey("x " + clef)).isEqualTo("x " + clef);
    }

    @Test
    void asciiBoundaryCharactersAreHandledLikeTheGeneralPath() {
        List<String> samples = List.of("", " ", "A", "Z", "@", "[", "`", "{", "a", "z", fromCodePoints(0x7F),
                fromCodePoints(0), fromCodePoints(0x1F), fromCodePoints(0x09), fromCodePoints(0x0D), " A  z ",
                "@A[ `a{", fromCodePoints(0x1C) + " x " + fromCodePoints(0x1D));

        for (String sample : samples) {
            assertThat(DuplicateKey.asciiKey(sample)).as("sample length %d", sample.length())
                    .isEqualTo(DuplicateKey.generalKey(sample));
        }
    }

    @Test
    void asciiFastPathEqualsTheGeneralPathOnTenThousandGeneratedPairs() {
        Random random = new Random(22L);
        int pairs = 0;
        for (int round = 0; round < 10_000; round++) {
            String title = randomAscii(random);
            String author = randomAscii(random);

            assertThat(DuplicateKey.asciiKey(title)).isEqualTo(DuplicateKey.generalKey(title));
            assertThat(DuplicateKey.asciiKey(author)).isEqualTo(DuplicateKey.generalKey(author));
            assertThat(DuplicateKey.textKey(title)).isEqualTo(DuplicateKey.generalKey(title));
            pairs++;
        }
        assertThat(pairs).isEqualTo(10_000);
    }

    @Test
    void keyIsIdempotentOnGeneratedAndUnicodeSamples() {
        Random random = new Random(23L);
        List<String> unicode = List.of("Stra" + fromCodePoints(0xDF) + "e", fromCodePoints(0xFF23) + "lean  Code",
                fromCodePoints(0x130, 0x3000, 0x1D11E, 0x212B), "e" + fromCodePoints(0x301) + " X");
        for (String sample : unicode) {
            String once = DuplicateKey.textKey(sample);
            assertThat(DuplicateKey.textKey(once)).isEqualTo(once);
        }
        for (int round = 0; round < 2_000; round++) {
            String once = DuplicateKey.textKey(randomAscii(random));
            assertThat(DuplicateKey.textKey(once)).isEqualTo(once);
        }
    }

    @Test
    void whiteSpaceEqualsTheUnicodeWhiteSpacePropertyOverAllCodePoints() {
        Pattern property = Pattern.compile("\\p{IsWhite_Space}");
        int disagreements = 0;
        for (int point = 0; point <= Character.MAX_CODE_POINT; point++) {
            boolean expected = property.matcher(new String(Character.toChars(point))).matches();
            if (DuplicateKey.isWhiteSpace(point) != expected) {
                disagreements++;
            }
        }

        assertThat(disagreements).isZero();
    }

    @Test
    void toStringNeverShowsTheKeys() {
        String text = DuplicateKey.of("Secret Title", "Secret Author").toString();

        assertThat(text).doesNotContainIgnoringCase("secret");
    }

    private static String randomAscii(Random random) {
        int length = random.nextInt(24);
        StringBuilder text = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            text.append((char) (random.nextInt(4) == 0 ? ' ' : random.nextInt(128)));
        }
        return text.toString();
    }
}
