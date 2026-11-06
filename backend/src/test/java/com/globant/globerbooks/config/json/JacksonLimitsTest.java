package com.globant.globerbooks.config.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Proves the read constraints and the string rules of {@link JacksonConfig} (T-04, T-11, T-27, T-31).
 * Each limit is a pair: the configured mapper refuses the input, the same mapper with only that
 * limit raised accepts it.
 */
class JacksonLimitsTest {

    private static final JsonMapper STRICT = build(factory -> { });

    /** A text member. */
    record Text(String title) { }

    /** A decimal member. */
    record Real(Double number) { }

    /** A list member. */
    record Tags(List<String> tags) { }

    private static JsonMapper build(Consumer<JsonFactoryBuilder> tweak) {
        JsonFactoryBuilder factory = JsonFactory.builder();
        JacksonConfig.strictFactory(factory);
        tweak.accept(factory);
        JsonMapper.Builder mapper = JsonMapper.builder(factory.build());
        JacksonConfig.strictMapper(mapper);
        return mapper.build();
    }

    private static void assertRefused(JsonMapper mapper, String json, Class<?> type) {
        assertThatThrownBy(() -> mapper.readValue(json, type)).isInstanceOf(JacksonException.class);
    }

    private static void assertAccepted(JsonMapper mapper, String json, Class<?> type) {
        assertThat(mapper.readValue(json, type)).isNotNull();
    }

    @Test
    void refusesUnpairedSurrogatesAndKeepsValidPairs() {
        for (String escape : List.of("\\uD800", "\\uDC00", "\\uDE00\\uD83D", "\\uD83Dx", "x\\uD83D")) {
            assertRefused(STRICT, "{\"title\":\"" + escape + "\"}", Text.class);
        }
        assertThat(STRICT.readValue("{\"title\":\"\\uD83D\\uDE00\"}", Text.class).title())
                .isEqualTo(new String(Character.toChars(0x1F600)));
        assertThat(StrictStringDeserializer.hasUnpairedSurrogate("a\uD83D\uDE00b")).isFalse();
        assertThat(StrictStringDeserializer.hasUnpairedSurrogate("a\uDE00")).isTrue();
    }

    @Test
    void limitsNestingDepthOnlyBecauseOfTheConstraint() {
        String deep = "{\"x\":".repeat(JacksonConfig.MAX_DEPTH + 1) + "1" + "}".repeat(JacksonConfig.MAX_DEPTH + 1);
        JsonMapper relaxed = build(cfg -> cfg.streamReadConstraints(
                StreamReadConstraints.builder().maxNestingDepth(1000).build()));
        assertRefused(STRICT, deep, Object.class);
        assertAccepted(relaxed, deep, Object.class);
    }

    @Test
    void limitsStringLengthOnlyBecauseOfTheConstraint() {
        String json = "{\"title\":\"" + "a".repeat(JacksonConfig.MAX_STRING + 1) + "\"}";
        JsonMapper relaxed = build(cfg -> cfg.streamReadConstraints(StreamReadConstraints.builder()
                .maxStringLength(100_000).maxDocumentLength(100_000).build()));
        assertRefused(STRICT, json, Text.class);
        assertAccepted(relaxed, json, Text.class);
        String atLimit = "{\"title\":\"" + "a".repeat(JacksonConfig.MAX_STRING - 12) + "\"}";
        assertThat(STRICT.readValue(atLimit, Text.class).title()).hasSize(JacksonConfig.MAX_STRING - 12);
    }

    @Test
    void limitsNameLengthOnlyBecauseOfTheConstraint() {
        String json = "{\"" + "n".repeat(JacksonConfig.MAX_NAME + 1) + "\":1}";
        JsonMapper relaxed = build(cfg -> cfg.streamReadConstraints(
                StreamReadConstraints.builder().maxNameLength(1000).build()));
        assertRefused(STRICT, json, Object.class);
        assertAccepted(relaxed, json, Object.class);
    }

    @Test
    void limitsNumberLengthOnlyBecauseOfTheConstraint() {
        String json = "{\"number\":1" + "0".repeat(JacksonConfig.MAX_NUMBER) + "}";
        JsonMapper relaxed = build(cfg -> cfg.streamReadConstraints(
                StreamReadConstraints.builder().maxNumberLength(1000).build()));
        assertRefused(STRICT, json, Real.class);
        assertAccepted(relaxed, json, Real.class);
    }

    @Test
    void limitsDocumentLengthOnlyBecauseOfTheConstraint() {
        String json = "{\"title\":\"a\"}" + " ".repeat((int) JacksonConfig.MAX_DOCUMENT);
        JsonMapper relaxed = build(cfg -> cfg.streamReadConstraints(
                StreamReadConstraints.builder().maxDocumentLength(1_000_000).build()));
        assertRefused(STRICT, json, Text.class);
        assertAccepted(relaxed, json, Text.class);
    }

    @Test
    void limitsTokenCountOnlyBecauseOfTheConstraint() {
        String json = "{\"tags\":[" + "\"a\",".repeat((int) JacksonConfig.MAX_TOKENS) + "\"a\"]}";
        JsonMapper relaxed = build(cfg -> cfg.streamReadConstraints(
                StreamReadConstraints.builder().maxTokenCount(100_000).build()));
        assertRefused(STRICT, json, Tags.class);
        assertAccepted(relaxed, json, Tags.class);
    }

    @Test
    void failsFastOnHugeExponentsWithoutConversion() {
        long started = System.nanoTime();
        assertRefused(STRICT, "{\"title\":1e999999999}", Text.class);
        assertRefused(STRICT, "{\"title\":1e-999999999}", Text.class);
        assertThat((System.nanoTime() - started) / 1_000_000).isLessThan(300);
    }
}
