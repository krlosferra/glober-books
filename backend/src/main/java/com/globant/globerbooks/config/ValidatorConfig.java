package com.globant.globerbooks.config;

import jakarta.validation.MessageInterpolator;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Builds the validator factory with a message interpolator that evaluates nothing, so no
 * expression language runs and no text from a request can become part of a message.
 */
@Configuration(proxyBeanMethods = false)
public class ValidatorConfig {

    /**
     * Creates the validator factory.
     *
     * @return a factory whose messages are returned exactly as written
     */
    @Bean
    public static LocalValidatorFactoryBean defaultValidator() {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.setMessageInterpolator(new LiteralMessageInterpolator());
        return factory;
    }

    /** Returns every template unchanged; it never resolves {@code {...}} or {@code ${...}}. */
    static final class LiteralMessageInterpolator implements MessageInterpolator {

        @Override
        public String interpolate(String template, Context context) {
            return template;
        }

        @Override
        public String interpolate(String template, Context context, Locale locale) {
            return template;
        }
    }
}
