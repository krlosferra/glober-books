package com.globant.globerbooks.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body of {@code POST /api/v1/books} (spec 5.2). Basic checks only; the full text policy
 * arrives with Tasks 18 to 20. {@code allowDuplicate} is accepted and ignored until Task 23a.
 *
 * @param title the title; required, not blank
 * @param author the author; required, not blank
 * @param status the wire status, or null for the default {@code unread}
 * @param allowDuplicate accepted without effect
 */
public record CreateBookRequest(@NotBlank String title, @NotBlank String author, String status,
        Boolean allowDuplicate) {
}
