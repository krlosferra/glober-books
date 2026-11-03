package com.globant.globerbooks.error;

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
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new Probe())
            .setControllerAdvice(new GlobalExceptionHandler(factory))
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
}
