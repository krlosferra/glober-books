package com.globant.globerbooks.support;

import com.globant.globerbooks.domain.Book;
import com.globant.globerbooks.domain.BookStatus;
import com.globant.globerbooks.repository.BookRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fills the library for tests through the repository (never through a test-only endpoint). Books get
 * distinct ids, titles, authors and creation instants, so no two seeded books are duplicates of each
 * other. Every text is already trimmed and in NFC, as the service stores it.
 */
public final class LibrarySeeder {

    /** Title length of a maximum-size book, in code points (Appendix B). */
    public static final int MAX_TITLE = 500;
    /** Author length of a maximum-size book, in code points (Appendix B). */
    public static final int MAX_AUTHOR = 200;

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00.000Z");
    /** Non-ASCII on purpose: capital Cyrillic, a precomposed capital, a full-width letter, a supplementary symbol. */
    private static final int[] FILLER = {0x0416, 0x00C9, 0xFF37, 0x1F4D8, 0x0429};
    private static final int BATCH = 500;

    private LibrarySeeder() {
    }

    /**
     * Returns the id of the seeded book with this index.
     *
     * @param index the zero-based index
     * @return a lower-case version 4 UUID text
     */
    public static String idOf(int index) {
        return new UUID(0x4000L, 0x8000_0000_0000_0000L | index).toString();
    }

    /**
     * Returns the creation instant of the seeded book with this index (one millisecond apart).
     *
     * @param index the zero-based index
     * @return the instant
     */
    public static Instant addedAt(int index) {
        return BASE.plusMillis(index);
    }

    /**
     * Returns the title of the seeded short book with this index.
     *
     * @param index the zero-based index
     * @return the title
     */
    public static String shortTitle(int index) {
        return "Seed title " + index;
    }

    /**
     * Returns the title of the seeded maximum-size book with this index: exactly 500 code points.
     *
     * @param index the zero-based index
     * @return the title
     */
    public static String maxTitle(int index) {
        return padded("T" + index + " ", MAX_TITLE);
    }

    /**
     * Returns the author of the seeded maximum-size book with this index: exactly 200 code points.
     *
     * @param index the zero-based index
     * @return the author
     */
    public static String maxAuthor(int index) {
        return padded("A" + index + " ", MAX_AUTHOR);
    }

    /**
     * Stores books with short texts.
     *
     * @param repository the book store
     * @param count how many books
     */
    public static void seedShort(BookRepository repository, int count) {
        seed(repository, count, false);
    }

    /**
     * Stores books of maximum size (500-code-point titles, 200-code-point authors).
     *
     * @param repository the book store
     * @param count how many books
     */
    public static void seedMaxSize(BookRepository repository, int count) {
        seed(repository, count, true);
    }

    /**
     * Stores maximum-size books that all share one maximum-size author (200 code points, the author of
     * index 0) and have distinct maximum-size titles. This is the worst case of the duplicate scan: every
     * row matches on the author, so the title key is computed for every row.
     *
     * @param repository the book store
     * @param count how many books
     */
    public static void seedMaxSizeSharedAuthor(BookRepository repository, int count) {
        List<Book> batch = new ArrayList<>(BATCH);
        for (int index = 0; index < count; index++) {
            batch.add(new Book(idOf(index), maxTitle(index), maxAuthor(0), BookStatus.UNREAD, null,
                    addedAt(index), null));
            if (batch.size() == BATCH) {
                repository.saveAll(batch);
                batch.clear();
            }
        }
        repository.saveAll(batch);
    }

    private static void seed(BookRepository repository, int count, boolean maxSize) {
        List<Book> batch = new ArrayList<>(BATCH);
        for (int index = 0; index < count; index++) {
            String title = maxSize ? maxTitle(index) : shortTitle(index);
            String author = maxSize ? maxAuthor(index) : "Seed author " + index;
            batch.add(new Book(idOf(index), title, author, BookStatus.UNREAD, null, addedAt(index), null));
            if (batch.size() == BATCH) {
                repository.saveAll(batch);
                batch.clear();
            }
        }
        repository.saveAll(batch);
    }

    private static String padded(String prefix, int codePoints) {
        StringBuilder text = new StringBuilder(prefix);
        int length = prefix.codePointCount(0, prefix.length());
        for (int index = 0; length < codePoints; index++, length++) {
            text.appendCodePoint(FILLER[index % FILLER.length]);
        }
        return text.toString();
    }
}
