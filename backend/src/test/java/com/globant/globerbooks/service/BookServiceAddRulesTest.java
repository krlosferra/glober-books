package com.globant.globerbooks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globant.globerbooks.domain.Book;
import com.globant.globerbooks.domain.BookStatus;
import com.globant.globerbooks.dto.BookResponse;
import com.globant.globerbooks.dto.CreateBookRequest;
import com.globant.globerbooks.error.ApiException;
import com.globant.globerbooks.error.DuplicateBookException;
import com.globant.globerbooks.error.ErrorCode;
import com.globant.globerbooks.repository.BookKeyRow;
import com.globant.globerbooks.repository.BookRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Unit tests of the capacity and duplicate rules of the add (BR-003, BR-009, US-004.1 to .9, C-01, C-02). */
class BookServiceAddRulesTest {

    private static final UUID FIXED_ID = UUID.fromString("3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b");
    private static final Instant NOW = Instant.parse("2026-02-19T10:00:00.123Z");

    private final BookRepository repository = mock(BookRepository.class);
    private final BookService service = new BookService(
            repository, Clock.fixed(NOW, ZoneOffset.UTC), () -> FIXED_ID);

    private BookResponse addSaved(CreateBookRequest request) {
        when(repository.save(any(Book.class))).thenAnswer(call -> call.getArgument(0));
        return service.create(request);
    }

    private static final String OLD_ID = "00000000-0000-4000-8000-000000000001";
    private static final String NEW_ID = "00000000-0000-4000-8000-000000000002";
    private static final Instant EARLY = Instant.parse("2026-01-01T00:00:00.000Z");

    private static BookKeyRow row(String id, String title, String author, Instant added) {
        return new BookKeyRow(id, title, author, added);
    }

    private static Book stored(String id, Instant added) {
        return new Book(id, "Clean Code", "Robert Martin", BookStatus.COMPLETED, 5, added, added);
    }

    private DuplicateBookException duplicateOf(CreateBookRequest request) {
        DuplicateBookException[] holder = new DuplicateBookException[1];
        assertThatThrownBy(() -> service.create(request)).isInstanceOfSatisfying(
                DuplicateBookException.class, thrown -> holder[0] = thrown);
        return holder[0];
    }

    @Test
    void rejectsWithLibraryFullAt5000BeforeAnyDuplicateCheckWithAndWithoutOverride() {
        when(repository.count()).thenReturn(5000L);
        for (Boolean override : new Boolean[] {null, false, true}) {
            assertThatThrownBy(() -> service.create(new CreateBookRequest("A", "B", null, override)))
                    .isInstanceOfSatisfying(ApiException.class,
                            thrown -> assertThat(thrown.code()).isEqualTo(ErrorCode.LIBRARY_FULL));
        }

        verify(repository, never()).findAllBy();
        verify(repository, never()).save(any());
    }

    @Test
    void addsTheBookAt4999AndCountsBeforeScanningBeforeInserting() {
        when(repository.count()).thenReturn(4999L);

        addSaved(new CreateBookRequest("A", "B", null, null));

        InOrder order = inOrder(repository);
        order.verify(repository).count();
        order.verify(repository).findAllBy();
        order.verify(repository).save(any(Book.class));
    }

    @Test
    void overrideSkipsOnlyTheScanAndStoresASeparateBook() {
        when(repository.findAllBy()).thenReturn(List.of(row(OLD_ID, "A", "B", EARLY)));

        BookResponse result = addSaved(new CreateBookRequest("a", "b", null, true));

        assertThat(result.id()).isEqualTo(FIXED_ID.toString());
        verify(repository).count();
        verify(repository, never()).findAllBy();
    }

    @Test
    void absentAndFalseOverrideRunTheScanOnceWithoutALoop() {
        addSaved(new CreateBookRequest("A", "B", null, null));
        addSaved(new CreateBookRequest("A", "B", null, false));

        verify(repository, times(2)).findAllBy();
        verify(repository, never()).findAll();
    }

    @Test
    void rejectsAMatchByKeyWithTheExistingBookAndStoresNothing() {
        when(repository.findAllBy()).thenReturn(List.of(
                row(OLD_ID, "other", "Robert Martin", EARLY), row(NEW_ID, "Clean Code", "Robert Martin", EARLY)));
        when(repository.findById(NEW_ID)).thenReturn(Optional.of(stored(NEW_ID, EARLY)));

        DuplicateBookException failure = duplicateOf(
                new CreateBookRequest(" clean   code ", "ROBERT MARTIN", "unread", null));

        assertThat(failure.code()).isEqualTo(ErrorCode.DUPLICATE_BOOK);
        assertThat(failure.existingBook().id()).isEqualTo(NEW_ID);
        verify(repository, never()).save(any());
    }

    @Test
    void reportsTheEarliestOfTwoMatchesByDateAdded() {
        Instant later = Instant.parse("2026-03-01T00:00:00.000Z");
        when(repository.findAllBy()).thenReturn(List.of(
                row(OLD_ID, "Clean Code", "Robert Martin", later),
                row(NEW_ID, "clean code", "robert martin", EARLY)));
        when(repository.findById(NEW_ID)).thenReturn(Optional.of(stored(NEW_ID, EARLY)));

        assertThat(duplicateOf(new CreateBookRequest("Clean Code", "Robert Martin", null, null))
                .existingBook().id()).isEqualTo(NEW_ID);
    }

    @Test
    void breaksATieOnTheMillisecondByTheLowestIdTextWhateverTheRowOrder() {
        when(repository.findById(OLD_ID)).thenReturn(Optional.of(stored(OLD_ID, EARLY)));
        for (List<BookKeyRow> rows : List.of(
                List.of(row(NEW_ID, "Clean Code", "RM", EARLY), row(OLD_ID, "Clean Code", "RM", EARLY)),
                List.of(row(OLD_ID, "Clean Code", "RM", EARLY), row(NEW_ID, "Clean Code", "RM", EARLY)))) {
            when(repository.findAllBy()).thenReturn(rows);

            assertThat(duplicateOf(new CreateBookRequest("Clean Code", "RM", null, null))
                    .existingBook().id()).isEqualTo(OLD_ID);
        }
    }

    @Test
    void sharpSAndDoubleSDoNotMatchSoBothStore() {
        when(repository.findAllBy()).thenReturn(List.of(row(OLD_ID, "Stra\u00DFe", "A", EARLY)));

        BookResponse result = addSaved(new CreateBookRequest("STRASSE", "A", null, null));

        assertThat(result.title()).isEqualTo("STRASSE");
    }
}
