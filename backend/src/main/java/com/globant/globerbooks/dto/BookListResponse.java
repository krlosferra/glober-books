package com.globant.globerbooks.dto;

import java.util.List;

/**
 * Body of {@code GET /api/v1/books} (spec 5.1).
 *
 * @param books the books in list order
 * @param total the number of books returned
 */
public record BookListResponse(List<BookResponse> books, int total) {

    /**
     * Copies the list so the record is immutable.
     */
    public BookListResponse {
        books = List.copyOf(books);
    }

    /**
     * Builds the response for a list; {@code total} is its size.
     *
     * @param books the books in list order
     * @return the response
     */
    public static BookListResponse of(List<BookResponse> books) {
        return new BookListResponse(books, books.size());
    }
}
