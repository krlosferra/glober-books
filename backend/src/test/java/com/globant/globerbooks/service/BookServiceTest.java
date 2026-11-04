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
import com.globant.globerbooks.dto.CreateBookRequest;
import com.globant.globerbooks.error.ErrorBody.FieldProblem;
import com.globant.globerbooks.error.ErrorCode;
import com.globant.globerbooks.repository.BookRepository;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

/** Unit tests of the thin add and list rules (spec 5.1, 5.2, US-001.1, US-002.1 to .3, BR-010). */
class BookServiceTest {

    private static final UUID FIXED_ID = UUID.fromString("3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b");
    private static final Instant NOW = Instant.parse("2026-02-19T10:00:00.123456789Z");

    private final BookRepository repository = mock(BookRepository.class);
    private final BookService service = new BookService(
            repository, Clock.fixed(NOW, ZoneOffset.UTC), () -> FIXED_ID);

    private BookResponse addSaved(CreateBookRequest request) {
        when(repository.save(any(Book.class))).thenAnswer(call -> call.getArgument(0));
        return service.add(request);
    }

    @Test
    void addsUnreadBookWithIdAndMillisecondDate() {
        BookResponse result = addSaved(new CreateBookRequest("Clean Code", "Robert Martin", null, null));

        assertThat(result.id()).isEqualTo("3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b");
        assertThat(result.status()).isEqualTo("unread");
        assertThat(result.rating()).isNull();
        assertThat(result.dateCompleted()).isNull();
        assertThat(result.dateAdded()).isEqualTo(Instant.parse("2026-02-19T10:00:00.123Z"));
    }

    @Test
    void addsCompletedBookWithCompletionDate() {
        BookResponse result = addSaved(new CreateBookRequest("A", "B", "completed", null));

        assertThat(result.status()).isEqualTo("completed");
        assertThat(result.dateCompleted()).isEqualTo(Instant.parse("2026-02-19T10:00:00.123Z"));
    }

    @Test
    void ignoresAllowDuplicate() {
        BookResponse result = addSaved(new CreateBookRequest("A", "B", "reading", true));

        assertThat(result.status()).isEqualTo("reading");
    }

    @Test
    void acceptsBoundaryLengthsMeasuredInCodePoints() {
        String title = "📘".repeat(500);
        String author = "a".repeat(200);

        BookResponse result = addSaved(new CreateBookRequest(title, author, null, null));

        assertThat(result.title()).isEqualTo(title);
        assertThat(result.author()).isEqualTo(author);
    }

    @Test
    void rejectsEmptyBlankAndNullTitleWithField() {
        for (String title : new String[] {"", "   ", null}) {
            BookValidationException failure = failureOf(new CreateBookRequest(title, "B", null, null));

            assertThat(failure.code()).isEqualTo(ErrorCode.VALIDATION_ERROR);
            assertThat(failure.problems()).containsExactly(
                    new FieldProblem("title", "must be between 1 and 500 characters"));
        }
    }

    @Test
    void rejectsTitleOf501AndAuthorOf201CodePoints() {
        BookValidationException failure = failureOf(
                new CreateBookRequest("a".repeat(501), "📘".repeat(201), null, null));

        assertThat(failure.code()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(failure.problems()).containsExactly(
                new FieldProblem("title", "must be between 1 and 500 characters"),
                new FieldProblem("author", "must be between 1 and 200 characters"));
    }

    @Test
    void reportsInvalidStatusAloneAsInvalidStatus() {
        BookValidationException failure = failureOf(new CreateBookRequest("A", "B", "Unread", null));

        assertThat(failure.code()).isEqualTo(ErrorCode.INVALID_STATUS);
        assertThat(failure.problems()).containsExactly(
                new FieldProblem("status", "must be one of: unread, reading, completed"));
    }

    @Test
    void reportsInvalidStatusWithAnotherFieldAsValidationError() {
        BookValidationException failure = failureOf(new CreateBookRequest("", "B", "bogus", null));

        assertThat(failure.code()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(failure.problems()).extracting(FieldProblem::field).containsExactly("title", "status");
    }

    @Test
    void storesNothingWhenInvalid() {
        failureOf(new CreateBookRequest("", "", null, null));

        verify(repository, never()).save(any());
    }

    @Test
    void storesTheBookOnceWithTheGeneratedId() {
        addSaved(new CreateBookRequest("A", "B", null, null));

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(FIXED_ID.toString());
        assertThat(captor.getValue().getStatus()).isEqualTo(BookStatus.UNREAD);
    }

    @Test
    void listsInRepositoryOrderWithEmptyAsEmptyList() {
        when(repository.findAllByOrderByDateAddedDescIdAsc()).thenReturn(List.of());

        assertThat(service.list()).isEmpty();
    }

    @Test
    void listMapsEveryBookKeepingOrder() {
        Book first = new Book("00000000-0000-4000-8000-00000000000b", "T2", "A", BookStatus.READING, null,
                Instant.parse("2026-02-19T10:00:00.000Z"), null);
        Book second = new Book("00000000-0000-4000-8000-00000000000a", "T1", "A", BookStatus.UNREAD, null,
                Instant.parse("2026-02-18T10:00:00.000Z"), null);
        when(repository.findAllByOrderByDateAddedDescIdAsc()).thenReturn(List.of(first, second));

        assertThat(service.list()).extracting(BookResponse::title).containsExactly("T2", "T1");
    }

    @Test
    void addIsTransactionalAndListIsReadOnlyTransactional() throws NoSuchMethodException {
        Method add = BookService.class.getMethod("add", CreateBookRequest.class);
        Method list = BookService.class.getMethod("list");

        assertThat(add.getAnnotation(Transactional.class).readOnly()).isFalse();
        assertThat(list.getAnnotation(Transactional.class).readOnly()).isTrue();
    }

    private BookValidationException failureOf(CreateBookRequest request) {
        BookValidationException[] holder = new BookValidationException[1];
        assertThatThrownBy(() -> service.add(request)).isInstanceOfSatisfying(
                BookValidationException.class, thrown -> holder[0] = thrown);
        return holder[0];
    }
}
