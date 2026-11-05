package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests of the header rules (spec 5.0, Appendix D rows E-07, E-08, E-10 to E-14, E-34). */
class HeaderPolicyTest {

    private static final int PORT = 18080;

    @ParameterizedTest
    @ValueSource(strings = {"localhost:18080", "127.0.0.1:18080", "LOCALHOST:18080", "LocalHost:18080"})
    void allowsLocalHostsWithTheLocalPort(String host) {
        assertThat(HeaderPolicy.hostAllowed(List.of(host), PORT)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"evil.example", "localhost", "localhost:18081", "[::1]:18080",
        "127.0.0.1:18080, evil.example", "localhost.:18080", "user@localhost:18080", "localhost:018080",
        "localhost:18080 ", "http://localhost:18080", ""})
    void rejectsForgedHosts(String host) {
        assertThat(HeaderPolicy.hostAllowed(List.of(host), PORT)).isFalse();
    }

    @Test
    void rejectsAbsentOrRepeatedHost() {
        assertThat(HeaderPolicy.hostAllowed(List.of(), PORT)).isFalse();
        assertThat(HeaderPolicy.hostAllowed(List.of("localhost:18080", "localhost:18080"), PORT)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:18080", "http://127.0.0.1:18080"})
    void allowsOwnOrigins(String origin) {
        assertThat(HeaderPolicy.originAllowed(List.of(origin), PORT)).isTrue();
    }

    @Test
    void allowsAbsentOrigin() {
        assertThat(HeaderPolicy.originAllowed(List.of(), PORT)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://evil.example", "null", "http://localhost:18081", "https://localhost:18080",
        "http://localhost:18080/", ""})
    void rejectsForeignOrigins(String origin) {
        assertThat(HeaderPolicy.originAllowed(List.of(origin), PORT)).isFalse();
    }

    @Test
    void rejectsRepeatedOrigin() {
        assertThat(HeaderPolicy.originAllowed(List.of("http://localhost:18080", "http://localhost:18080"), PORT))
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"same-origin", "none"})
    void allowsSameOriginAndNoneFetchSite(String value) {
        assertThat(HeaderPolicy.fetchSiteAllowed(List.of(value))).isTrue();
    }

    @Test
    void allowsAbsentFetchSite() {
        assertThat(HeaderPolicy.fetchSiteAllowed(List.of())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"cross-site", "same-site", "", "same-origin, cross-site"})
    void rejectsOtherFetchSites(String value) {
        assertThat(HeaderPolicy.fetchSiteAllowed(List.of(value))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/json;charset=utf-8",
        "application/json; charset=UTF-8", "Application/JSON", "application/json ; charset=utf-8",
        "application/json;charset=utf-8 "})
    void allowsTheJsonGrammar(String value) {
        assertThat(HeaderPolicy.contentTypeAllowed(List.of(value))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/plain", "application/jsonx", "application/json; charset=ISO-8859-1",
        "application/json; charset=\"utf-8\"", "application/json; charset=utf-8; foo=bar",
        "application/json; boundary=x", "application/json, text/plain", "application/json; charset = utf-8",
        "application/json;", ""})
    void rejectsOtherContentTypes(String value) {
        assertThat(HeaderPolicy.contentTypeAllowed(List.of(value))).isFalse();
    }

    @Test
    void rejectsAbsentOrRepeatedContentType() {
        assertThat(HeaderPolicy.contentTypeAllowed(List.of())).isFalse();
        assertThat(HeaderPolicy.contentTypeAllowed(List.of("application/json", "application/json"))).isFalse();
    }

    @Test
    void requiresContentTypeForEveryMethodButGetAndHead() {
        assertThat(HeaderPolicy.requiresContentType("GET")).isFalse();
        assertThat(HeaderPolicy.requiresContentType("HEAD")).isFalse();
        for (String method : List.of("POST", "PUT", "PATCH", "DELETE", "OPTIONS")) {
            assertThat(HeaderPolicy.requiresContentType(method)).isTrue();
        }
    }

    @Test
    void securityHeadersMatchTheConstitution() {
        assertThat(HeaderPolicy.SECURITY_HEADERS)
                .containsEntry("X-Content-Type-Options", "nosniff")
                .containsEntry("X-Frame-Options", "DENY")
                .containsEntry("Referrer-Policy", "no-referrer")
                .containsEntry("Cross-Origin-Opener-Policy", "same-origin")
                .containsEntry("Cross-Origin-Resource-Policy", "same-origin")
                .containsEntry("Cache-Control", "no-store")
                .hasSize(8);
        assertThat(HeaderPolicy.SECURITY_HEADERS.get("Content-Security-Policy"))
                .startsWith("default-src 'self'; script-src 'self'")
                .endsWith("require-trusted-types-for 'script'")
                .doesNotContain("upgrade-insecure-requests");
        assertThat(HeaderPolicy.SECURITY_HEADERS.get("Permissions-Policy"))
                .contains("camera=()", "microphone=()", "geolocation=()", "payment=()", "usb=()", "serial=()",
                        "clipboard-read=()", "display-capture=()", "fullscreen=()")
                .doesNotContain("bluetooth");
    }
}
