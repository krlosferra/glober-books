package com.globant.globerbooks.config.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globant.globerbooks.config.WebMvcConfig;
import com.globant.globerbooks.config.transport.BoundedBodyRequest;
import com.globant.globerbooks.error.MalformedRequestException;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests of {@link StrictJsonBodyResolver} and of its installation by {@link WebMvcConfig}
 * (BR-012 step 5, SC-31). Expected outcomes come from spec 5.0 and Appendix D.
 */
class StrictJsonBodyResolverTest {

    /** Probe body with one rule so {@code @Valid} can be seen working. */
    record Probe(@NotBlank String title) { }

    /** Holder of annotated parameters. */
    static final class Handler {
        void validated(@Valid @RequestBody Probe body) { }

        void unvalidated(@RequestBody Probe body) { }

        void plain(Probe body) { }
    }

    private final JsonMapper mapper = strictMapper();
    private final StrictJsonBodyResolver resolver = new StrictJsonBodyResolver(mapper);

    private static JsonMapper strictMapper() {
        JsonFactoryBuilder factory = JsonFactory.builder();
        JacksonConfig.strictFactory(factory);
        JsonMapper.Builder builder = JsonMapper.builder(factory.build());
        JacksonConfig.strictMapper(builder);
        return builder.build();
    }

    private static MethodParameter parameter(String method) throws NoSuchMethodException {
        Method found = Handler.class.getDeclaredMethod(method, Probe.class);
        return new MethodParameter(found, 0);
    }

    private Object resolve(String method, byte[] body) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setContent(body);
        return resolve(method, request);
    }

    private Object resolve(String method, HttpServletRequest request) throws Exception {
        return resolver.resolveArgument(parameter(method), null, new ServletWebRequest(request),
                (web, target, name) -> {
                    WebDataBinder binder = new WebDataBinder(target, name);
                    binder.setValidator(new SpringValidatorAdapter(
                            Validation.buildDefaultValidatorFactory().getValidator()));
                    return binder;
                });
    }

    private static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void supportsOnlyParametersMarkedAsRequestBody() throws Exception {
        assertThat(resolver.supportsParameter(parameter("validated"))).isTrue();
        assertThat(resolver.supportsParameter(parameter("plain"))).isFalse();
    }

    @Test
    void readsAValidBodyIntoTheDeclaredType() throws Exception {
        Object value = resolve("validated", utf8("{\"title\":\"Clean Code\"}"));
        assertThat(value).isEqualTo(new Probe("Clean Code"));
    }

    @Test
    void readsMultiByteUtf8WithoutLoss() throws Exception {
        Object value = resolve("validated", utf8("{\"title\":\"Straße 中文\"}"));
        assertThat(value).isEqualTo(new Probe("Straße 中文"));
    }

    @Test
    void rejectsInvalidUtf8WithoutReplacing() {
        byte[] body = {'{', '"', 't', 'i', 't', 'l', 'e', '"', ':', '"', (byte) 0xFF, '"', '}'};
        assertThatThrownBy(() -> resolve("validated", body)).isInstanceOf(MalformedRequestException.class);
    }

    @Test
    void rejectsTruncatedAndOverlongSequences() {
        byte[] truncated = {'{', '"', 't', 'i', 't', 'l', 'e', '"', ':', '"', (byte) 0xE2, (byte) 0x82, '"', '}'};
        byte[] overlong = {'{', '"', 't', 'i', 't', 'l', 'e', '"', ':', '"', (byte) 0xC0, (byte) 0x80, '"', '}'};
        assertThatThrownBy(() -> resolve("validated", truncated)).isInstanceOf(MalformedRequestException.class);
        assertThatThrownBy(() -> resolve("validated", overlong)).isInstanceOf(MalformedRequestException.class);
    }

    @Test
    void rejectsByteOrderMark() {
        byte[] body = new byte[3 + 15];
        byte[] mark = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        System.arraycopy(mark, 0, body, 0, 3);
        System.arraycopy(utf8("{\"title\":\"abc\"}"), 0, body, 3, 15);
        assertThatThrownBy(() -> resolve("validated", body)).isInstanceOf(MalformedRequestException.class);
    }

    @Test
    void rejectsZeroByteBodyAndJsonNull() {
        assertThatThrownBy(() -> resolve("validated", new byte[0])).isInstanceOf(MalformedRequestException.class);
        assertThatThrownBy(() -> resolve("validated", utf8("null"))).isInstanceOf(MalformedRequestException.class);
    }

    @Test
    void rejectsNonObjectValues() {
        for (String body : new String[] {"[]", "\"x\"", "5", "true"}) {
            assertThatThrownBy(() -> resolve("validated", utf8(body))).as(body)
                    .isInstanceOf(MalformedRequestException.class);
        }
    }

    @Test
    void rejectsABodyOverTheCapEvenIfTheGateWasBypassed() {
        byte[] body = new byte[BoundedBodyRequest.LIMIT + 1];
        Arrays.fill(body, (byte) ' ');
        assertThatThrownBy(() -> resolve("validated", body)).isInstanceOf(MalformedRequestException.class);
    }

    @Test
    void failureCarriesAFixedMessageWithNoBodyContent() {
        assertThatThrownBy(() -> resolve("validated", utf8("{\"title\":SECRETVALUE}")))
                .isInstanceOf(MalformedRequestException.class)
                .hasMessage("malformed request")
                .hasNoCause();
    }

    @Test
    void mapsAReadFailureOfTheStreamToMalformed() {
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public ServletInputStream getInputStream() {
                return new ServletInputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("boom SECRETVALUE");
                    }

                    @Override
                    public boolean isFinished() {
                        return false;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(ReadListener listener) {
                        throw new IllegalStateException();
                    }
                };
            }
        };
        assertThatThrownBy(() -> resolve("validated", request)).isInstanceOf(MalformedRequestException.class)
                .hasMessage("malformed request");
    }

    @Test
    void runsBeanValidationWhenTheParameterIsMarkedValid() {
        assertThatThrownBy(() -> resolve("validated", utf8("{\"title\":\"  \"}")))
                .isInstanceOf(MethodArgumentNotValidException.class);
    }

    @Test
    void skipsBeanValidationWhenTheParameterIsNotMarkedValid() throws Exception {
        assertThat(resolve("unvalidated", utf8("{\"title\":\"  \"}"))).isEqualTo(new Probe("  "));
    }

    @Test
    void configPutsTheStrictResolverBeforeTheFrameworkOnes() throws Exception {
        RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
        adapter.setMessageConverters(List.of());
        adapter.setApplicationContext(new StaticApplicationContext());
        adapter.afterPropertiesSet();
        int before = adapter.getArgumentResolvers().size();
        ObjectProvider<JsonMapper> provider = new StaticListableBeanFactory(Map.of("mapper", mapper))
                .getBeanProvider(JsonMapper.class);
        BeanPostProcessor installer = WebMvcConfig.strictBodyResolverInstaller(provider);
        installer.postProcessAfterInitialization(adapter, "adapter");
        assertThat(adapter.getArgumentResolvers()).hasSize(before + 1);
        assertThat(adapter.getArgumentResolvers().get(0)).isInstanceOf(StrictJsonBodyResolver.class);
    }
}
