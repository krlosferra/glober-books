package com.globant.globerbooks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globant.globerbooks.domain.Book;
import com.globant.globerbooks.domain.BookStatus;
import com.globant.globerbooks.dto.BookResponse;
import com.globant.globerbooks.error.ApiException;
import com.globant.globerbooks.error.ErrorCode;
import com.globant.globerbooks.repository.BookRepository;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unit tests of get, delete and the status change with a fixed clock (BR-004, BR-008, DEC-023, S-04, S-06,
 * S-11, T-38). The expected dates are the fixed clock value truncated to milliseconds.
 */
class BookStatusServiceTest {

    private static final String ID = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b";
    private static final Instant ADDED = Instant.parse("2026-02-01T08:00:00.000Z");
    private static final Instant COMPLETED_EARLIER = Instant.parse("2026-02-10T09:00:00.000Z");
    private static final Instant NOW = Instant.parse("2026-02-19T10:00:00.123456789Z");
    private static final Instant NOW_MILLIS = Instant.parse("2026-02-19T10:00:00.123Z");

    private final BookRepository repository = mock(BookRepository.class);
    private final BookService service = new BookService(
            repository, Clock.fixed(NOW, ZoneOffset.UTC), () -> UUID.fromString(ID));

    private Book stored(BookStatus status, Integer rating, Instant completed) {
        Book book = new Book(ID, "Clean Code", "Robert Martin", status, rating, ADDED, completed);
        when(repository.findById(ID)).thenReturn(Optional.of(book));
        when(repository.saveAndFlush(any(Book.class))).thenAnswer(call -> call.getArgument(0));
        return book;
    }

    @Test
    void entersCompletedWithTheClockValueAndNoRating() {
        stored(BookStatus.READING, null, null);

        BookResponse result = service.changeStatus(ID, "completed");

        assertThat(result.status()).isEqualTo("completed");
        assertThat(result.dateCompleted()).isEqualTo(NOW_MILLIS);
        assertThat(result.rating()).isNull();
        assertThat(result.dateAdded()).isEqualTo(ADDED);
    }

    @Test
    void leavingCompletedClearsRatingAndCompletionDateWithTheStatusInOneSave() {
        Book book = stored(BookStatus.COMPLETED, 4, COMPLETED_EARLIER);

        BookResponse result = service.changeStatus(ID, "reading");

        assertThat(result.status()).isEqualTo("reading");
        assertThat(result.rating()).isNull();
        assertThat(result.dateCompleted()).isNull();
        assertThat(book.getStatus()).isEqualTo(BookStatus.READING);
        verify(repository).saveAndFlush(book);
    }

    @Test
    void leavingCompletedForUnreadClearsRatingAndCompletionDate() {
        stored(BookStatus.COMPLETED, 5, COMPLETED_EARLIER);

        BookResponse result = service.changeStatus(ID, "unread");

        assertThat(result.status()).isEqualTo("unread");
        assertThat(result.rating()).isNull();
        assertThat(result.dateCompleted()).isNull();
    }

    @Test
    void completedToCompletedKeepsRatingAndTheOriginalCompletionDate() {
        stored(BookStatus.COMPLETED, 3, COMPLETED_EARLIER);

        BookResponse result = service.changeStatus(ID, "completed");

        assertThat(result.status()).isEqualTo("completed");
        assertThat(result.rating()).isEqualTo(3);
        assertThat(result.dateCompleted()).isEqualTo(COMPLETED_EARLIER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unread", "reading"})
    void sameStateChangeChangesNothing(String status) {
        stored(BookStatus.fromWire(status).orElseThrow(), null, null);

        BookResponse result = service.changeStatus(ID, status);

        assertThat(result).isEqualTo(new BookResponse(ID, "Clean Code", "Robert Martin", status, null, ADDED, null));
    }

    @Test
    void unreadToReadingKeepsCompletionFieldsNull() {
        stored(BookStatus.UNREAD, null, null);

        BookResponse result = service.changeStatus(ID, "reading");

        assertThat(result.status()).isEqualTo("reading");
        assertThat(result.dateCompleted()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Completed", "", "COMPLETED", " reading", "done"})
    void judgesTheStatusBeforeLookingForTheBook(String status) {
        assertThatThrownBy(() -> service.changeStatus(ID, status))
                .isInstanceOfSatisfying(BookValidationException.class, failure -> {
                    assertThat(failure.code()).isEqualTo(ErrorCode.INVALID_STATUS);
                    assertThat(failure.problems()).hasSize(1);
                    assertThat(failure.problems().get(0).field()).isEqualTo("status");
                });

        verify(repository, never()).findById(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void unknownBookGivesBookNotFoundAndSavesNothing() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(ID, "reading"))
                .isInstanceOfSatisfying(ApiException.class,
                        failure -> assertThat(failure.code()).isEqualTo(ErrorCode.BOOK_NOT_FOUND));

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void getReturnsTheStoredBook() {
        stored(BookStatus.COMPLETED, 4, COMPLETED_EARLIER);

        BookResponse result = service.get(ID);

        assertThat(result).isEqualTo(new BookResponse(
                ID, "Clean Code", "Robert Martin", "completed", 4, ADDED, COMPLETED_EARLIER));
    }

    @Test
    void getOfAnUnknownIdGivesBookNotFound() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(ID)).isInstanceOfSatisfying(ApiException.class,
                failure -> assertThat(failure.code()).isEqualTo(ErrorCode.BOOK_NOT_FOUND));
    }

    @Test
    void deleteRemovesTheBookAndReturnsItAsItWas() {
        Book book = stored(BookStatus.COMPLETED, 5, COMPLETED_EARLIER);

        BookResponse result = service.delete(ID);

        assertThat(result).isEqualTo(new BookResponse(
                ID, "Clean Code", "Robert Martin", "completed", 5, ADDED, COMPLETED_EARLIER));
        verify(repository).delete(book);
    }

    @Test
    void deleteOfAnUnknownIdGivesBookNotFoundAndDeletesNothing() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(ID)).isInstanceOfSatisfying(ApiException.class,
                failure -> assertThat(failure.code()).isEqualTo(ErrorCode.BOOK_NOT_FOUND));

        verify(repository, never()).delete(any(Book.class));
    }

    @Test
    void everyUseCaseRunsInOneTransactionAndReadsAreReadOnly() throws NoSuchMethodException {
        Method change = BookService.class.getMethod("changeStatus", String.class, String.class);
        Method remove = BookService.class.getMethod("delete", String.class);
        Method read = BookService.class.getMethod("get", String.class);

        assertThat(change.getAnnotation(Transactional.class).readOnly()).isFalse();
        assertThat(remove.getAnnotation(Transactional.class).readOnly()).isFalse();
        assertThat(read.getAnnotation(Transactional.class).readOnly()).isTrue();
    }
}
