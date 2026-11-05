package com.globant.globerbooks.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.globant.globerbooks.config.BookText;
import com.globant.globerbooks.service.TextPolicy;
import jakarta.validation.constraints.Pattern;

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
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Pattern(regexp = "unread|reading|completed", message = "must be one of: unread, reading, completed")
        String status,
        Boolean allowDuplicate) {
}
