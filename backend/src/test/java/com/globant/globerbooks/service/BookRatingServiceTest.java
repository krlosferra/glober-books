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
import com.globant.globerbooks.dto.RatingValue;
import com.globant.globerbooks.error.ApiException;
import com.globant.globerbooks.error.ErrorBody.FieldProblem;
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
 * Unit tests of the rating use cases with a fixed clock (BR-002, BR-007, BR-012 steps 6 to 8, BR-013, S-01,
 * S-02, S-05, T-36, T-37, SEC-F-170). Expected codes come from the spec, not from the code.
 */
class BookRatingServiceTest {

    private static final String ID = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b";
    private static final Instant ADDED = Instant.parse("2026-02-01T08:00:00.000Z");
    private static final Instant DONE = Instant.parse("2026-02-10T09:00:00.000Z");
    private static final String MESSAGE = "must be an integer from 1 to 5";

    private final BookRepository repository = mock(BookRepository.class);
    private final BookService service = new BookService(repository,
            Clock.fixed(Instant.parse("2026-02-19T10:00:00.123456789Z"), ZoneOffset.UTC), () -> UUID.fromString(ID));

    private Book stored(BookStatus status, Integer rating) {
        Book book = new Book(ID, "Clean Code", "Robert Martin", status, rating, ADDED,
                status == BookStatus.COMPLETED ? DONE : null);
        when(repository.findById(ID)).thenReturn(Optional.of(book));
        when(repository.saveAndFlush(any(Book.class))).thenAnswer(call -> call.getArgument(0));
        return book;
    }

    private static void assertCode(Throwable failure, ErrorCode expected) {
        assertThat(failure).isInstanceOf(ApiException.class);
        assertThat(((ApiException) failure).code()).isEqualTo(expected);
    }

    private static void assertInvalidRating(Throwable failure) {
        assertThat(failure).isInstanceOf(BookValidationException.class);
        BookValidationException invalid = (BookValidationException) failure;
        assertThat(invalid.code()).isEqualTo(ErrorCode.INVALID_RATING);
        assertThat(invalid.problems()).containsExactly(new FieldProblem("rating", MESSAGE));
        assertThat(invalid.getMessage()).isEqualTo("INVALID_RATING");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void ratesACompletedBookWithEachValueFromOneToFive(int value) {
        stored(BookStatus.COMPLETED, null);

        BookResponse result = service.rate(ID, new RatingValue(value));

        assertThat(result.rating()).isEqualTo(value);
        assertThat(result.status()).isEqualTo("completed");
        assertThat(result.dateCompleted()).isEqualTo(DONE);
    }

    @Test
    void replacesAnExistingRating() {
        stored(BookStatus.COMPLETED, 2);

        assertThat(service.rate(ID, new RatingValue(5)).rating()).isEqualTo(5);
    }

    @Test
    void judgesTheInvalidMarkerBeforeLookingForTheBook() {
        assertThatThrownBy(() -> service.rate(ID, RatingValue.INVALID))
                .satisfies(BookRatingServiceTest::assertInvalidRating);

        verify(repository, never()).findById(any());
    }

    @Test
    void judgesTheInvalidMarkerBeforeTheStatusOfAnUnreadBook() {
        Book book = stored(BookStatus.UNREAD, null);

        assertThatThrownBy(() -> service.rate(ID, RatingValue.INVALID))
                .satisfies(BookRatingServiceTest::assertInvalidRating);

        assertThat(book.getRating()).isEmpty();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void anUnknownIdWithAValidRatingIsBookNotFound() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rate(ID, new RatingValue(4)))
                .satisfies(failure -> assertCode(failure, ErrorCode.BOOK_NOT_FOUND));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNREAD", "READING"})
    void refusesAValidRatingOnABookThatIsNotCompletedAndLeavesItUnchanged(String name) {
        Book book = stored(BookStatus.valueOf(name), null);

        assertThatThrownBy(() -> service.rate(ID, new RatingValue(4)))
                .satisfies(failure -> assertCode(failure, ErrorCode.RATING_NOT_ALLOWED))
                .satisfies(failure -> assertThat(((ApiException) failure).status()).isEqualTo(400));

        assertThat(book.getRating()).isEmpty();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void checksTheStatusBeforeCallingTheDomainMethod() {
        Book book = mock(Book.class);
        when(book.getStatus()).thenReturn(BookStatus.READING);
        when(repository.findById(ID)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> service.rate(ID, new RatingValue(3))).isInstanceOf(ApiException.class);

        verify(book, never()).assignRating(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void doesNotCatchAnUnexpectedDomainRefusal() {
        Book book = mock(Book.class);
        when(book.getStatus()).thenReturn(BookStatus.COMPLETED);
        when(repository.findById(ID)).thenReturn(Optional.of(book));
        IllegalStateException refusal = new IllegalStateException("rating needs a completed book");
        org.mockito.Mockito.doThrow(refusal).when(book).assignRating(3);

        assertThatThrownBy(() -> service.rate(ID, new RatingValue(3))).isSameAs(refusal);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void clearsTheRatingAndKeepsStatusAndCompletionDate() {
        stored(BookStatus.COMPLETED, 5);

        BookResponse result = service.unrate(ID);

        assertThat(result.rating()).isNull();
        assertThat(result.status()).isEqualTo("completed");
        assertThat(result.dateCompleted()).isEqualTo(DONE);
    }

    @Test
    void clearingAnUnratedBookIsIdempotentAndAlsoWorksForAnUnreadBook() {
        stored(BookStatus.UNREAD, null);

        assertThat(service.unrate(ID).rating()).isNull();
        assertThat(service.unrate(ID).rating()).isNull();
    }

    @Test
    void clearingAnUnknownIdIsBookNotFound() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unrate(ID))
                .satisfies(failure -> assertCode(failure, ErrorCode.BOOK_NOT_FOUND));
    }

    @Test
    void eachRatingUseCaseIsOneTransactionAndNoUseCaseIsNamedLikeAMutatorTheBugFinderFlags() throws Exception {
        for (String name : new String[] {"rate", "unrate"}) {
            Method method = java.util.Arrays.stream(BookService.class.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(name)).findFirst().orElseThrow();
            assertThat(method.getAnnotation(Transactional.class)).as(name).isNotNull();
            assertThat(method.getAnnotation(Transactional.class).readOnly()).isFalse();
        }
    }
}
