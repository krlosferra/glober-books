package com.globant.globerbooks.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.dto.CreateBookRequest;
import com.globant.globerbooks.support.UnicodeFixtures;
import jakarta.validation.ConstraintViolation;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Unit tests of the {@link BookText} constraint, the status pattern and the literal interpolator
 * (spec BR-001, BR-006, US-001.4 to .6, T-13, T-34, T-35). Messages are copied from the spec list.
 */
class BookTextTest {

    private static final String TITLE_LENGTH = "must be between 1 and 500 characters";
    private static final String AUTHOR_LENGTH = "must be between 1 and 200 characters";
    private static final String STATUS_MESSAGE = "must be one of: unread, reading, completed";
    private static final String INJECTION = "${7*7}";

    private static LocalValidatorFactoryBean factory;

    @BeforeAll
    static void startFactory() {
        UnicodeFixtures.requirePinnedData();
        factory = ValidatorConfig.defaultValidator();
        factory.afterPropertiesSet();
    }

    private static Set<ConstraintViolation<CreateBookRequest>> validate(String title, String author, String status) {
        return factory.validate(new CreateBookRequest(title, author, status, null));
    }

    private static String messageOf(Set<ConstraintViolation<CreateBookRequest>> violations, String field) {
        List<String> found = violations.stream().filter(each -> each.getPropertyPath().toString().equals(field))
                .map(ConstraintViolation::getMessage).toList();
        assertThat(found).hasSizeLessThanOrEqualTo(1);
        return found.isEmpty() ? null : found.get(0);
    }

    @Test
    void acceptsValidTitleAuthorAndAbsentStatus() {
        assertThat(validate("Clean Code", "Robert Martin", null)).isEmpty();
    }

    @Test
    void acceptsTitleOf500AndAuthorOf200CodePoints() {
        assertThat(validate("📘".repeat(500), "a".repeat(200), "unread")).isEmpty();
    }

    @Test
    void reportsNullAsRequiredAndBlankAsEmpty() {
        assertThat(messageOf(validate(null, "A", null), "title")).isEqualTo("is required");
        assertThat(messageOf(validate("", "A", null), "title")).isEqualTo("must not be empty");
        assertThat(messageOf(validate("A", " \t ", null), "author")).isEqualTo("must not be empty");
    }

    @Test
    void reportsLengthAt501AndAt201() {
        assertThat(messageOf(validate("a".repeat(501), "A", null), "title")).isEqualTo(TITLE_LENGTH);
        assertThat(messageOf(validate("A", "a".repeat(201), null), "author")).isEqualTo(AUTHOR_LENGTH);
    }

    @Test
    void reportsForbiddenCharactersWithTheFixedMessage() {
        String zeroWidthSpace = UnicodeFixtures.of(0x200B);

        assertThat(messageOf(validate("A", "Au" + zeroWidthSpace + "thor", null), "author"))
                .isEqualTo("contains characters that are not allowed");
    }

    @Test
    void firstFailingCheckWinsLengthBeforeCharacters() {
        String tooLongAndForbidden = UnicodeFixtures.repeat(0x200B, 501);

        assertThat(messageOf(validate(tooLongAndForbidden, "A", null), "title")).isEqualTo(TITLE_LENGTH);
    }

    @Test
    void messageNeverEchoesExpressionTextOrItsResult() {
        String hostile = INJECTION + "a".repeat(495);
        assertThat(hostile.length()).isEqualTo(501);

        Set<ConstraintViolation<CreateBookRequest>> violations = validate(hostile, "A", null);

        assertThat(violations).hasSize(1);
        String message = violations.iterator().next().getMessage();
        assertThat(message).isEqualTo(TITLE_LENGTH).doesNotContain(INJECTION).doesNotContain("49");
    }

    @Test
    void literalInterpolatorEvaluatesNothing() {
        ValidatorConfig.LiteralMessageInterpolator interpolator = new ValidatorConfig.LiteralMessageInterpolator();

        assertThat(interpolator.interpolate("{a} ${7*7} {min}", null)).isEqualTo("{a} ${7*7} {min}");
        assertThat(interpolator.interpolate("${7*7}", null, Locale.ROOT)).isEqualTo("${7*7}");
    }

    @Test
    void acceptsThePersianWordBuiltFromCodePoints() {
        String persian = UnicodeFixtures.of(
                0x0645, 0x06CC, UnicodeFixtures.ZWNJ, 0x062E, 0x0648, 0x0627, 0x0647, 0x0645);
        assertThat(persian.codePointAt(2)).isEqualTo(0x200C);

        assertThat(validate(persian, persian, null)).isEmpty();
    }

    @Test
    void itemSixIsJudgedAfterNfcSoNineMarksThatComposeToEightAreAccepted() {
        String nineMarks = UnicodeFixtures.baseWithAcutes(9).replace("x", "a");

        assertThat(validate(nineMarks, "A", null)).isEmpty();
        assertThat(messageOf(validate(UnicodeFixtures.baseWithAcutes(10).replace("x", "a"), "A", null), "title"))
                .isEqualTo("contains characters that are not allowed");
    }

    @Test
    void statusMustBeOneOfTheThreeExactValues() {
        for (String valid : new String[] {"unread", "reading", "completed"}) {
            assertThat(validate("A", "B", valid)).isEmpty();
        }
        for (String invalid : new String[] {"Completed", "reading ", "", "done", "unread\n"}) {
            assertThat(messageOf(validate("A", "B", invalid), "status")).isEqualTo(STATUS_MESSAGE);
        }
    }

    @Test
    void reportsEachFailingFieldOnce() {
        Set<ConstraintViolation<CreateBookRequest>> violations = validate("", null, "Done");

        assertThat(violations).extracting(each -> each.getPropertyPath().toString())
                .containsExactlyInAnyOrder("title", "author", "status");
    }
}
