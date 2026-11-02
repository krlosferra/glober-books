package com.globant.globerbooks.error;

/**
 * The sixteen error codes of the API with their HTTP status and fixed informative text
 * (spec Appendix A). Texts never contain user input, class names, SQL or paths.
 */
public enum ErrorCode {
    /** Title or author invalid, or two or more fields invalid. */
    VALIDATION_ERROR(400, "The request contains invalid values."),
    /** Status value not one of the three. */
    INVALID_STATUS(400, "Status must be unread, reading or completed."),
    /** Rating not an integer from 1 to 5. */
    INVALID_RATING(400, "Rating must be an integer from 1 to 5."),
    /** Valid rating on a book that is not completed. */
    RATING_NOT_ALLOWED(400, "Only completed books can be rated."),
    /** Well-formed id with no book. */
    BOOK_NOT_FOUND(404, "Book not found."),
    /** Add matches an existing book. */
    DUPLICATE_BOOK(409, "A book with the same title and author already exists."),
    /** Add when the library holds 5,000 books. */
    LIBRARY_FULL(409, "The library is full."),
    /** Bad JSON or any parser failure. */
    MALFORMED_REQUEST(400, "The request is malformed."),
    /** Host not allowed. */
    INVALID_HOST(400, "The host is not allowed."),
    /** Origin or Fetch-Metadata not allowed. */
    FORBIDDEN(403, "The request is not allowed."),
    /** Unknown or not routable path. */
    NOT_FOUND(404, "Resource not found."),
    /** Method not defined for the path. */
    METHOD_NOT_ALLOWED(405, "Method not allowed."),
    /** Accept does not allow JSON. */
    NOT_ACCEPTABLE(406, "The response format is not acceptable."),
    /** Body over 16 KiB. */
    PAYLOAD_TOO_LARGE(413, "The request body is too large."),
    /** Content-Type missing or outside the grammar. */
    UNSUPPORTED_MEDIA_TYPE(415, "The content type is not supported."),
    /** Unexpected failure. */
    INTERNAL_ERROR(500, "Unexpected error.");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * Returns the HTTP status of this code.
     *
     * @return the status
     */
    public int status() {
        return status;
    }

    /**
     * Returns the fixed informative text of this code.
     *
     * @return the message
     */
    public String message() {
        return message;
    }
}
