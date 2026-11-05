package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Cases come from spec 5.0 routing: what is not routable, and that nothing is normalised. */
class RequestTargetTest {

    @ParameterizedTest
    @ValueSource(strings = {"/", "/index.html", "/api/v1/books", "/api/v1/books/", "/api/v1/%62ooks",
        "/api/v1/books/%2e%2e%2e", "//", "/a//b"})
    void routesPathsWithNoForbiddenForm(String raw) {
        assertThat(RequestTarget.of(raw).routable()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "api", "/a;b", "/a;", "/a\\b", "/a%2fb", "/a%2Fb", "/a%5cb", "/a%5Cb",
        "/a%3bb", "/a%3Bb", "/a/./b", "/a/../b", "/a/%2e%2e", "/a/%2E%2E", "/a/%2e./b", "/a/.%2E",
        "/a/%2e", "/.", "/.."})
    void refusesPathsThatSpecMarksNotRoutable(String raw) {
        assertThat(RequestTarget.of(raw).routable()).isFalse();
    }

    @Test
    void treatsThreeEncodedDotsAsAnOrdinarySegment() {
        assertThat(RequestTarget.of("/api/v1/books/%2e%2e%2e").routable()).isTrue();
    }

    @Test
    void keepsEverythingAsReceived() {
        RequestTarget target = RequestTarget.of("//a/B/");

        assertThat(target.path()).isEqualTo("//a/B/");
        assertThat(target.segments()).containsExactly("", "a", "B", "");
    }

    @Test
    void treatsNullAsEmptyPath() {
        assertThat(RequestTarget.of(null).path()).isEmpty();
        assertThat(RequestTarget.of(null).segments()).isEmpty();
    }
}
