package com.globant.globerbooks.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * A book of the library, stored in the {@code books} table.
 *
 * <p>The entity has no public setters. The text policy lives in the service layer; the status and
 * rating transitions that keep INV-1 to INV-4 live here, and the database CHECK constraints repeat
 * them as a last defence.
 * Timestamps are millisecond UTC instants stored as 24-character text.
 */
@Entity
@Table(name = "books")
public class Book {

    /** Lowest valid rating. */
    public static final int MIN_RATING = 1;
    /** Highest valid rating. */
    public static final int MAX_RATING = 5;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "author", nullable = false)
    private String author;

    @Column(name = "status", nullable = false)
    @Convert(converter = BookStatus.WireConverter.class)
    private BookStatus status;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "date_added", nullable = false, updatable = false)
    @Convert(converter = InstantConverter.class)
    private Instant dateAdded;

    @Column(name = "date_completed")
    @Convert(converter = InstantConverter.class)
    private Instant dateCompleted;

    /** Required by JPA. */
    protected Book() {
    }

    /**
     * Creates a book with every stored member.
     *
     * @param id the lower-case UUID text
     * @param title the stored title
     * @param author the stored author
     * @param status the reading status
     * @param rating the rating, or null
     * @param dateAdded the creation instant
     * @param dateCompleted the completion instant, or null
     */
    public Book(String id, String title, String author, BookStatus status, Integer rating,
            Instant dateAdded, Instant dateCompleted) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.status = status;
        this.rating = rating;
        this.dateAdded = dateAdded;
        this.dateCompleted = dateCompleted;
    }

    /**
     * Returns the id.
     *
     * @return the lower-case UUID text
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the title.
     *
     * @return the stored title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the author.
     *
     * @return the stored author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Returns the status.
     *
     * @return the reading status
     */
    public BookStatus getStatus() {
        return status;
    }

    /**
     * Returns the rating.
     *
     * @return the rating, or empty when the book is not rated
     */
    public Optional<Integer> getRating() {
        return Optional.ofNullable(rating);
    }

    /**
     * Returns the creation instant.
     *
     * @return the instant the book was added
     */
    public Instant getDateAdded() {
        return dateAdded;
    }

    /**
     * Returns the completion instant.
     *
     * @return the instant the book was completed, or empty when it is not completed
     */
    public Optional<Instant> getDateCompleted() {
        return Optional.ofNullable(dateCompleted);
    }

    /**
     * Changes the status (BR-004, BR-008). Entering {@code completed} from another status sets the
     * completion date to the clock time, truncated to milliseconds; leaving {@code completed}
     * clears the rating and the completion date together; a change to the same status changes
     * nothing.
     *
     * @param target the new status
     * @param clock the application clock, read only when the book enters {@code completed}
     */
    public void changeStatus(BookStatus target, Clock clock) {
        if (target == status) {
            return;
        }
        if (target == BookStatus.COMPLETED) {
            dateCompleted = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        } else {
            rating = null;
            dateCompleted = null;
        }
        status = target;
    }

    /**
     * Assigns or replaces the rating (BR-007, INV-1, INV-2).
     *
     * @param value the rating, 1 to 5
     * @throws IllegalStateException when the book is not completed; the service maps it to
     *     {@code RATING_NOT_ALLOWED}
     * @throws IllegalArgumentException when the value is outside 1 to 5 (the service has judged it
     *     already, so this is a defence only)
     */
    public void assignRating(int value) {
        if (status != BookStatus.COMPLETED) {
            throw new IllegalStateException("rating needs a completed book");
        }
        if (value < MIN_RATING || value > MAX_RATING) {
            throw new IllegalArgumentException("rating out of range");
        }
        rating = value;
    }

    /** Removes the rating; the status and the completion date stay as they are. */
    public void clearRating() {
        rating = null;
    }

    /** Stores an instant as {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} (24 characters, UTC, milliseconds). */
    @Converter
    public static class InstantConverter implements AttributeConverter<Instant, String> {

        private static final DateTimeFormatter FORMAT =
                DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

        @Override
        public String convertToDatabaseColumn(Instant attribute) {
            return attribute == null ? null : FORMAT.format(attribute);
        }

        @Override
        public Instant convertToEntityAttribute(String dbData) {
            return dbData == null ? null : Instant.from(FORMAT.parse(dbData));
        }
    }
}
