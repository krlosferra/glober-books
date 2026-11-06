package com.globant.globerbooks.dto;

/**
 * Request body of {@code PUT /api/v1/books/{id}/rating} (spec 5.6). A missing {@code rating}
 * member is the invalid marker, like an explicit {@code null} (BR-002); unknown members are
 * refused by the strict mapper.
 *
 * @param rating the rating as written; never null
 */
public record RatingRequest(RatingValue rating) {

    /** Replaces a missing member by the invalid marker. */
    public RatingRequest {
        if (rating == null) {
            rating = RatingValue.INVALID;
        }
    }
}
