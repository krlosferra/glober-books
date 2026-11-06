package com.globant.globerbooks.config;

import com.globant.globerbooks.service.BookValidationException;
import com.globant.globerbooks.service.TextPolicy;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bean Validation constraint for a title or an author. The check is {@link TextPolicy#check}; the
 * violation message is one of the fixed texts of the spec, never built from the value.
 */
@Documented
@Constraint(validatedBy = BookText.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface BookText {

    /**
     * Fallback message; the validator replaces it with the fixed text of the failing check.
     *
     * @return the template
     */
    String message() default "is invalid";

    /**
     * Validation groups.
     *
     * @return the groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload, unused.
     *
     * @return the payload types
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * Maximum length in code points.
     *
     * @return the limit, {@link TextPolicy#TITLE_MAX} or {@link TextPolicy#AUTHOR_MAX}
     */
    int max();

    /** Validator that delegates to the text policy. */
    class Validator implements ConstraintValidator<BookText, String> {

        private int max;

        @Override
        public void initialize(BookText annotation) {
            this.max = annotation.max();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return TextPolicy.check(value, max).map(problem -> {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        BookValidationException.messageFor(problem, max)).addConstraintViolation();
                return false;
            }).orElse(true);
        }
    }
}
