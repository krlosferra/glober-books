package com.globant.globerbooks.error;

import com.globant.globerbooks.domain.Book;
import com.globant.globerbooks.dto.BookResponse;

/**
 * Raised when an add matches an existing book (BR-003). It carries the earliest match, which the advice
 * puts in {@code details.existing_book}. Its message is the code name.
 */
public final class DuplicateBookException extends ApiException {

    private static final long serialVersionUID = 1L;

    private final transient BookResponse existingBook;

    /**
     * Creates the exception.
     *
     * @param existing the earliest matching book
     */
    public DuplicateBookException(Book existing) {
        super(ErrorCode.DUPLICATE_BOOK);
        this.existingBook = BookResponse.from(existing);
    }

    /**
     * Returns the existing book in its wire representation.
     *
     * @return the book
     */
    public BookResponse existingBook() {
        return existingBook;
    }
}
