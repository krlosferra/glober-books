package com.globant.globerbooks.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Test-only filter behind the gate: when {@code X-Probe-Send-Error} is present it calls
 * {@code sendError} with that status and a message, after a {@code reset} when {@code X-Probe-Reset}
 * is present. It exists to prove that a {@code sendError} raised inside the application is the envelope.
 */
@TestConfiguration
public class ErrorProbeConfig {

    /**
     * Registers the probe filter just after the gate.
     *
     * @return the registration
     */
    @Bean
    FilterRegistrationBean<OncePerRequestFilter> errorProbe() {
        OncePerRequestFilter probe = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    FilterChain chain) throws ServletException, IOException {
                String status = request.getHeader("X-Probe-Send-Error");
                if (status == null) {
                    chain.doFilter(request, response);
                    return;
                }
                if (request.getHeader("X-Probe-Reset") != null) {
                    response.reset();
                }
                response.sendError(Integer.parseInt(status), "secret probe text");
            }
        };
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(probe);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
