package com.globant.globerbooks.dto;

import com.globant.globerbooks.domain.Book;
import java.time.Instant;

/**
 * Wire representation of a book (spec Section 5.0): exactly seven members; {@code rating} and
 * {@code date_completed} are written as null when absent.
 *
 * @param id lower-case UUID text
 * @param title stored title
 * @param author stored author
 * @param status lower-case status
 * @param rating rating 1 to 5, or null
 * @param dateAdded creation instant
 * @param dateCompleted completion instant, or null
 */
public record BookResponse(String id, String title, String author, String status, Integer rating,
        Instant dateAdded, Instant dateCompleted) {

    /**
     * Maps an entity to its response.
     *
     * @param book the entity
     * @return the response with the lower-case status and null for absent members
     */
    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(),
                book.getStatus().wireValue(), book.getRating().orElse(null), book.getDateAdded(),
                book.getDateCompleted().orElse(null));
    }
}
