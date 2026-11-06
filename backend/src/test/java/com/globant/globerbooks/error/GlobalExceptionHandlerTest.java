package com.globant.globerbooks.error;

import com.globant.globerbooks.config.transport.RouteTable;
import com.globant.globerbooks.config.transport.StaticAssetCatalog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Unit tests of the exception advice (tasks.md Task 06 criteria 4 and 5, SC-11, SC-29). */
class GlobalExceptionHandlerTest {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";

    private final ErrorBodyFactory factory = new ErrorBodyFactory(
            Clock.fixed(Instant.parse("2026-02-19T10:00:00.123Z"), ZoneOffset.UTC));
    private final RouteTable routes = new RouteTable(new StaticAssetCatalog("classpath:/static/"));
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new Probe())
            .setControllerAdvice(new GlobalExceptionHandler(factory, routes))
            .build();

    /** Controller that fails on demand. */
    @RestController
    @RequestMapping("/probe")
    static class Probe {
        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("secret user text /etc/passwd");
        }

        @PostMapping(value = "/typed", consumes = MediaType.APPLICATION_JSON_VALUE)
        String typed() {
            return "x";
        }

        @GetMapping(value = "/accepts", produces = MediaType.APPLICATION_JSON_VALUE)
        String accepts() {
            return "x";
        }
    }

    /** Controller that validates the real request record, as the book endpoint does. */
    @RestController
    @RequestMapping("/probe/books")
    static class Validating {
        @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
        String add(@jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
                com.globant.globerbooks.dto.CreateBookRequest request) {
            return "ok";
        }
    }

    private org.springframework.test.web.servlet.ResultActions postBook(String body) throws Exception {
        org.springframework.validation.beanvalidation.LocalValidatorFactoryBean validator =
                com.globant.globerbooks.config.ValidatorConfig.defaultValidator();
        validator.afterPropertiesSet();
        MockMvc books = MockMvcBuilders.standaloneSetup(new Validating()).setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler(factory, routes)).build();
        return books.perform(post("/probe/books").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void emptyObjectGivesValidationErrorWithTitleThenAuthor() throws Exception {
        postBook("{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.length()").value(2))
                .andExpect(jsonPath("$.details[0].field").value("title"))
                .andExpect(jsonPath("$.details[0].message").value("is required"))
                .andExpect(jsonPath("$.details[1].field").value("author"))
                .andExpect(jsonPath("$.details[1].message").value("is required"));
    }

    @Test
    void onlyStatusFailingGivesInvalidStatus() throws Exception {
        postBook("{\"title\":\"a\",\"author\":\"b\",\"status\":\"Done\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS"))
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[0].field").value("status"))
                .andExpect(jsonPath("$.details[0].message").value("must be one of: unread, reading, completed"));
    }

    @Test
    void statusWithAnotherFailingFieldGivesValidationErrorOrderedTitleThenStatus() throws Exception {
        postBook("{\"status\":\"Done\",\"author\":\"a\",\"title\":\"\"}")
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.length()").value(2))
                .andExpect(jsonPath("$.details[0].field").value("title"))
                .andExpect(jsonPath("$.details[0].message").value("must not be empty"))
                .andExpect(jsonPath("$.details[1].field").value("status"));
    }

    @Test
    void titleAuthorAndStatusFailuresAreOrderedTitleAuthorStatus() throws Exception {
        postBook("{\"status\":\"Done\",\"author\":\"\",\"title\":null}")
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("title"))
                .andExpect(jsonPath("$.details[1].field").value("author"))
                .andExpect(jsonPath("$.details[2].field").value("status"));
    }

    @Test
    void explicitNullStatusGivesInvalidStatusAndAbsentStatusIsAccepted() throws Exception {
        postBook("{\"title\":\"a\",\"author\":\"b\",\"status\":null}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS"))
                .andExpect(jsonPath("$.details[0].field").value("status"));
        postBook("{\"title\":\"a\",\"author\":\"b\"}").andExpect(status().isOk());
    }

    @Test
    void hostileTitleOf501CharactersIsRejectedWithoutEchoingIt() throws Exception {
        String hostile = "${7*7}" + "a".repeat(495);

        postBook("{\"title\":\"" + hostile + "\",\"author\":\"b\"}")
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].message").value("must be between 1 and 500 characters"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("${7*7}"))))
                .andExpect(jsonPath("$.details[0].message").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("49"))));
    }

    @Test
    void unexpectedExceptionGives500InternalErrorWithoutDetail() throws Exception {
        mvc.perform(get("/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error").value("Unexpected error."))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("secret"))));
    }

    @Test
    void answersJsonEvenWhenAcceptIsXml() throws Exception {
        mvc.perform(get("/probe/boom").accept(MediaType.TEXT_XML))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void unknownPathGives404NotFound() throws Exception {
        mvc.perform(get("/probe/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    /** Controller on a defined path that lacks a method, so the framework raises its own 405. */
    @RestController
    @RequestMapping("/api/v1/books")
    static class GetOnly {
        @GetMapping
        String list() {
            return "x";
        }
    }

    @Test
    void frameworkMethodNotSupportedCarriesTheAllowOfTheRouteTable() throws Exception {
        MockMvc books = MockMvcBuilders.standaloneSetup(new GetOnly())
                .setControllerAdvice(new GlobalExceptionHandler(factory, routes)).build();

        books.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Allow", "GET, POST"))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void wrongMethodGives405MethodNotAllowed() throws Exception {
        mvc.perform(post("/probe/boom").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unacceptableAcceptGives406NotAcceptable() throws Exception {
        mvc.perform(get("/probe/accepts").accept(MediaType.TEXT_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.code").value("NOT_ACCEPTABLE"));
    }

    @Test
    void unsupportedContentTypeGives415() throws Exception {
        mvc.perform(post("/probe/typed").contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    /** Controller whose actions raise the service exceptions. */
    @RestController
    @RequestMapping("/probe/service")
    static class Failing {
        @GetMapping("/api")
        String api() {
            throw new ApiException(ErrorCode.LIBRARY_FULL);
        }

        @GetMapping("/duplicate")
        String duplicate() {
            throw new DuplicateBookException(new com.globant.globerbooks.domain.Book(
                    "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b", "Clean Code", "Robert Martin",
                    com.globant.globerbooks.domain.BookStatus.UNREAD, null,
                    Instant.parse("2026-02-19T10:00:00.000Z"), null));
        }
    }

    private MockMvc failingMvc() {
        tools.jackson.databind.json.JsonMapper mapper = tools.jackson.databind.json.JsonMapper.builder()
                .propertyNamingStrategy(tools.jackson.databind.PropertyNamingStrategies.SNAKE_CASE).build();
        return MockMvcBuilders.standaloneSetup(new Failing())
                .setMessageConverters(
                        new org.springframework.http.converter.json.JacksonJsonHttpMessageConverter(mapper))
                .setControllerAdvice(new GlobalExceptionHandler(factory, routes)).build();
    }

    @Test
    void apiExceptionBecomesItsCodeAndStatusWithoutDetails() throws Exception {
        failingMvc().perform(get("/probe/service/api"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().string("{\"error\":\"The library is full.\",\"code\":\"LIBRARY_FULL\","
                        + "\"timestamp\":\"2026-02-19T10:00:00.123Z\"}"));
    }

    @Test
    void duplicateExceptionGives409WithExactlyOneExistingBookInTheSpecBody() throws Exception {
        failingMvc().perform(get("/probe/service/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().string("{\"error\":\"A book with the same title and author already exists.\","
                        + "\"code\":\"DUPLICATE_BOOK\",\"timestamp\":\"2026-02-19T10:00:00.123Z\","
                        + "\"details\":{\"existing_book\":{\"id\":\"3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b\","
                        + "\"title\":\"Clean Code\",\"author\":\"Robert Martin\",\"status\":\"unread\","
                        + "\"rating\":null,\"date_added\":\"2026-02-19T10:00:00.000Z\","
                        + "\"date_completed\":null}}}"));
    }

    @Test
    void duplicateBodyHasNoMemberOfTypeObjectAndExceptionMessageIsTheCodeName() {
        for (java.lang.reflect.RecordComponent component : DuplicateErrorBody.class.getRecordComponents()) {
            assertThat(component.getType()).isNotEqualTo(Object.class);
        }
        for (java.lang.reflect.RecordComponent component : DuplicateErrorBody.Details.class.getRecordComponents()) {
            assertThat(component.getType()).isEqualTo(com.globant.globerbooks.dto.BookResponse.class);
        }
        assertThat(new ApiException(ErrorCode.LIBRARY_FULL).getMessage()).isEqualTo("LIBRARY_FULL");
        assertThat(new ApiException(ErrorCode.LIBRARY_FULL).status()).isEqualTo(409);
    }
}
