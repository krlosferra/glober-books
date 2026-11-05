package com.globant.globerbooks.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.globant.globerbooks.config.transport.RouteTable;
import com.globant.globerbooks.config.transport.StaticAssetCatalog;
import com.globant.globerbooks.dto.BookListResponse;
import com.globant.globerbooks.dto.BookResponse;
import com.globant.globerbooks.dto.CreateBookRequest;
import com.globant.globerbooks.error.ErrorBody.FieldProblem;
import com.globant.globerbooks.error.ErrorBodyFactory;
import com.globant.globerbooks.error.ErrorCode;
import com.globant.globerbooks.error.ErrorResponseWriter;
import com.globant.globerbooks.error.GlobalExceptionHandler;
import com.globant.globerbooks.service.BookService;
import com.globant.globerbooks.service.BookValidationException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Slice tests of the book endpoints: binding, status codes, error mapping (spec 5.1, 5.2). */
@WebMvcTest(BookController.class)
@Import({GlobalExceptionHandler.class, ErrorBodyFactory.class, ErrorResponseWriter.class,
    RouteTable.class, BookControllerTest.FixedClock.class})
class BookControllerTest {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";
    private static final String HOST = "localhost:80";
    private static final String STORED_ID = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b";
    private static final BookResponse STORED = new BookResponse(STORED_ID, "Clean Code", "Robert Martin",
            "unread", null, Instant.parse("2026-02-19T10:00:00.000Z"), null);

    /** Fixed clock for the error timestamp. */
    @TestConfiguration
    static class FixedClock {
        @Bean
        StaticAssetCatalog catalog() {
            return new StaticAssetCatalog("classpath:/static/");
        }

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-02-19T10:00:00.000Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BookService service;

    private org.springframework.test.web.servlet.ResultActions add(String body) throws Exception {
        return mvc.perform(post("/api/v1/books").header("Host", HOST)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void addReturns201WithTheBook() throws Exception {
        when(service.create(any(CreateBookRequest.class))).thenReturn(STORED);

        add("{\"title\":\"Clean Code\",\"author\":\"Robert Martin\",\"allow_duplicate\":false}")
                .andExpect(status().isCreated())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.id").value(STORED_ID))
                .andExpect(jsonPath("$.status").value("unread"))
                .andExpect(jsonPath("$.date_added").value("2026-02-19T10:00:00.000Z"));
    }

    @Test
    void addPassesSnakeCaseMembersToTheService() throws Exception {
        when(service.create(any(CreateBookRequest.class))).thenReturn(STORED);

        add("{\"title\":\"T\",\"author\":\"A\",\"status\":\"reading\",\"allow_duplicate\":true}")
                .andExpect(status().isCreated());

        verify(service).create(new CreateBookRequest("T", "A", "reading", true));
    }

    @Test
    void addRejectsEmptyTitleWithFieldAndNoServiceCall() throws Exception {
        add("{\"title\":\"\",\"author\":\"A\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("title"))
                .andExpect(jsonPath("$.details[0].message").value("must not be empty"))
                .andExpect(jsonPath("$.timestamp").value("2026-02-19T10:00:00.000Z"));

        verify(service, never()).create(any());
    }

    @Test
    void addRejectsMissingTitleAndAuthorWithBothFields() throws Exception {
        add("{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("title"))
                .andExpect(jsonPath("$.details[0].message").value("is required"))
                .andExpect(jsonPath("$.details[1].field").value("author"))
                .andExpect(jsonPath("$.details[1].message").value("is required"))
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    void addMapsServiceValidationFailure() throws Exception {
        when(service.create(any(CreateBookRequest.class))).thenThrow(new BookValidationException(
                ErrorCode.INVALID_STATUS,
                List.of(new FieldProblem("status", "must be one of: unread, reading, completed"))));

        add("{\"title\":\"T\",\"author\":\"A\",\"status\":\"bogus\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS"))
                .andExpect(jsonPath("$.details[0].field").value("status"));
    }

    @Test
    void addRejectsUnknownMemberAsMalformed() throws Exception {
        add("{\"title\":\"T\",\"author\":\"A\",\"isbn\":\"1\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void addRejectsUnreadableJsonAsMalformed() throws Exception {
        add("{\"title\":").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void listReturns200WithBooksAndTotal() throws Exception {
        when(service.list()).thenReturn(List.of(STORED));

        mvc.perform(get("/api/v1/books").header("Host", HOST))
                .andExpect(status().isOk())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.books[0].id").value(STORED_ID));
    }

    @Test
    void emptyListReturnsEmptyArrayAndZero() throws Exception {
        when(service.list()).thenReturn(List.of());

        mvc.perform(get("/api/v1/books").header("Host", HOST))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"books\":[],\"total\":0}"));
    }

    @Test
    void endpointsExposeOnlyDtosAndNeverBufferTheBody() {
        for (Method method : BookController.class.getDeclaredMethods()) {
            if (method.isSynthetic()) {
                continue;
            }
            assertThat(method.getReturnType()).isEqualTo(ResponseEntity.class);
            assertThat(method.getGenericReturnType().getTypeName())
                    .doesNotContain("domain.Book>").doesNotContain("byte[]").doesNotContain("String");
            Arrays.stream(method.getParameterTypes()).forEach(type ->
                    assertThat(type.getName()).isNotEqualTo("com.globant.globerbooks.domain.Book"));
        }
        assertThat(BookListResponse.class.isRecord()).isTrue();
    }
}
