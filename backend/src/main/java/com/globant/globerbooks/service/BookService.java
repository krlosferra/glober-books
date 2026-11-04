package com.globant.globerbooks.service;

import com.globant.globerbooks.domain.Book;
import com.globant.globerbooks.domain.BookStatus;
import com.globant.globerbooks.dto.BookResponse;
import com.globant.globerbooks.dto.CreateBookRequest;
import com.globant.globerbooks.error.ErrorBody.FieldProblem;
import com.globant.globerbooks.error.ErrorCode;
import com.globant.globerbooks.repository.BookRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for books. Each public method is one transaction. Task 07 holds only the thin checks
 * of title, author and status; the text policy (Tasks 18 to 20) and the duplicate and capacity
 * rules (Task 23a) come later. This class never logs request data.
 */
@Service
public class BookService {

    static final int TITLE_MAX = 500;
    static final int AUTHOR_MAX = 200;

    private final BookRepository repository;
    private final Clock clock;
    private final Supplier<UUID> idSupplier;

    /**
     * Creates the service.
     *
     * @param repository the book store
     * @param clock the application clock
     * @param idSupplier the source of version 4 ids
     */
    public BookService(BookRepository repository, Clock clock, Supplier<UUID> idSupplier) {
        this.repository = repository;
        this.clock = clock;
        this.idSupplier = idSupplier;
    }

    /**
     * Adds a book in one transaction. {@code allowDuplicate} has no effect yet.
     *
     * @param request the new book
     * @return the stored book
     * @throws BookValidationException when title, author or status is invalid
     */
    @Transactional
    public BookResponse add(CreateBookRequest request) {
        String title = request.title() == null ? "" : request.title().strip();
        String author = request.author() == null ? "" : request.author().strip();
        BookStatus status = request.status() == null
                ? BookStatus.UNREAD : BookStatus.fromWire(request.status()).orElse(null);
        check(title, author, status);
        Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        Instant completed = status == BookStatus.COMPLETED ? now : null;
        Book book = new Book(idSupplier.get().toString(), title, author, status, null, now, completed);
        return BookResponse.from(repository.save(book));
    }

    /**
     * Lists all books, newest first, ties by id. Read-only; the list is returned after the
     * transaction ends.
     *
     * @return the books as responses, possibly empty
     */
    @Transactional(readOnly = true)
    public List<BookResponse> list() {
        return repository.findAllByOrderByDateAddedDescIdAsc().stream().map(BookResponse::from).toList();
    }

    private static void check(String title, String author, BookStatus status) {
        List<FieldProblem> problems = new ArrayList<>();
        if (outOfRange(title, TITLE_MAX)) {
            problems.add(new FieldProblem("title", "must be between 1 and 500 characters"));
        }
        if (outOfRange(author, AUTHOR_MAX)) {
            problems.add(new FieldProblem("author", "must be between 1 and 200 characters"));
        }
        if (status == null) {
            problems.add(new FieldProblem("status", "must be one of: unread, reading, completed"));
        }
        if (problems.isEmpty()) {
            return;
        }
        boolean onlyStatus = problems.size() == 1 && "status".equals(problems.get(0).field());
        throw new BookValidationException(
                onlyStatus ? ErrorCode.INVALID_STATUS : ErrorCode.VALIDATION_ERROR, problems);
    }

    private static boolean outOfRange(String text, int max) {
        int length = text.codePointCount(0, text.length());
        return length < 1 || length > max;
    }
}
