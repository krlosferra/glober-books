package com.globant.globerbooks.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globant.globerbooks.config.json.JacksonConfig;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests of the rating literal rules on a bare strict mapper (BR-002; Appendix D T-17, T-18,
 * T-31, T-32, T-37). The same table is proved through the real resolver in
 * {@code RatingBodyFullContextTest}. Expected outcomes are copied from the spec rows.
 */
class RatingValueDeserializerTest {

    private static final JsonMapper MAPPER = strictMapper();
    private static final String HUNDRED = "9".repeat(100);

    private static JsonMapper strictMapper() {
        JsonFactoryBuilder factory = JsonFactory.builder();
        JacksonConfig.strictFactory(factory);
        JsonMapper.Builder builder = JsonMapper.builder(factory.build());
        JacksonConfig.strictMapper(builder);
        return builder.build();
    }

    /**
     * Rating literals that give INVALID_RATING.
     *
     * @return the literals as written
     */
    static List<String> invalidLiterals() {
        return List.of("0", "6", "-1", "-3", "-0", "4.9", "5.0", "1e0", "1E1", "null", HUNDRED,
                "-" + "9".repeat(99), "0." + "0".repeat(97) + "1", "1e999999999", "1e-999999999",
                "0.0000000000000000000000000000000000000001", "10", "55", "1.0E0");
    }

    /**
     * Rating literals that give MALFORMED_REQUEST.
     *
     * @return the literals as written
     */
    static List<String> malformedLiterals() {
        return List.of("\"5\"", "\"\"", "true", "false", "[5]", "[]", "{}", "{\"a\":1}", "01", "+5", "NaN",
                "Infinity", "-Infinity", "5.", ".5", "0x5", "5e", "9".repeat(101), "-" + "9".repeat(101),
                "-" + "9".repeat(100), "0." + "0".repeat(99) + "1", "-0." + "0".repeat(97) + "1");
    }

    static Stream<Arguments> validLiterals() {
        return Stream.of(Arguments.of("1", 1), Arguments.of("2", 2), Arguments.of("3", 3),
                Arguments.of("4", 4), Arguments.of("5", 5));
    }

    @ParameterizedTest
    @MethodSource("validLiterals")
    void readsEachIntegerOneToFive(String literal, int expected) {
        RatingRequest request = MAPPER.readValue("{\"rating\":" + literal + "}", RatingRequest.class);

        assertThat(request.rating().isValid()).isTrue();
        assertThat(request.rating().rating()).contains(expected);
    }

    @ParameterizedTest
    @MethodSource("invalidLiterals")
    void marksAnyOtherNumberAndNullAsInvalid(String literal) {
        RatingRequest request = MAPPER.readValue("{\"rating\":" + literal + "}", RatingRequest.class);

        assertThat(request.rating()).isEqualTo(RatingValue.INVALID);
        assertThat(request.rating().isValid()).isFalse();
        assertThat(request.rating().rating()).isEmpty();
    }

    @Test
    void anEmptyObjectBodyIsTheInvalidMarker() {
        RatingRequest request = MAPPER.readValue("{}", RatingRequest.class);

        assertThat(request.rating()).isEqualTo(RatingValue.INVALID);
    }

    @ParameterizedTest
    @MethodSource("malformedLiterals")
    void refusesEveryOtherTokenAndEveryBrokenNumber(String literal) {
        assertThatThrownBy(() -> MAPPER.readValue("{\"rating\":" + literal + "}", RatingRequest.class))
                .isInstanceOf(JacksonException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"rating\":4,\"rating\":5}", "{\"rating\":5,\"status\":\"completed\"}",
        "{\"status\":\"completed\",\"rating\":5}", "{\"rating\":5} x", "[5]", "5"})
    void refusesDuplicateAndUnknownMembersAndWrongDocuments(String body) {
        assertThatThrownBy(() -> MAPPER.readValue(body, RatingRequest.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void readsAHugeExponentWithoutExpandingItWithin300Milliseconds() {
        long started = System.nanoTime();
        RatingRequest request = MAPPER.readValue("{\"rating\":1e999999999}", RatingRequest.class);
        long millis = (System.nanoTime() - started) / 1_000_000;

        assertThat(request.rating()).isEqualTo(RatingValue.INVALID);
        assertThat(millis).isLessThan(300);
    }

    @Test
    void invalidMarkerHasNoNumber() {
        assertThat(new RatingRequest(null).rating()).isSameAs(RatingValue.INVALID);
    }
}
