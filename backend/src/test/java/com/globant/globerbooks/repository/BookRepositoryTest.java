package com.globant.globerbooks.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.globant.globerbooks.domain.Book;
import com.globant.globerbooks.domain.BookStatus;
import com.globant.globerbooks.dto.BookResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.data.jpa.repository.Query;

/**
 * Proves the books table, its CHECK constraints (INV-1 to INV-4), Hibernate validation on the
 * STRICT table, timestamp and status round trips and the list order (BR-010) on a real SQLite
 * file. Expected values come from spec Section 4 and Section 5.0, not from the code under test.
 */
@DataJpaTest(properties = {
    "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.sql.init.mode=always"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookRepositoryTest {

    private static final String ID_A = "00000000-0000-4000-8000-00000000000a";
    private static final String ID_B = "00000000-0000-4000-8000-00000000000b";
    private static final String ID_C = "00000000-0000-4000-8000-00000000000c";
    private static final String STAMP = "2026-01-02T03:04:05.678Z";
    private static final String INSERT =
            "INSERT INTO books (id, title, author, status, rating, date_added, date_completed)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final Path DATABASE_DIRECTORY = createDirectory();

    @Autowired
    private BookRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE_DIRECTORY.resolve("books.db"));
    }

    private static Path createDirectory() {
        try {
            Path directory = Files.createTempDirectory("book-repository-test");
            directory.toFile().deleteOnExit();
            return directory;
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    @Test
    void hibernateValidatePassesAndStoredDdlEqualsNormalisedSchema() throws IOException {
        String schema = new String(
                getClass().getClassLoader().getResourceAsStream("schema.sql").readAllBytes(),
                StandardCharsets.UTF_8);
        String stored = jdbc.queryForObject(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'books'", String.class);

        assertThat(repository.count()).isZero();
        assertThat(normalise(stored)).isEqualTo(normalise(schema));
    }

    private static String normalise(String ddl) {
        return ddl.replace("IF NOT EXISTS ", "").replaceAll("\\s+", " ").replaceAll("\\s*;\\s*$", "").trim();
    }

    @Test
    void savedBookReturnsWithSameMillisecondTimestampsAndLowerCaseStatus() {
        Instant added = Instant.parse("2026-01-02T03:04:05.678Z");
        Instant completed = Instant.parse("2026-02-03T04:05:06.007Z");
        repository.saveAndFlush(new Book(ID_A, "Title", "Author", BookStatus.COMPLETED, 4, added, completed));
        repository.flush();

        Book found = repository.findById(ID_A).orElseThrow();

        assertThat(found.getDateAdded()).isEqualTo(added);
        assertThat(found.getDateCompleted()).contains(completed);
        assertThat(found.getStatus()).isEqualTo(BookStatus.COMPLETED);
        assertThat(found.getRating()).contains(4);
        assertThat(jdbc.queryForObject("SELECT status FROM books WHERE id = ?", String.class, ID_A))
                .isEqualTo("completed");
        assertThat(jdbc.queryForObject("SELECT date_added FROM books WHERE id = ?", String.class, ID_A))
                .isEqualTo("2026-01-02T03:04:05.678Z");
        assertThat(jdbc.queryForObject("SELECT date_completed FROM books WHERE id = ?", String.class, ID_A))
                .isEqualTo("2026-02-03T04:05:06.007Z");
    }

    @Test
    void unreadBookKeepsNullRatingAndNullCompletionDate() {
        repository.saveAndFlush(new Book(ID_A, "T", "A", BookStatus.UNREAD, null,
                Instant.parse("2026-01-01T00:00:00.000Z"), null));

        Book found = repository.findById(ID_A).orElseThrow();

        assertThat(found.getRating()).isEmpty();
        assertThat(found.getDateCompleted()).isEmpty();
        BookResponse response = BookResponse.from(found);
        assertThat(response.rating()).isNull();
        assertThat(response.dateCompleted()).isNull();
        assertThat(response.status()).isEqualTo("unread");
    }

    @Test
    void orderedQuerySortsByDateAddedDescendingThenIdAscendingIncludingTies() {
        Instant older = Instant.parse("2026-01-01T00:00:00.000Z");
        Instant newer = Instant.parse("2026-01-01T00:00:00.001Z");
        repository.saveAll(List.of(
                new Book(ID_C, "c", "x", BookStatus.UNREAD, null, newer, null),
                new Book(ID_A, "a", "x", BookStatus.UNREAD, null, older, null),
                new Book(ID_B, "b", "x", BookStatus.UNREAD, null, newer, null)));
        repository.flush();

        List<String> ids = repository.findAllByOrderByDateAddedDescIdAsc().stream().map(Book::getId).toList();

        assertThat(ids).containsExactly(ID_B, ID_C, ID_A);
    }

    @Test
    void statusFilterReturnsOnlyThatStatusInTheSameOrder() {
        Instant moment = Instant.parse("2026-01-01T00:00:00.000Z");
        repository.saveAll(List.of(
                new Book(ID_C, "c", "x", BookStatus.READING, null, moment, null),
                new Book(ID_A, "a", "x", BookStatus.UNREAD, null, moment, null),
                new Book(ID_B, "b", "x", BookStatus.READING, null, moment, null)));
        repository.flush();

        assertThat(repository.findByStatusOrderByDateAddedDescIdAsc(BookStatus.READING))
                .extracting(Book::getId).containsExactly(ID_B, ID_C);
        assertThat(repository.findByStatusOrderByDateAddedDescIdAsc(BookStatus.COMPLETED)).isEmpty();
    }

    @Test
    void titleOfFiveHundredEmojiPassesAndFiveHundredAndOneIsRejected() {
        String emoji = "😀";

        insert(ID_A, emoji.repeat(500), "A", "unread", null, STAMP, null);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM books", Integer.class)).isEqualTo(1);
        assertRejected(ID_B, emoji.repeat(501), "A", "unread", null, STAMP, null);
    }

    @Test
    void authorOfTwoHundredEmojiPassesAndTwoHundredAndOneIsRejected() {
        String emoji = "😀";

        insert(ID_A, "T", emoji.repeat(200), "unread", null, STAMP, null);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM books", Integer.class)).isEqualTo(1);
        assertRejected(ID_B, "T", emoji.repeat(201), "unread", null, STAMP, null);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validRows")
    void acceptsBoundaryRowsThatTheSpecAllows(String label, Object[] row) {
        insert(row);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM books", Integer.class)).isEqualTo(1);
    }

    static Stream<Arguments> validRows() {
        return Stream.of(
            row("title length 1, author length 1", ID_A, "T", "A", "unread", null, STAMP, null),
            row("title length 500", ID_A, "t".repeat(500), "A", "reading", null, STAMP, null),
            row("author length 200", ID_A, "T", "a".repeat(200), "reading", null, STAMP, null),
            row("rating 1 completed", ID_A, "T", "A", "completed", 1, STAMP, STAMP),
            row("rating 5 completed", ID_A, "T", "A", "completed", 5, STAMP, STAMP),
            row("completed without rating", ID_A, "T", "A", "completed", null, STAMP, STAMP));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRows")
    void databaseRejectsRowsThatBreakAnInvariant(String label, Object[] row) {
        assertThatThrownBy(() -> insert(row)).isInstanceOf(DataAccessException.class);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM books", Integer.class)).isZero();
    }

    static Stream<Arguments> invalidRows() {
        String good = ID_A;
        return Stream.of(
            row("id 35 characters", "00000000-0000-4000-8000-00000000000", "T", "A", "unread", null, STAMP, null),
            row("id 37 characters", "00000000-0000-4000-8000-00000000000aa", "T", "A", "unread", null, STAMP, null),
            row("id upper case", "00000000-0000-4000-8000-00000000000A", "T", "A", "unread", null, STAMP, null),
            row("id non-hex character", "00000000-0000-4000-8000-00000000000g", "T", "A", "unread", null, STAMP, null),
            row("id hyphen misplaced", "0000000-00000-4000-8000-00000000000a", "T", "A", "unread", null, STAMP, null),
            row("id without hyphens", "0000000000004000800000000000000a0000", "T", "A", "unread", null, STAMP, null),
            row("empty id", "", "T", "A", "unread", null, STAMP, null),
            row("title length 0", good, "", "A", "unread", null, STAMP, null),
            row("title length 501", good, "t".repeat(501), "A", "unread", null, STAMP, null),
            row("author length 0", good, "T", "", "unread", null, STAMP, null),
            row("author length 201", good, "T", "a".repeat(201), "unread", null, STAMP, null),
            row("status upper case", good, "T", "A", "READING", null, STAMP, null),
            row("status outside the set", good, "T", "A", "done", null, STAMP, null),
            row("rating 0", good, "T", "A", "completed", 0, STAMP, STAMP),
            row("rating 6", good, "T", "A", "completed", 6, STAMP, STAMP),
            row("rating non-numeric text", good, "T", "A", "completed", "abc", STAMP, STAMP),
            row("rating on unread", good, "T", "A", "unread", 3, STAMP, null),
            row("rating on reading", good, "T", "A", "reading", 3, STAMP, null),
            row("date_completed on unread", good, "T", "A", "unread", null, STAMP, STAMP),
            row("date_completed on reading", good, "T", "A", "reading", null, STAMP, STAMP),
            row("completed without date_completed", good, "T", "A", "completed", null, STAMP, null),
            row("date_added without milliseconds", good, "T", "A", "unread", null, "2026-01-02T03:04:05Z", null),
            row("date_added with space separator", good, "T", "A", "unread", null, "2026-01-02 03:04:05.678Z", null),
            row("date_added lower-case z", good, "T", "A", "unread", null, "2026-01-02T03:04:05.678z", null),
            row("date_added with offset", good, "T", "A", "unread", null, "2026-01-02T03:04:05.678+0", null),
            row("date_added with letters in digits", good, "T", "A", "unread", null, "2026-01-02T03:04:05.67aZ", null),
            row("date_added empty", good, "T", "A", "unread", null, "", null),
            row("date_completed malformed", good, "T", "A", "completed", null, STAMP, "2026-01-02T03:04:05Z"),
            row("date_completed 25 characters", good, "T", "A", "completed", null, STAMP, STAMP + "Z"));
    }

    @Test
    void bookHasNoPublicSetters() {
        List<String> setters = Arrays.stream(Book.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .toList();

        assertThat(setters).isEmpty();
    }

    @Test
    void repositoryDeclaresNoNativeOrHandWrittenQueries() {
        long annotated = Arrays.stream(BookRepository.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Query.class))
                .count();

        assertThat(annotated).isZero();
    }

    @Test
    void keyRowsCarryTheFourScanColumnsOfEveryBookInOneQuery() {
        insert(ID_A, "Title A", "Author A", "completed", 5, STAMP, STAMP);
        insert(ID_B, "Title B", "Author B", "unread", null, STAMP, null);

        List<BookKeyRow> rows = repository.findAllBy();

        assertThat(rows).extracting(BookKeyRow::id, BookKeyRow::title, BookKeyRow::author, BookKeyRow::dateAdded)
                .containsExactlyInAnyOrder(
                        tuple(ID_A, "Title A", "Author A", Instant.parse(STAMP)),
                        tuple(ID_B, "Title B", "Author B", Instant.parse(STAMP)));
        assertThat(rows.get(0).toString()).doesNotContain("Title");
    }

    @Test
    void bookResponseHasExactlyTheSevenMembersOfSection5Dot0InOrder() {
        List<String> names = Arrays.stream(BookResponse.class.getRecordComponents())
                .map(RecordComponent::getName).toList();

        assertThat(names).containsExactly(
                "id", "title", "author", "status", "rating", "dateAdded", "dateCompleted");
    }

    private void assertRejected(String id, String title, String author, String status, Object rating,
            String added, String completed) {
        assertThatThrownBy(() -> insert(id, title, author, status, rating, added, completed))
                .isInstanceOf(DataAccessException.class);
    }

    private void insert(String id, String title, String author, String status, Object rating,
            String added, String completed) {
        insert(new Object[] {id, title, author, status, rating, added, completed});
    }

    private void insert(Object[] row) {
        jdbc.update(INSERT, row);
    }

    private static Arguments row(String label, Object... values) {
        return Arguments.of(label, values);
    }
}
