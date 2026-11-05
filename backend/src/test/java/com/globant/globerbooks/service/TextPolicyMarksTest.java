package com.globant.globerbooks.service;

import static com.globant.globerbooks.support.UnicodeFixtures.ACUTE;
import static com.globant.globerbooks.support.UnicodeFixtures.HANGUL_FILLER;
import static com.globant.globerbooks.support.UnicodeFixtures.VS16;
import static com.globant.globerbooks.support.UnicodeFixtures.ZWJ;
import static com.globant.globerbooks.support.UnicodeFixtures.ZWNJ;
import static com.globant.globerbooks.support.UnicodeFixtures.baseWithAcutes;
import static com.globant.globerbooks.support.UnicodeFixtures.of;
import static com.globant.globerbooks.support.UnicodeFixtures.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.globant.globerbooks.service.TextPolicy.Problem;
import com.globant.globerbooks.support.UnicodeFixtures;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests of BR-006 items 6b to 6d: visible content, mark runs and joiner placement (T-10, T-23 to T-25). */
class TextPolicyMarksTest {

    private static final String PERSIAN = "می‌خواهم";

    @BeforeAll
    static void pinnedUnicodeDataIsPresent() {
        UnicodeFixtures.requirePinnedData();
    }

    private static Optional<Problem> title(String text) {
        return TextPolicy.check(text, TextPolicy.TITLE_MAX);
    }

    static Stream<String> blankOrMarkFirstExamples() {
        return Stream.of(of(HANGUL_FILLER), of(0x2800), of(ACUTE), of(ACUTE) + "abc", of(VS16),
                of(0x115F), of(0x1160), of(0xFFA0), of(0x115F, 0x1160, 0x3164, 0xFFA0, 0x2800),
                of(0x20DD) + "a", of(0x0903) + "a", " " + of(ACUTE) + "a");
    }

    @ParameterizedTest
    @MethodSource("blankOrMarkFirstExamples")
    void rejectsTextThatRendersBlankOrBeginsWithAMark(String text) {
        assertThat(title(text)).contains(Problem.FORBIDDEN_CHARACTERS);
    }

    @Test
    void acceptsABlankFillerBetweenVisibleCharacters() {
        assertThat(title("ab" + of(HANGUL_FILLER) + "cd")).isEmpty();
    }

    @Test
    void acceptsNumbersPunctuationAndSymbolsAsVisibleContent() {
        assertThat(title("1")).isEmpty();
        assertThat(title("!")).isEmpty();
        assertThat(title("$")).isEmpty();
        assertThat(title(of(0x1F600))).isEmpty();
        assertThat(title(of(HANGUL_FILLER) + "x")).isEmpty();
    }

    @Test
    void acceptsARunOfEightAndRejectsARunOfNine() {
        assertThat(title(baseWithAcutes(8))).isEmpty();
        assertThat(title(baseWithAcutes(9))).contains(Problem.FORBIDDEN_CHARACTERS);
    }

    @Test
    void acceptsARunOfEightWithAJoinerInside() {
        assertThat(title("x" + repeat(ACUTE, 7) + of(ZWJ) + "y")).isEmpty();
    }

    @Test
    void rejectsARunOfTenMarksAndAJoiner() {
        assertThat(title(baseWithAcutes(8) + of(ZWJ, ACUTE))).contains(Problem.FORBIDDEN_CHARACTERS);
    }

    @Test
    void acceptsOneVariationSelectorAndRejectsTwoInOneRun() {
        assertThat(title("a" + of(0xE0100))).isEmpty();
        assertThat(title("a" + of(0xE0100, 0xE0101))).contains(Problem.FORBIDDEN_CHARACTERS);
        assertThat(title("a" + of(VS16, ZWJ, VS16))).contains(Problem.FORBIDDEN_CHARACTERS);
        assertThat(title("a" + of(ACUTE, VS16, ACUTE, VS16))).contains(Problem.FORBIDDEN_CHARACTERS);
    }

    @Test
    void acceptsTwoVariationSelectorsInSeparateRuns() {
        assertThat(title("a" + of(VS16) + "b" + of(VS16))).isEmpty();
    }

    @Test
    void acceptsTheHeartFireEmojiSequence() {
        assertThat(title(of(0x2764, VS16, ZWJ, 0x1F525))).isEmpty();
    }

    @Test
    void acceptsAFamilyEmojiJoinedWithJoiners() {
        assertThat(title(of(0x1F468, ZWJ, 0x1F469, ZWJ, 0x1F467, ZWJ, 0x1F466))).isEmpty();
    }

    @Test
    void acceptsThePersianWordWithANonJoiner() {
        assertThat(title(PERSIAN)).isEmpty();
    }

    static Stream<String> misplacedJoinerExamples() {
        return Stream.of(of(ZWNJ) + "ab", "ab" + of(ZWNJ), of(ZWJ) + "ab", "ab" + of(ZWJ),
                "a" + of(ZWNJ, ZWJ) + "b", "a" + of(ZWJ, ZWNJ) + "b", "a" + of(ZWJ, ZWJ) + "b",
                "a" + of(ZWNJ, ZWNJ) + "b", "a" + of(ZWJ, 0x20) + "b", "a" + of(0x20, ZWJ) + "b",
                "a" + of(ZWNJ, 0xA0) + "b", "a" + of(0x3000, ZWNJ) + "b");
    }

    @ParameterizedTest
    @MethodSource("misplacedJoinerExamples")
    void rejectsAMisplacedJoiner(String text) {
        assertThat(title(text)).contains(Problem.FORBIDDEN_CHARACTERS);
    }

    @Test
    void finishesTheFiftyRepetitionInputWellInsideATimeBound() {
        String input = "x" + (repeat(ACUTE, 8) + of(ZWJ)).repeat(50) + "y";
        long start = System.nanoTime();

        Optional<Problem> result = title(input);

        long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();
        assertThat(result).contains(Problem.FORBIDDEN_CHARACTERS);
        assertThat(elapsedMillis).as("elapsed milliseconds").isLessThan(2000L);
        assertThatCode(() -> title(input)).doesNotThrowAnyException();
    }

    @Test
    void finishesALongMarkRunAtTheSixteenKibCapInLinearTime() {
        String input = "x" + repeat(ACUTE, 8000);
        long start = System.nanoTime();

        Optional<Problem> result = TextPolicy.check(input, 10_000);

        long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();
        assertThat(result).contains(Problem.FORBIDDEN_CHARACTERS);
        assertThat(elapsedMillis).isLessThan(2000L);
    }
}
