package com.globant.globerbooks.service;

import com.globant.globerbooks.domain.Book;
import com.globant.globerbooks.domain.BookStatus;
import com.globant.globerbooks.dto.BookResponse;
import com.globant.globerbooks.dto.CreateBookRequest;
import com.globant.globerbooks.error.ApiException;
import com.globant.globerbooks.error.DuplicateBookException;
import com.globant.globerbooks.error.ErrorBody.FieldProblem;
import com.globant.globerbooks.error.ErrorCode;
import com.globant.globerbooks.repository.BookKeyRow;
import com.globant.globerbooks.repository.BookRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for books. Each public method is one transaction. The service repeats the text policy
 * and the status check of the request (defence in depth) and applies the capacity and duplicate rules
 * (BR-009, BR-003). This class never logs request data.
 */
@Service
public class BookService {

    /** Most books the library holds (BR-009). */
    static final int LIBRARY_LIMIT = 5000;

    private static final Comparator<BookKeyRow> EARLIEST = Comparator
            .comparing(BookKeyRow::dateAdded).thenComparing(BookKeyRow::id);

    private static final String STATUS_MESSAGE = "must be one of: unread, reading, completed";

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
     * Creates a book in one transaction: validation, then the count, then the duplicate scan, then the
     * insert. At 5,000 books the add fails with {@code LIBRARY_FULL} before any duplicate check, with or
     * without {@code allowDuplicate}; the override skips only the duplicate check (BR-003, BR-009).
     *
     * @param request the new book
     * @return the stored book
     * @throws BookValidationException when title, author or status is invalid
     * @throws ApiException {@code LIBRARY_FULL} when 5,000 books exist
     * @throws DuplicateBookException when the key matches a stored book and there is no override
     */
    @Transactional
    public BookResponse create(CreateBookRequest request) {
        BookStatus status = request.status() == null
                ? BookStatus.UNREAD : BookStatus.fromWire(request.status()).orElse(null);
        check(request, status);
        String title = TextPolicy.normalize(request.title());
        String author = TextPolicy.normalize(request.author());
        if (repository.count() >= LIBRARY_LIMIT) {
            throw new ApiException(ErrorCode.LIBRARY_FULL);
        }
        if (!Boolean.TRUE.equals(request.allowDuplicate())) {
            findEarliestMatch(title, author).ifPresent(match -> {
                throw new DuplicateBookException(repository.findById(match.id()).orElseThrow());
            });
        }
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

    /** One query for the key columns; the incoming key is computed once, stored rows from stored text. */
    private Optional<BookKeyRow> findEarliestMatch(String title, String author) {
        DuplicateKey incoming = DuplicateKey.of(title, author);
        return repository.findAllBy().stream()
                .filter(row -> DuplicateKey.textKey(row.author()).equals(incoming.authorKey())
                        && DuplicateKey.textKey(row.title()).equals(incoming.titleKey()))
                .min(EARLIEST);
    }

    private static void check(CreateBookRequest request, BookStatus status) {
        List<FieldProblem> problems = new ArrayList<>();
        addTextProblem(problems, "title", request.title(), TextPolicy.TITLE_MAX);
        addTextProblem(problems, "author", request.author(), TextPolicy.AUTHOR_MAX);
        if (status == null) {
            problems.add(new FieldProblem("status", STATUS_MESSAGE));
        }
        if (problems.isEmpty()) {
            return;
        }
        boolean onlyStatus = problems.size() == 1 && "status".equals(problems.get(0).field());
        throw new BookValidationException(
                onlyStatus ? ErrorCode.INVALID_STATUS : ErrorCode.VALIDATION_ERROR, problems);
    }

    private static void addTextProblem(List<FieldProblem> problems, String field, String raw, int max) {
        TextPolicy.check(raw, max).ifPresent(
                problem -> problems.add(new FieldProblem(field, BookValidationException.messageFor(problem, max))));
    }
}
