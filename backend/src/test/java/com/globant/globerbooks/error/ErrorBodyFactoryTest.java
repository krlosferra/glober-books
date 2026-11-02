package com.globant.globerbooks.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/** Unit tests of the error catalog, the factory and the writer (spec Appendix A, INV-6). */
class ErrorBodyFactoryTest {

    private static final Instant FIXED = Instant.parse("2026-02-19T10:00:00.123456Z");
    private static final String FIXED_TEXT = "2026-02-19T10:00:00.123Z";

    private final ErrorBodyFactory factory = new ErrorBodyFactory(Clock.fixed(FIXED, ZoneOffset.UTC));
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final ErrorResponseWriter writer = new ErrorResponseWriter(factory);

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(ErrorBody body) {
        return mapper.readValue(mapper.writeValueAsString(body), Map.class);
    }

    @Test
    void catalogHasExactlyTheSixteenCodesOfAppendixA() {
        assertThat(ErrorCode.values()).extracting(Enum::name).containsExactlyInAnyOrder(
                "VALIDATION_ERROR", "INVALID_STATUS", "INVALID_RATING", "RATING_NOT_ALLOWED",
                "BOOK_NOT_FOUND", "DUPLICATE_BOOK", "LIBRARY_FULL", "MALFORMED_REQUEST",
                "INVALID_HOST", "FORBIDDEN", "NOT_FOUND", "METHOD_NOT_ALLOWED",
                "NOT_ACCEPTABLE", "PAYLOAD_TOO_LARGE", "UNSUPPORTED_MEDIA_TYPE", "INTERNAL_ERROR");
    }

    @ParameterizedTest
    @CsvSource({
        "VALIDATION_ERROR,400", "INVALID_STATUS,400", "INVALID_RATING,400", "RATING_NOT_ALLOWED,400",
        "BOOK_NOT_FOUND,404", "DUPLICATE_BOOK,409", "LIBRARY_FULL,409", "MALFORMED_REQUEST,400",
        "INVALID_HOST,400", "FORBIDDEN,403", "NOT_FOUND,404", "METHOD_NOT_ALLOWED,405",
        "NOT_ACCEPTABLE,406", "PAYLOAD_TOO_LARGE,413", "UNSUPPORTED_MEDIA_TYPE,415", "INTERNAL_ERROR,500"})
    void everyCodeHasTheStatusOfAppendixA(ErrorCode code, int status) {
        assertThat(code.status()).isEqualTo(status);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "VALIDATION_ERROR|The request contains invalid values.",
        "INVALID_STATUS|Status must be unread, reading or completed.",
        "INVALID_RATING|Rating must be an integer from 1 to 5.",
        "RATING_NOT_ALLOWED|Only completed books can be rated.",
        "BOOK_NOT_FOUND|Book not found.",
        "DUPLICATE_BOOK|A book with the same title and author already exists.",
        "LIBRARY_FULL|The library is full.",
        "INTERNAL_ERROR|Unexpected error."})
    void codesWithTextInAppendixAUseThatExactText(ErrorCode code, String text) {
        assertThat(code.message()).isEqualTo(text);
    }

    @Test
    void bodyWithoutDetailsHasExactlyErrorCodeAndTimestamp() throws IOException {
        for (ErrorCode code : ErrorCode.values()) {
            ErrorBody body = factory.create(code);

            Map<String, Object> json = toMap(body);

            assertThat(json.keySet()).as(code.name()).containsExactlyInAnyOrder("error", "code", "timestamp");
            assertThat(json).containsEntry("code", code.name()).containsEntry("timestamp", FIXED_TEXT);
            assertThat(json.get("error")).isEqualTo(code.message());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void bodyWithDetailsCarriesFieldAndMessageEntries() throws IOException {
        List<ErrorBody.FieldProblem> problems = List.of(
                new ErrorBody.FieldProblem("title", "is required"),
                new ErrorBody.FieldProblem("author", "must not be empty"));

        ErrorBody body = factory.create(ErrorCode.VALIDATION_ERROR, problems);

        Map<String, Object> json = toMap(body);
        assertThat(json.keySet()).containsExactlyInAnyOrder("error", "code", "timestamp", "details");
        assertThat((List<Object>) json.get("details")).containsExactly(
                Map.of("field", "title", "message", "is required"),
                Map.of("field", "author", "message", "must not be empty"));
    }

    @Test
    void rejectsMissingOrEmptyDetails() {
        assertThatThrownBy(() -> factory.create(ErrorCode.INVALID_RATING, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factory.create(ErrorCode.INVALID_RATING, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void timestampComesFromTheInjectedClockInTheFixedFormat() {
        assertThat(factory.create(ErrorCode.NOT_FOUND).timestamp()).isEqualTo(FIXED_TEXT);
        ErrorBodyFactory zeroMillis = new ErrorBodyFactory(
                Clock.fixed(Instant.parse("2026-02-19T10:00:00Z"), ZoneOffset.ofHours(5)));
        assertThat(zeroMillis.create(ErrorCode.NOT_FOUND).timestamp())
                .matches(Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"))
                .isEqualTo("2026-02-19T10:00:00.000Z");
    }

    @Test
    void noMessageContainsClassNamesSqlOrPaths() {
        Pattern forbidden = Pattern.compile("(?i)\\b(java|org\\.|com\\.|select|insert|sqlite|exception)\\b|[/\\\\]");
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(forbidden.matcher(code.message()).find()).as(code.name()).isFalse();
        }
    }

    @Test
    void noDetailsMemberIsOfTypeObject() {
        for (RecordComponent component : ErrorBody.class.getRecordComponents()) {
            assertThat(component.getType()).as(component.getName()).isNotEqualTo(Object.class);
            assertThat(component.getGenericType().getTypeName()).doesNotContain("Object");
        }
        for (RecordComponent component : ErrorBody.FieldProblem.class.getRecordComponents()) {
            assertThat(component.getType()).isEqualTo(String.class);
        }
    }

    @Test
    void writerSetsStatusExplicitContentTypeAndWritesOnlyTheEnvelope() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.getOutputStream().write("leftover".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        writer.write(response, ErrorCode.PAYLOAD_TOO_LARGE);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
        assertThat(response.getContentAsString()).isEqualTo(
                "{\"error\":\"The request body is too large.\",\"code\":\"PAYLOAD_TOO_LARGE\","
                + "\"timestamp\":\"" + FIXED_TEXT + "\"}");
        assertThat(response.getContentLength()).isEqualTo(response.getContentAsByteArray().length);
    }

    @Test
    void writerWritesDetailsOfAPreparedBodyWithTheStatusOfItsCode() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ErrorBody body = factory.create(ErrorCode.INVALID_STATUS,
                List.of(new ErrorBody.FieldProblem("status", "must be one of: unread, reading, completed")));

        writer.write(response, ErrorCode.INVALID_STATUS, body);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).isEqualTo(
                "{\"error\":\"Status must be unread, reading or completed.\",\"code\":\"INVALID_STATUS\","
                + "\"timestamp\":\"" + FIXED_TEXT + "\",\"details\":[{\"field\":\"status\","
                + "\"message\":\"must be one of: unread, reading, completed\"}]}");
    }
}
