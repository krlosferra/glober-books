package com.globant.globerbooks.service;

import static com.globant.globerbooks.support.UnicodeFixtures.ACUTE;
import static com.globant.globerbooks.support.UnicodeFixtures.of;
import static com.globant.globerbooks.support.UnicodeFixtures.repeat;
import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.service.TextPolicy.Problem;
import com.globant.globerbooks.support.UnicodeFixtures;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests of BR-006 items 1 to 6: trim, NFC, length in code points and forbidden characters. */
class TextPolicyTest {

    @BeforeAll
    static void pinnedUnicodeDataIsPresent() {
        UnicodeFixtures.requirePinnedData();
    }

    private static Optional<Problem> title(String text) {
        return TextPolicy.check(text, TextPolicy.TITLE_MAX);
    }

    private static Optional<Problem> author(String text) {
        return TextPolicy.check(text, TextPolicy.AUTHOR_MAX);
    }

    static Stream<String> acceptedExamples() {
        return Stream.of("Les Misérables", "Victor Hugo", "  Clean Code  ", of(0xA0) + "Clean Code",
                repeat(0x1F600, 500), "a" + of(0xFFFD) + "b", "two  spaces", "Mixed CASE");
    }

    @ParameterizedTest
    @MethodSource("acceptedExamples")
    void acceptsTheSpecPassingExamples(String text) {
        assertThat(title(text)).isEmpty();
    }

    @Test
    void reportsRequiredForNull() {
        assertThat(title(null)).contains(Problem.REQUIRED);
    }

    static Stream<String> emptyExamples() {
        return Stream.of("", "   ", of(0xA0), of(0x09, 0x0A, 0x20), of(0x85, 0x2028, 0x2029, 0x3000));
    }

    @ParameterizedTest
    @MethodSource("emptyExamples")
    void reportsEmptyWhenOnlyWhiteSpaceRemains(String text) {
        assertThat(title(text)).contains(Problem.EMPTY);
    }

    static Stream<Integer> whiteSpaceCodePoints() {
        return Stream.of(0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x20, 0x85, 0xA0, 0x1680, 0x2000, 0x200A,
                0x2028, 0x2029, 0x202F, 0x205F, 0x3000);
    }

    @ParameterizedTest
    @MethodSource("whiteSpaceCodePoints")
    void trimsEveryUnicodeWhiteSpaceCodePointAtBothEnds(int point) {
        String padded = of(point) + "abc" + of(point);

        assertThat(TextPolicy.normalize(padded)).isEqualTo("abc");
        assertThat(title(padded)).isEmpty();
    }

    @Test
    void trimsWhatStringStripWouldKeepAndKeepsWhatItWouldTrim() {
        assertThat(TextPolicy.normalize(of(0xA0) + "a")).isEqualTo("a");
        assertThat(TextPolicy.normalize(of(0x1C) + "a")).isEqualTo(of(0x1C) + "a");
        assertThat(title(of(0x1C) + "a")).contains(Problem.FORBIDDEN_CHARACTERS);
    }

    @Test
    void rejectsTabAndLineFeedInsideButTrimsThemAtTheEnds() {
        assertThat(title("a\tb")).contains(Problem.FORBIDDEN_CHARACTERS);
        assertThat(title("a\nb")).contains(Problem.FORBIDDEN_CHARACTERS);
        assertThat(title("\ta\n")).isEmpty();
        assertThat(TextPolicy.normalize("\ta\n")).isEqualTo("a");
    }

    @Test
    void storesTheNfcFormKeepingCaseAndInternalSpacing() {
        String stored = TextPolicy.normalize("  Cafe" + of(ACUTE) + "  AU  Lait ");

        assertThat(stored).isEqualTo("Café  AU  Lait");
    }

    @Test
    void acceptsTitleLimitsAndRejectsOneMore() {
        assertThat(title("a".repeat(500))).isEmpty();
        assertThat(title("a".repeat(501))).contains(Problem.TOO_LONG);
    }

    @Test
    void acceptsAuthorLimitsAndRejectsOneMore() {
        assertThat(author("a".repeat(200))).isEmpty();
        assertThat(author("a".repeat(201))).contains(Problem.TOO_LONG);
    }

    @Test
    void countsCodePointsNotUtf16Units() {
        assertThat(title(repeat(0x1F600, 500))).isEmpty();
        assertThat(title(repeat(0x1F600, 501))).contains(Problem.TOO_LONG);
    }

    @Test
    void countsDecomposedEAfterNfc() {
        String decomposed = of('e', ACUTE).repeat(500);

        assertThat(decomposed.codePointCount(0, decomposed.length())).isEqualTo(1000);
        assertThat(title(decomposed)).isEmpty();
    }

    @Test
    void rejectsThreeHundredU1D15EBecauseNfcGivesSixHundred() {
        assertThat(title(repeat(0x1D15E, 300))).contains(Problem.TOO_LONG);
    }

    @Test
    void reportsLengthBeforeCharacters() {
        assertThat(title("a".repeat(500) + of(0)))
                .as("501 code points including NUL").contains(Problem.TOO_LONG);
    }

    @Test
    void reportsASixteenThousandCharacterTitleAsLengthNotAnythingElse() {
        assertThat(title("a".repeat(16000))).contains(Problem.TOO_LONG);
    }

    static Stream<Integer> forbiddenCodePoints() {
        return Stream.of(0x00, 0x1F, 0x7F, 0x9F, 0x200B, 0x202E, 0x00AD, 0x200E, 0x2060, 0x061C, 0xFEFF,
                0x2028, 0x2029, 0xE000, 0xF8FF, 0xF0000, 0x100000, 0x0378, 0x2FFC, 0xE0041, 0xE0000,
                0xE007F, 0xFFFE, 0xFFFF, 0xFDD0, 0xFDEF, 0x1FFFE, 0x1FFFF, 0x10FFFE, 0x10FFFF);
    }

    @ParameterizedTest
    @MethodSource("forbiddenCodePoints")
    void rejectsForbiddenCodePointInsideText(int point) {
        assertThat(title("a" + of(point) + "b")).contains(Problem.FORBIDDEN_CHARACTERS);
    }

    @Test
    void rejectsAnUnpairedSurrogateAsForbiddenCharacters() {
        assertThat(title("a\uD800b")).contains(Problem.FORBIDDEN_CHARACTERS);
    }

    @Test
    void acceptsJoinersVariationSelectorAndFffdBetweenLetters() {
        assertThat(title("a" + of(0xFFFD) + "b")).isEmpty();
        assertThat(title("می‌خواهم")).isEmpty();
        assertThat(title(of(0x2764, 0xFE0F, 0x200D, 0x1F525))).isEmpty();
    }

    @Test
    void acceptsSpaceSeparatorsInside() {
        assertThat(title("a" + of(0xA0, 0x3000, 0x2003) + "b")).isEmpty();
    }

    @Test
    void doesNotReturnTheTextInAnyResult() {
        assertThat(title("secret" + of(0)).orElseThrow().toString()).doesNotContain("secret");
    }
}
