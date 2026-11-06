package com.globant.globerbooks.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.globant.globerbooks.dto.BookResponse;

/**
 * The {@code 409 DUPLICATE_BOOK} envelope (spec Appendix A, 5.2): the common members and {@code details}
 * holding exactly one member, {@code existing_book}. Typed on purpose: no {@code Object}, map or tree.
 *
 * @param error informative fixed text
 * @param code the error code name
 * @param timestamp UTC timestamp from the shared factory
 * @param details the one-member details object
 */
public record DuplicateErrorBody(String error, String code, String timestamp, Details details) {

    /**
     * Builds the body from an envelope made by the factory and the existing book.
     *
     * @param base the factory envelope for {@code DUPLICATE_BOOK}
     * @param existing the existing book
     * @return the body
     */
    public static DuplicateErrorBody of(ErrorBody base, BookResponse existing) {
        return new DuplicateErrorBody(base.error(), base.code(), base.timestamp(), new Details(existing));
    }

    /**
     * The details of a duplicate: one book and nothing else.
     *
     * @param existingBook the matching book
     */
    public record Details(@JsonProperty("existing_book") BookResponse existingBook) {
    }
}
