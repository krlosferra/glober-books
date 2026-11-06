package com.globant.globerbooks.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests of the status and rating transitions (BR-004, BR-007, BR-008, INV-1, INV-2, INV-4,
 * US-006.3 to .5). Expected values come from the spec text, not from the code.
 */
class BookTest {

    private static final Instant ADDED = Instant.parse("2026-01-01T00:00:00.000Z");
    private static final Instant NOW = Instant.parse("2026-03-04T05:06:07.891Z");
    private static final Clock CLOCK = Clock.fixed(NOW.plusNanos(123_456), ZoneOffset.UTC);
    private static final Clock FAIL_CLOCK = new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            throw new AssertionError("the clock must not be read");
        }
    };

    private static Book book(BookStatus status, Integer rating, Instant completed) {
        return new Book("00000000-0000-4000-8000-000000000001", "T", "A", status, rating, ADDED, completed);
    }

    @ParameterizedTest
    @EnumSource(value = BookStatus.class, names = {"UNREAD", "READING"})
    void enteringCompletedSetsTheCompletionDateFromTheClockAndLeavesRatingNull(BookStatus from) {
        Book book = book(from, null, null);

        book.changeStatus(BookStatus.COMPLETED, CLOCK);

        assertThat(book.getStatus()).isEqualTo(BookStatus.COMPLETED);
        assertThat(book.getDateCompleted()).contains(NOW);
        assertThat(book.getRating()).isEmpty();
        assertThat(book.getDateAdded()).isEqualTo(ADDED);
    }

    @ParameterizedTest
    @EnumSource(value = BookStatus.class, names = {"UNREAD", "READING"})
    void leavingCompletedClearsRatingAndCompletionDateTogether(BookStatus target) {
        Book book = book(BookStatus.COMPLETED, 4, ADDED);

        book.changeStatus(target, FAIL_CLOCK);

        assertThat(book.getStatus()).isEqualTo(target);
        assertThat(book.getRating()).isEmpty();
        assertThat(book.getDateCompleted()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = BookStatus.class, names = {"UNREAD", "READING"})
    void leavingCompletedWithoutRatingClearsTheCompletionDate(BookStatus target) {
        Book book = book(BookStatus.COMPLETED, null, ADDED);

        book.changeStatus(target, FAIL_CLOCK);

        assertThat(book.getDateCompleted()).isEmpty();
        assertThat(book.getRating()).isEmpty();
    }

    @Test
    void returningToCompletedGivesTheNewTimeAndNoRating() {
        Book book = book(BookStatus.COMPLETED, 5, ADDED);
        book.changeStatus(BookStatus.READING, FAIL_CLOCK);

        book.changeStatus(BookStatus.COMPLETED, CLOCK);

        assertThat(book.getDateCompleted()).contains(NOW);
        assertThat(book.getRating()).isEmpty();
    }

    @Test
    void completedToCompletedChangesNothing() {
        Book book = book(BookStatus.COMPLETED, 3, ADDED);

        book.changeStatus(BookStatus.COMPLETED, FAIL_CLOCK);

        assertThat(book.getRating()).contains(3);
        assertThat(book.getDateCompleted()).contains(ADDED);
        assertThat(book.getStatus()).isEqualTo(BookStatus.COMPLETED);
    }

    @Test
    void unreadAndReadingSwitchKeepRatingAndDateNull() {
        Book book = book(BookStatus.UNREAD, null, null);

        book.changeStatus(BookStatus.READING, FAIL_CLOCK);
        assertThat(book.getStatus()).isEqualTo(BookStatus.READING);
        book.changeStatus(BookStatus.READING, FAIL_CLOCK);
        book.changeStatus(BookStatus.UNREAD, FAIL_CLOCK);

        assertThat(book.getStatus()).isEqualTo(BookStatus.UNREAD);
        assertThat(book.getRating()).isEmpty();
        assertThat(book.getDateCompleted()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void completedBookTakesEachRatingAndReplacesIt(int value) {
        Book book = book(BookStatus.COMPLETED, 2, ADDED);

        book.assignRating(value);

        assertThat(book.getRating()).contains(value);
        assertThat(book.getDateCompleted()).contains(ADDED);
    }

    @ParameterizedTest
    @EnumSource(value = BookStatus.class, names = {"UNREAD", "READING"})
    void bookThatIsNotCompletedRefusesARatingAndStaysUnchanged(BookStatus status) {
        Book book = book(status, null, null);

        assertThatThrownBy(() -> book.assignRating(4)).isInstanceOf(IllegalStateException.class);

        assertThat(book.getRating()).isEmpty();
        assertThat(book.getStatus()).isEqualTo(status);
        assertThat(book.getDateCompleted()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 6, 100})
    void ratingOutsideOneToFiveIsRefusedAndTheOldOneStays(int value) {
        Book book = book(BookStatus.COMPLETED, 3, ADDED);

        assertThatThrownBy(() -> book.assignRating(value)).isInstanceOf(IllegalArgumentException.class);

        assertThat(book.getRating()).contains(3);
    }

    @Test
    void clearingTheRatingKeepsStatusAndCompletionDate() {
        Book book = book(BookStatus.COMPLETED, 5, ADDED);

        book.clearRating();

        assertThat(book.getRating()).isEmpty();
        assertThat(book.getStatus()).isEqualTo(BookStatus.COMPLETED);
        assertThat(book.getDateCompleted()).contains(ADDED);
    }

    @Test
    void clearingAnAbsentRatingIsHarmless() {
        Book book = book(BookStatus.UNREAD, null, null);

        book.clearRating();

        assertThat(book.getRating()).isEmpty();
    }
}
