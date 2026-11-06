package com.globant.globerbooks.dto;

import com.globant.globerbooks.config.json.JacksonConfig;
import com.globant.globerbooks.domain.Book;
import java.util.Optional;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * A rating as it was written in the request (BR-002). Only an integer token whose text is exactly
 * {@code 1} to {@code 5} is valid. Every other number (judged from its token and text, never
 * converted), an explicit {@code null} and a missing member give the invalid marker; any other
 * token type is a type mismatch (the strict resolver turns it into {@code MALFORMED_REQUEST}).
 *
 * @param value the rating 1 to 5, or null for the invalid marker
 */
@JsonDeserialize(using = RatingValue.Reader.class)
public record RatingValue(Integer value) {

    /** The marker of a number or null that is not a rating. */
    public static final RatingValue INVALID = new RatingValue(null);

    /**
     * Tells whether the literal was a rating from 1 to 5.
     *
     * @return true when valid
     */
    public boolean isValid() {
        return value != null;
    }

    /**
     * Returns the rating.
     *
     * @return the rating, or empty for the invalid marker
     */
    public Optional<Integer> rating() {
        return Optional.ofNullable(value);
    }

    /** Reads the token without any numeric conversion. */
    public static final class Reader extends StdDeserializer<RatingValue> {

        private static final long serialVersionUID = 1L;

        /** Creates the deserializer. */
        public Reader() {
            super(RatingValue.class);
        }

        @Override
        public RatingValue deserialize(JsonParser parser, DeserializationContext context) {
            JsonToken token = parser.currentToken();
            if (token != JsonToken.VALUE_NUMBER_INT && token != JsonToken.VALUE_NUMBER_FLOAT) {
                return (RatingValue) context.handleUnexpectedToken(RatingValue.class, parser);
            }
            String text = parser.getString();
            if (text.length() > JacksonConfig.MAX_NUMBER) {
                // The read constraint counts digits only; the spec counts the literal as written.
                return (RatingValue) context.reportInputMismatch(RatingValue.class, "number too long");
            }
            return token == JsonToken.VALUE_NUMBER_INT ? fromText(text) : INVALID;
        }

        @Override
        public RatingValue getNullValue(DeserializationContext context) {
            return INVALID;
        }

        private static RatingValue fromText(String text) {
            if (text.length() == 1 && text.charAt(0) >= '0' + Book.MIN_RATING
                    && text.charAt(0) <= '0' + Book.MAX_RATING) {
                return new RatingValue(text.charAt(0) - '0');
            }
            return INVALID;
        }
    }
}
