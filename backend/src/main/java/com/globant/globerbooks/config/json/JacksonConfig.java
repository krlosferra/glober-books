package com.globant.globerbooks.config.json;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import org.springframework.boot.jackson.autoconfigure.JsonFactoryBuilderCustomizer;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Strict reading of request bodies (spec 5.0, BR-012 step 5, SC-31). Every switch is set
 * explicitly, so a change of a library default cannot loosen the parser; each one has a test in
 * {@code JacksonStrictnessTest}. The same static methods build the mapper in the tests.
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    /** Deepest nesting of arrays and objects. */
    public static final int MAX_DEPTH = 16;
    /** Longest string value in characters (the body cap, so a long string is a length error). */
    public static final int MAX_STRING = 16_384;
    /** Longest member name in characters. */
    public static final int MAX_NAME = 64;
    /** Longest number literal in characters. */
    public static final int MAX_NUMBER = 100;
    /** Longest document in characters (the body cap). */
    public static final long MAX_DOCUMENT = 16_384;
    /** Most tokens in a document; the largest legal body has about a dozen. */
    public static final long MAX_TOKENS = 64;

    /**
     * Customizer of the token stream factory: read constraints, duplicate names, and every
     * non-standard JSON syntax switched off.
     *
     * @return the customizer
     */
    @Bean
    public JsonFactoryBuilderCustomizer strictFactoryCustomizer() {
        return JacksonConfig::strictFactory;
    }

    /**
     * Customizer of the mapper: unknown members, trailing tokens, scalar coercion, numeric enums,
     * float-to-integer and null booleans, plus the strict string deserializer.
     *
     * @return the customizer
     */
    @Bean
    public JsonMapperBuilderCustomizer strictMapperCustomizer() {
        return JacksonConfig::strictMapper;
    }

    /**
     * Applies the strict token-stream settings to a factory builder.
     *
     * @param builder the builder to change
     */
    public static void strictFactory(JsonFactoryBuilder builder) {
        builder.streamReadConstraints(StreamReadConstraints.builder()
                .maxNestingDepth(MAX_DEPTH).maxStringLength(MAX_STRING).maxNameLength(MAX_NAME)
                .maxNumberLength(MAX_NUMBER).maxDocumentLength(MAX_DOCUMENT).maxTokenCount(MAX_TOKENS).build());
        builder.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION);
        builder.disable(JsonReadFeature.ALLOW_JAVA_COMMENTS, JsonReadFeature.ALLOW_YAML_COMMENTS,
                JsonReadFeature.ALLOW_SINGLE_QUOTES, JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES,
                JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER,
                JsonReadFeature.ALLOW_RS_CONTROL_CHAR, JsonReadFeature.ALLOW_TRAILING_COMMA,
                JsonReadFeature.ALLOW_MISSING_VALUES, JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS,
                JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS, JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS,
                JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS,
                JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS);
    }

    /**
     * Applies the strict databind settings to a mapper builder.
     *
     * @param builder the builder to change
     */
    public static void strictMapper(JsonMapper.Builder builder) {
        builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        builder.enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
        builder.disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
        builder.disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT,
                DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS,
                DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        builder.withConfigOverride(Boolean.class,
                override -> override.setNullHandling(JsonSetter.Value.forValueNulls(Nulls.FAIL)));
        builder.addModule(new SimpleModule("strict-string")
                .addDeserializer(String.class, new StrictStringDeserializer()));
    }
}
