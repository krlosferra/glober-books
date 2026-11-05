package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Unit tests of the capped, deadline-bound body read (SC-06, SEC-F-073). */
class BoundedBodyRequestTest {

    private static MockHttpServletRequest body(int size) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/books");
        request.setContent(new byte[size]);
        return request;
    }

    @Test
    void readsABodyOfExactlyTheCap() throws IOException {
        BoundedBodyRequest read = BoundedBodyRequest.read(body(BoundedBodyRequest.LIMIT));

        assertThat(read.getInputStream().readAllBytes()).hasSize(BoundedBodyRequest.LIMIT);
    }

    @Test
    void rejectsABodyOneByteOverTheCap() {
        assertThatThrownBy(() -> BoundedBodyRequest.read(body(BoundedBodyRequest.LIMIT + 1)))
                .isInstanceOf(BoundedBodyRequest.TooLargeException.class);
    }

    @Test
    void readsAnEmptyBody() throws IOException {
        assertThat(BoundedBodyRequest.read(body(0)).getContentLengthLong()).isZero();
    }

    @Test
    void failsWhenTheTotalDeadlineHasPassed() {
        assertThatThrownBy(() -> BoundedBodyRequest.read(body(10), Duration.ZERO))
                .isInstanceOf(BoundedBodyRequest.TooSlowException.class);
    }

    @Test
    void theDefaultDeadlineIsTenSeconds() {
        assertThat(BoundedBodyRequest.DEADLINE).isEqualTo(Duration.ofSeconds(10));
    }
}
