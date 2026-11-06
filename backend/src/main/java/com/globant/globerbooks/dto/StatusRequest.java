package com.globant.globerbooks.dto;

import jakarta.validation.constraints.Pattern;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Request body of {@code PUT /api/v1/books/{id}/status} (spec 5.5). The value must be one of the
 * three exact lower-case statuses (BR-001). A missing member and an explicit {@code null} are read
 * as the empty text, which is not a status, so both give {@code INVALID_STATUS} (US-006.6); a
 * number, boolean, array or object is a type mismatch ({@code MALFORMED_REQUEST}).
 *
 * @param status the wire status; never null
 */
public record StatusRequest(
        @JsonDeserialize(using = CreateBookRequest.StatusDeserializer.class)
        @Pattern(regexp = "unread|reading|completed", message = "must be one of: unread, reading, completed")
        String status) {

    /** Turns a missing member into the empty text. */
    public StatusRequest {
        if (status == null) {
            status = "";
        }
    }
}
