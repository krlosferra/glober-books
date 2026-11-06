package com.globant.globerbooks.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.globant.globerbooks.config.transport.RouteTable;
import com.globant.globerbooks.dto.BookResponse;
import com.globant.globerbooks.dto.RatingValue;
import com.globant.globerbooks.error.ApiException;
import com.globant.globerbooks.error.ErrorBody.FieldProblem;
import com.globant.globerbooks.error.ErrorBodyFactory;
import com.globant.globerbooks.error.ErrorCode;
import com.globant.globerbooks.error.ErrorResponseWriter;
import com.globant.globerbooks.error.GlobalExceptionHandler;
import com.globant.globerbooks.service.BookService;
import com.globant.globerbooks.service.BookValidationException;
import java.time.Instant;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Slice tests of the two rating endpoints (spec 5.5, 5.6; T-36, T-37). Binding and mapping only; the
 * strictness of the real resolver is proved in full-context tests (SEC-F-159).
 */
@WebMvcTest(BookController.class)
@Import({GlobalExceptionHandler.class, ErrorBodyFactory.class, ErrorResponseWriter.class,
    RouteTable.class, BookControllerTest.FixedClock.class})
class BookRatingControllerTest {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";
    private static final String HOST = "localhost:80";
    private static final String ID = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b";
    private static final BookResponse STORED = new BookResponse(ID, "Clean Code", "Robert Martin",
            "completed", 4, Instant.parse("2026-02-19T10:00:00.000Z"), Instant.parse("2026-02-20T10:00:00.000Z"));

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BookService service;

    private ResultActions putRating(String body) throws Exception {
        return mvc.perform(put("/api/v1/books/" + ID + "/rating").header("Host", HOST)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions deleteRating() throws Exception {
        return mvc.perform(delete("/api/v1/books/" + ID + "/rating").header("Host", HOST)
                .contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void putRatingPassesTheValueToTheServiceAndReturns200() throws Exception {
        when(service.rate(ID, new RatingValue(4))).thenReturn(STORED);

        putRating("{\"rating\":4}").andExpect(status().isOk()).andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.id").value(ID)).andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    void putRatingPassesTheInvalidMarkerForNumbersNullAndAMissingMember() throws Exception {
        when(service.rate(ID, RatingValue.INVALID)).thenThrow(new BookValidationException(
                ErrorCode.INVALID_RATING, List.of(new FieldProblem("rating", "must be an integer from 1 to 5"))));

        for (String body : new String[] {"{\"rating\":777}", "{\"rating\":null}", "{\"rating\":5.0}", "{}"}) {
            putRating(body).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_RATING"))
                    .andExpect(jsonPath("$.details.length()").value(1))
                    .andExpect(jsonPath("$.details[0].field").value("rating"))
                    .andExpect(jsonPath("$.details[0].message").value("must be an integer from 1 to 5"))
                    .andExpect(content().string(Matchers.not(Matchers.containsString("777"))));
        }
    }

    @Test
    void putRatingMapsNotAllowedWithoutDetailsAndNotFoundTo404() throws Exception {
        when(service.rate(ID, new RatingValue(4))).thenThrow(new ApiException(ErrorCode.RATING_NOT_ALLOWED));
        when(service.rate(ID, new RatingValue(2))).thenThrow(new ApiException(ErrorCode.BOOK_NOT_FOUND));

        putRating("{\"rating\":4}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RATING_NOT_ALLOWED"))
                .andExpect(jsonPath("$.details").doesNotExist());
        putRating("{\"rating\":2}").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"));
    }

    @Test
    void putRatingRejectsAWrongTypeOrAnUnknownMemberAsMalformedWithoutAServiceCall() throws Exception {
        for (String body : new String[] {"{\"rating\":\"4\"}", "{\"rating\":true}", "{\"rating\":4,\"x\":1}"}) {
            putRating(body).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        }

        org.mockito.Mockito.verify(service, org.mockito.Mockito.never()).rate(any(), any());
    }

    @Test
    void deleteRatingReturns200WithTheBookAndMapsNotFoundTo404() throws Exception {
        when(service.unrate(ID)).thenReturn(STORED);

        deleteRating().andExpect(status().isOk()).andExpect(jsonPath("$.id").value(ID));
        verify(service).unrate(ID);

        when(service.unrate(ID)).thenThrow(new ApiException(ErrorCode.BOOK_NOT_FOUND));
        deleteRating().andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"));
    }
}
