package com.globant.globerbooks.config.json;

import com.globant.globerbooks.config.transport.BoundedBodyRequest;
import com.globant.globerbooks.error.MalformedRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Resolves a {@code @RequestBody} argument strictly (BR-012 step 5). The buffered body is decoded
 * with a UTF-8 decoder that reports bad input instead of replacing it, then parsed by the strict
 * mapper. Every failure is a {@link MalformedRequestException} with a fixed message: neither the
 * parser message nor any part of the body leaves this class (SC-31). {@code @Valid} is honoured
 * afterwards, so field problems stay {@code VALIDATION_ERROR} or {@code INVALID_STATUS}.
 */
public class StrictJsonBodyResolver implements HandlerMethodArgumentResolver {

    private final JsonMapper mapper;

    /**
     * Creates the resolver.
     *
     * @param mapper the strict mapper
     */
    public StrictJsonBodyResolver(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestBody.class);
    }

    @Override
    @Nullable
    public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer container,
            NativeWebRequest request, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest servlet = request.getNativeRequest(HttpServletRequest.class);
        if (servlet == null) {
            throw new MalformedRequestException();
        }
        Object value = parse(decode(servlet), mapper.constructType(parameter.getNestedGenericParameterType()));
        if (value == null) {
            throw new MalformedRequestException();
        }
        validateIfRequested(parameter, value, request, binderFactory);
        return value;
    }

    private static String decode(HttpServletRequest request) {
        try {
            byte[] bytes = request.getInputStream().readNBytes(BoundedBodyRequest.LIMIT + 1);
            if (bytes.length == 0 || bytes.length > BoundedBodyRequest.LIMIT) {
                throw new MalformedRequestException();
            }
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (IOException failure) {
            throw new MalformedRequestException();
        }
    }

    private Object parse(String text, JavaType type) {
        try {
            return mapper.readValue(text, type);
        } catch (JacksonException failure) {
            throw new MalformedRequestException();
        }
    }

    private static void validateIfRequested(MethodParameter parameter, Object value, NativeWebRequest request,
            @Nullable WebDataBinderFactory binderFactory) throws Exception {
        boolean requested = parameter.hasParameterAnnotation(Valid.class)
                || parameter.hasParameterAnnotation(Validated.class);
        if (!requested || binderFactory == null) {
            return;
        }
        WebDataBinder binder = binderFactory.createBinder(request, value, "request");
        binder.validate();
        BindingResult result = binder.getBindingResult();
        if (result.hasErrors()) {
            throw new MethodArgumentNotValidException(parameter, result);
        }
    }
}
