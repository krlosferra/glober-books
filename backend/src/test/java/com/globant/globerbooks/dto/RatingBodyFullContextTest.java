package com.globant.globerbooks.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globant.globerbooks.config.json.StrictJsonBodyResolver;
import com.globant.globerbooks.error.MalformedRequestException;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * Full-context proof (SEC-F-159) that the rating and status bodies are read strictly by the
 * resolver the application installs in front of the framework's own, with the application's own
 * mapper and validator. Expected codes come from BR-001, BR-002 and Appendix D T-17, T-18, T-19,
 * T-31, T-32, T-37; the sorting into codes happens in the advice, so here "invalid" means the
 * marker or the failed pattern and "malformed" means {@link MalformedRequestException}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class RatingBodyFullContextTest {

    @TempDir
    static Path dataDir;

    @Autowired
    private RequestMappingHandlerAdapter adapter;

    @Autowired
    private jakarta.validation.Validator validator;

    /** Holder of annotated parameters. */
    static final class Handler {
        void rate(@Valid @RequestBody RatingRequest body) { }

        void status(@Valid @RequestBody StatusRequest body) { }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("rating.db"));
    }

    private Object resolve(String method, Class<?> type, String body) throws Exception {
        Method found = Handler.class.getDeclaredMethod(method, type);
        MethodParameter parameter = new MethodParameter(found, 0);
        HandlerMethodArgumentResolver resolver = chosen(parameter);
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/x");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return resolver.resolveArgument(parameter, null, new ServletWebRequest(request),
                (web, target, name) -> {
                    WebDataBinder binder = new WebDataBinder(target, name);
                    binder.setValidator(new SpringValidatorAdapter(validator));
                    return binder;
                });
    }

    @Test
    void theStrictResolverComesBeforeTheFrameworkProcessor() throws Exception {
        Method found = Handler.class.getDeclaredMethod("rate", RatingRequest.class);

        HandlerMethodArgumentResolver chosen = chosen(new MethodParameter(found, 0));

        assertThat(chosen).isInstanceOf(StrictJsonBodyResolver.class);
    }

    private HandlerMethodArgumentResolver chosen(MethodParameter parameter) {
        return adapter.getArgumentResolvers().stream()
                .filter(candidate -> candidate.supportsParameter(parameter)).findFirst().orElseThrow();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "2", "3", "4", "5"})
    void readsTheFiveValidRatings(String literal) throws Exception {
        RatingRequest request = (RatingRequest) resolve("rate", RatingRequest.class, "{\"rating\":" + literal + "}");

        assertThat(request.rating().rating()).contains(Integer.parseInt(literal));
    }

    @ParameterizedTest
    @MethodSource("com.globant.globerbooks.dto.RatingValueDeserializerTest#invalidLiterals")
    void readsEveryInvalidLiteralAsTheMarkerNeverAsAnError(String literal) throws Exception {
        RatingRequest request = (RatingRequest) resolve("rate", RatingRequest.class, "{\"rating\":" + literal + "}");

        assertThat(request.rating()).isEqualTo(RatingValue.INVALID);
    }

    @Test
    void anEmptyObjectIsTheMarker() throws Exception {
        RatingRequest request = (RatingRequest) resolve("rate", RatingRequest.class, "{}");

        assertThat(request.rating()).isEqualTo(RatingValue.INVALID);
    }

    @ParameterizedTest
    @MethodSource("com.globant.globerbooks.dto.RatingValueDeserializerTest#malformedLiterals")
    void refusesEveryMalformedLiteralWithMalformedRequest(String literal) {
        assertThatThrownBy(() -> resolve("rate", RatingRequest.class, "{\"rating\":" + literal + "}"))
                .isInstanceOf(MalformedRequestException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"rating\":4,\"rating\":5}", "{\"status\":\"completed\",\"rating\":5}",
        "{\"rating\":5,\"status\":\"completed\"}", "{\"rating\":5}x", "[5]", "5", "null", ""})
    void refusesDuplicateUnknownAndWrongDocuments(String body) {
        assertThatThrownBy(() -> resolve("rate", RatingRequest.class, body))
                .isInstanceOf(MalformedRequestException.class);
    }

    @Test
    void theHugeExponentIsJudgedWithinThreeHundredMilliseconds() throws Exception {
        resolve("rate", RatingRequest.class, "{\"rating\":1}");
        long started = System.nanoTime();
        Object value = resolve("rate", RatingRequest.class, "{\"rating\":1e999999999}");
        long millis = (System.nanoTime() - started) / 1_000_000;

        assertThat(((RatingRequest) value).rating()).isEqualTo(RatingValue.INVALID);
        assertThat(millis).isLessThan(300);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unread", "reading", "completed"})
    void readsTheThreeStatuses(String status) throws Exception {
        String body = "{\"status\":\"" + status + "\"}";
        StatusRequest request = (StatusRequest) resolve("status", StatusRequest.class, body);

        assertThat(request.status()).isEqualTo(status);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"status\":\"Completed\"}", "{\"status\":\"\"}", "{\"status\":null}", "{}",
        "{\"status\":\"reading \"}", "{\"status\":\" reading\"}"})
    void wrongOrMissingStatusFailsTheFieldCheckOnTheStatusField(String body) {
        assertThatThrownBy(() -> resolve("status", StatusRequest.class, body))
                .isInstanceOfSatisfying(MethodArgumentNotValidException.class, failure -> {
                    assertThat(failure.getBindingResult().getFieldErrors()).hasSize(1);
                    assertThat(failure.getBindingResult().getFieldError().getField()).isEqualTo("status");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"status\":5}", "{\"status\":true}", "{\"status\":[]}", "{\"status\":{}}",
        "{\"status\":\"reading\",\"x\":1}", "{\"status\":\"reading\",\"status\":\"unread\"}",
        "{\"status\":\"\\uD800\"}"})
    void wrongTypeOrExtraMemberIsMalformed(String body) {
        assertThatThrownBy(() -> resolve("status", StatusRequest.class, body))
                .isInstanceOf(MalformedRequestException.class);
    }
}
