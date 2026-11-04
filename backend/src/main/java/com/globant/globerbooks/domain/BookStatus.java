package com.globant.globerbooks.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Reading status of a book; the wire and stored value is the lower-case name. */
public enum BookStatus {

    /** Not started. */
    UNREAD("unread"),
    /** In progress. */
    READING("reading"),
    /** Finished. */
    COMPLETED("completed");

    private final String wireValue;

    BookStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the lower-case wire value.
     *
     * @return {@code unread}, {@code reading} or {@code completed}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Finds the status for an exact lower-case wire value.
     *
     * @param value the text to look up; may be null
     * @return the status, or empty when the text is not one of the three exact values
     */
    public static Optional<BookStatus> fromWire(String value) {
        for (BookStatus status : values()) {
            if (status.wireValue.equals(value)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }

    /** Stores the status as its lower-case wire value. */
    @Converter
    public static class WireConverter implements AttributeConverter<BookStatus, String> {

        @Override
        public String convertToDatabaseColumn(BookStatus attribute) {
            return attribute == null ? null : attribute.wireValue;
        }

        @Override
        public BookStatus convertToEntityAttribute(String dbData) {
            if (dbData == null) {
                return null;
            }
            return fromWire(dbData).orElseThrow(() -> new IllegalStateException("Unknown stored status"));
        }
    }
}
