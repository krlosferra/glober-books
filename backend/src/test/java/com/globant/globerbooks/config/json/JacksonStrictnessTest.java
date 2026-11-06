package com.globant.globerbooks.config.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Proves every Jackson 3 switch of {@link JacksonConfig} (SC-31, SC-08, spec 5.0). Each test is a
 * pair: the configured mapper refuses the input, and the same mapper with only that one switch
 * flipped accepts it, so the refusal is caused by the switch and not by something else. Inputs
 * and the expected refusal come from spec 5.0 and Appendix D (T-04, T-05, T-16, T-21, T-27 to T-31).
 */
class JacksonStrictnessTest {

    private static final JsonMapper STRICT = build(factory -> { }, mapper -> { });

    /** A text member. */
    record Text(String title) { }

    /** A nested object member. */
    record Nest(Text inner) { }

    /** A boolean member. */
    record Flag(Boolean flag) { }

    /** An integer member. */
    record Whole(Integer whole) { }

    /** A primitive member. */
    record Prim(int count) { }

    /** A decimal member. */
    record Real(Double number) { }

    /** An enum member. */
    record Hue(Color color) { }

    /** A list member. */
    record Tags(List<String> tags) { }

    /** Enum used to prove the numeric-enum switch. */
    enum Color { RED, GREEN }

    private static JsonMapper build(Consumer<JsonFactoryBuilder> factoryTweak,
            Consumer<JsonMapper.Builder> mapperTweak) {
        JsonFactoryBuilder factory = JsonFactory.builder();
        JacksonConfig.strictFactory(factory);
        factoryTweak.accept(factory);
        JsonMapper.Builder mapper = JsonMapper.builder(factory.build());
        JacksonConfig.strictMapper(mapper);
        mapperTweak.accept(mapper);
        return mapper.build();
    }

    private static JsonMapper withFactory(Consumer<JsonFactoryBuilder> tweak) {
        return build(tweak, mapper -> { });
    }

    private static JsonMapper withMapper(Consumer<JsonMapper.Builder> tweak) {
        return build(factory -> { }, tweak);
    }

    private static void assertRefused(JsonMapper mapper, String json, Class<?> type) {
        assertThatThrownBy(() -> mapper.readValue(json, type)).isInstanceOf(JacksonException.class);
    }

    private static void assertAccepted(JsonMapper mapper, String json, Class<?> type) {
        assertThat(mapper.readValue(json, type)).isNotNull();
    }

    private static void pair(JsonMapper relaxed, String json, Class<?> type) {
        assertRefused(STRICT, json, type);
        assertAccepted(relaxed, json, type);
    }

    @Test
    void refusesJavaCommentsOnlyBecauseTheSwitchIsOff() {
        pair(withFactory(cfg -> cfg.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)), "/*x*/{\"title\":\"a\"}", Text.class);
    }

    @Test
    void refusesYamlCommentsOnlyBecauseTheSwitchIsOff() {
        pair(withFactory(cfg -> cfg.enable(JsonReadFeature.ALLOW_YAML_COMMENTS)), "#x\n{\"title\":\"a\"}", Text.class);
    }

    @Test
    void refusesSingleQuotesOnlyBecauseTheSwitchIsOff() {
        pair(withFactory(cfg -> cfg.enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)), "{'title':'a'}", Text.class);
    }

    @Test
    void refusesUnquotedNamesOnlyBecauseTheSwitchIsOff() {
        pair(withFactory(cfg -> cfg.enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)),
                "{title:\"a\"}", Text.class);
    }

    @Test
    void refusesRawControlCharacterInStringOnlyBecauseTheSwitchIsOff() {
        pair(withFactory(cfg -> cfg.enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)),
                "{\"title\":\"a\nb\"}", Text.class);
    }

    @Test
    void refusesBackslashBeforeAnyCharacterOnlyBecauseTheSwitchIsOff() {
        pair(withFactory(cfg -> cfg.enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)),
                "{\"title\":\"a\\qb\"}", Text.class);
    }

    @Test
    void refusesRecordSeparatorOnlyBecauseTheSwitchIsOff() {
        pair(withFactory(cfg -> cfg.enable(JsonReadFeature.ALLOW_RS_CONTROL_CHAR)),
                "\u001e{\"title\":\"a\"}", Text.class);
    }

    @Test
    void refusesTrailingCommaOnlyBecauseTheSwitchIsOff() {
        pair(withFactory(cfg -> cfg.enable(JsonReadFeature.ALLOW_TRAILING_COMMA)), "{\"title\":\"a\",}", Text.class);
    }

    @Test
    void refusesMissingValueOnlyBecauseTheSwitchIsOff() {
        JsonMapper relaxed = withFactory(cfg -> cfg.enable(JsonReadFeature.ALLOW_MISSING_VALUES));
        assertRefused(STRICT, "{\"tags\":[\"a\",,\"b\"]}", Tags.class);
        assertAccepted(relaxed, "{\"tags\":[\"a\",,\"b\"]}", Tags.class);
    }

    @Test
    void refusesNonNumericNumbersOnlyBecauseTheSwitchIsOff() {
        pair(withFactory(cfg -> cfg.enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)), "{\"number\":NaN}", Real.class);
    }

    @Test
    void refusesLeadingZerosOnlyBecauseTheSwitchIsOff() {
        pair(withFactory(cfg -> cfg.enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)),
                "{\"whole\":01}", Whole.class);
    }

    @Test
    void refusesLeadingPlusOnlyBecauseTheSwitchIsOff() {
        pair(withFactory(cfg -> cfg.enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)),
                "{\"whole\":+5}", Whole.class);
    }

    @Test
    void refusesLeadingDecimalPointOnlyBecauseTheSwitchIsOff() {
        pair(withFactory(cfg -> cfg.enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)),
                "{\"number\":.5}", Real.class);
    }

    @Test
    void refusesTrailingDecimalPointOnlyBecauseTheSwitchIsOff() {
        pair(withFactory(cfg -> cfg.enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)),
                "{\"number\":5.}", Real.class);
    }

    @Test
    void refusesDuplicateMemberAndDuplicateAfterUnescaping() {
        JsonMapper relaxed = withFactory(cfg -> cfg.disable(StreamReadFeature.STRICT_DUPLICATE_DETECTION));
        String plain = "{\"title\":\"a\",\"title\":\"b\"}";
        String escaped = "{\"title\":\"a\",\"\\u0074itle\":\"b\"}";
        pair(relaxed, plain, Text.class);
        pair(relaxed, escaped, Text.class);
        assertAccepted(JsonMapper.builder().build(), plain, Text.class); // control: the library default allows it
    }

    @Test
    void refusesUnknownMemberOnlyBecauseTheSwitchIsOff() {
        pair(withMapper(cfg -> cfg.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)),
                "{\"title\":\"a\",\"id\":\"x\"}", Text.class);
    }

    @Test
    void refusesMemberNameWithDifferentCaseAsUnknown() {
        assertRefused(STRICT, "{\"Title\":\"a\"}", Text.class);
    }

    @Test
    void refusesTrailingContentOnlyBecauseTheSwitchIsOff() {
        pair(withMapper(cfg -> cfg.disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)),
                "{\"title\":\"a\"}x", Text.class);
        assertRefused(STRICT, "{\"title\":\"a\"}{\"title\":\"b\"}", Text.class);
    }

    @Test
    void refusesScalarCoercionOnlyBecauseTheSwitchIsOff() {
        JsonMapper relaxed = withMapper(cfg -> cfg.enable(MapperFeature.ALLOW_COERCION_OF_SCALARS));
        pair(relaxed, "{\"whole\":\"5\"}", Whole.class);
    }

    @Test
    void refusesNumberAsBooleanAndBooleanTextAsWrongType() {
        assertRefused(STRICT, "{\"flag\":1}", Flag.class);
        assertRefused(STRICT, "{\"flag\":0}", Flag.class);
        assertRefused(STRICT, "{\"flag\":\"false\"}", Flag.class);
    }

    @Test
    void refusesNullBooleanAndKeepsAbsentBooleanNull() {
        // the control is a plain mapper: the library default reads null as a null Boolean
        pair(JsonMapper.builder().build(), "{\"flag\":null}", Flag.class);
        assertThat(STRICT.readValue("{}", Flag.class).flag()).isNull();
    }

    @Test
    void refusesFloatAsIntegerOnlyBecauseTheSwitchIsOff() {
        pair(withMapper(cfg -> cfg.enable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)), "{\"whole\":5.0}", Whole.class);
    }

    @Test
    void refusesSingleValueAsArrayOnlyBecauseTheSwitchIsOff() {
        pair(withMapper(cfg -> cfg.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)),
                "{\"tags\":\"a\"}", Tags.class);
    }

    @Test
    void refusesWrappedSingleValueOnlyBecauseTheSwitchIsOff() {
        pair(withMapper(cfg -> cfg.enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)),
                "{\"whole\":[5]}", Whole.class);
    }

    @Test
    void refusesEmptyStringAndEmptyArrayAsEmptyObject() {
        pair(withMapper(cfg -> cfg.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)),
                "{\"inner\":\"\"}", Nest.class);
        pair(withMapper(cfg -> cfg.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)),
                "{\"inner\":[]}", Nest.class);
        assertRefused(STRICT, "[]", Text.class);
        assertRefused(STRICT, "\"\"", Text.class);
    }

    @Test
    void refusesNullForPrimitiveOnlyBecauseTheSwitchIsOn() {
        pair(withMapper(cfg -> cfg.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)),
                "{\"count\":null}", Prim.class);
    }

    @Test
    void refusesNumericEnumOnlyBecauseTheSwitchIsOn() {
        pair(withMapper(cfg -> cfg.disable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)), "{\"color\":1}", Hue.class);
        assertThat(STRICT.readValue("{\"color\":\"GREEN\"}", Hue.class).color()).isEqualTo(Color.GREEN);
    }

    @Test
    void refusesNumberAndBooleanAndContainerWhereTextIsExpected() {
        for (String json : List.of("{\"title\":5}", "{\"title\":true}", "{\"title\":[\"a\"]}", "{\"title\":{}}",
                "{\"title\":1.5}")) {
            assertRefused(STRICT, json, Text.class);
        }
        assertThat(STRICT.readValue("{\"title\":null}", Text.class).title()).isNull();
    }
}
