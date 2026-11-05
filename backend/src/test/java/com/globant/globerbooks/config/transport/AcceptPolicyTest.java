package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Cases come from the Accept rule of spec 5.0 and E-06. */
class AcceptPolicyTest {

    private static boolean ok(String... lines) {
        return AcceptPolicy.acceptable(List.of(lines));
    }

    @Test
    void acceptsAbsentHeader() {
        assertThat(AcceptPolicy.acceptable(List.of())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", " \t "})
    void acceptsEmptyOrBlankHeader(String value) {
        assertThat(ok(value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"*/*", "application/json", "application/*", "APPLICATION/JSON", "Application/*",
        "text/xml, application/json", "text/xml,*/*", "application/json;charset=utf-8", "application/json ; q=1",
        "application/json;q=0.5", "application/json;q=1.000", "application/json;q=1.",
        "application/json;Q=0.001", "application/json;level=1;q=1", "application/json;other", "a/b;x=y, */*",
        "text/xml,,application/json", "application/json,", ",application/json", "*/*;q=0.1",
        "application/json;q=0, */*", "text/xml;q=0, application/json"})
    void acceptsRangesThatAllowJson(String value) {
        assertThat(ok(value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/xml", "text/*", "application/xml", "image/png", "application/jsonx",
        "application/json;q=0", "*/*;q=0", "application/*;q=0.0", "application/json;q=0.000",
        "application/json;q=0.", "*/*;q=0, text/xml",
        "application/json;q=0, application/*;q=0"})
    void refusesWhenNoRemainingRangeAllowsJson(String value) {
        assertThat(ok(value)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "application", "*/json", "application/", "/json", "a/b/c", "application/json;q",
        "application/json;q=", "application/json;q=2", "application/json;q=1.5", "application/json;q=0.1234",
        "application/json;q=-1", "application/json;q=.5", "application/json;q=abc", "application/json;q=1.0001",
        "application /json", "application/json, json", "application/json, */json", "*/*;q", "text/xml, ?"})
    void refusesAnUnparseableHeaderEvenWhenAnotherRangeMatches(String value) {
        assertThat(ok(value)).isFalse();
    }

    @Test
    void joinsSeveralHeaderLinesIntoOneList() {
        assertThat(ok("text/xml", "application/json")).isTrue();
        assertThat(ok("text/xml", "text/html")).isFalse();
        assertThat(ok("application/json", "bad")).isFalse();
    }
}
