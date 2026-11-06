package com.globant.globerbooks.dto;

import com.globant.globerbooks.config.BookText;
import com.globant.globerbooks.config.json.StrictStringDeserializer;
import com.globant.globerbooks.service.TextPolicy;
import jakarta.validation.constraints.Pattern;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Request body of {@code POST /api/v1/books} (spec 5.2). Components are declared in the order of
 * the {@code details} entries: title, author, status. {@code allowDuplicate} is accepted and
 * ignored until Task 23a.
 *
 * @param title the title; checked by the text policy
 * @param author the author; checked by the text policy
 * @param status the wire status; absent (null) means {@code unread}, an explicit JSON null is read
 *     as the empty text and so is not one of the three values
 * @param allowDuplicate accepted without effect
 */
public record CreateBookRequest(
        @BookText(max = TextPolicy.TITLE_MAX) String title,
        @BookText(max = TextPolicy.AUTHOR_MAX) String author,
        @JsonDeserialize(using = CreateBookRequest.StatusDeserializer.class)
        @Pattern(regexp = "unread|reading|completed", message = "must be one of: unread, reading, completed")
        String status,
        Boolean allowDuplicate) {

    /**
     * Reads {@code status} as text, strictly (a number or boolean is a type mismatch). A missing
     * member stays null (the default {@code unread}); an explicit JSON null becomes the empty
     * text, which is not a status (BR-001, DEC-016).
     */
    public static final class StatusDeserializer extends StrictStringDeserializer {

        @Override
        public Object getNullValue(DeserializationContext context) {
            return "";
        }

        @Override
        public Object getAbsentValue(DeserializationContext context) {
            return null;
        }
    }
}
