package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globant.globerbooks.config.sqlite.StoredLibraryValidator.Reason;
import com.globant.globerbooks.support.StoredLibraryFixtures;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Task 34b, criteria 1 and 2: the row judgment (formats, limits in code points, INV-1 to INV-4), the fixed
 * reasons and the DDL normalisation, on values taken from the spec (Section 4, US-009.5, SEC-F-068).
 */
class StoredLibraryRowCheckTest {

    private static final String MARK = StoredLibraryFixtures.MARKER;

    @Test
    void rowCheckAcceptsTheBoundaries() {
        String longTitle = "T".repeat(500);
        String emojiTitle = "📘".repeat(500);
        String longAuthor = "A".repeat(200);
        String id = StoredLibraryFixtures.id(1);
        String date = StoredLibraryFixtures.DATE;

        assertThatCode(() -> {
            StoredLibraryValidator.requireRow(id, "T", "A", "unread", null, date, null);
            StoredLibraryValidator.requireRow(id, longTitle, longAuthor, "reading", null, date, null);
            StoredLibraryValidator.requireRow(id, emojiTitle, "A", "unread", null, date, null);
            StoredLibraryValidator.requireRow(id, "T", "A", "completed", 1, date, date);
            StoredLibraryValidator.requireRow(id, "T", "A", "completed", 5, date, date);
            StoredLibraryValidator.requireRow(id, "T", "A", "completed", null, date, date);
        }).doesNotThrowAnyException();
    }

    @Test
    void rowCheckRefusesEveryFormatLimitAndInvariantViolation() {
        String id = StoredLibraryFixtures.id(1);
        String date = StoredLibraryFixtures.DATE;
        Runnable[] violations = {
            () -> StoredLibraryValidator.requireRow(id, "", "A", "unread", null, date, null),
            () -> StoredLibraryValidator.requireRow(id, "T".repeat(501), "A", "unread", null, date, null),
            () -> StoredLibraryValidator.requireRow(id, "📘".repeat(501), "A", "unread", null, date, null),
            () -> StoredLibraryValidator.requireRow(id, "T", "", "unread", null, date, null),
            () -> StoredLibraryValidator.requireRow(id, "T", "A".repeat(201), "unread", null, date, null),
            () -> StoredLibraryValidator.requireRow(id, "a\0b", "A", "unread", null, date, null),
            () -> StoredLibraryValidator.requireRow(id, "T", "a\0b", "unread", null, date, null),
            () -> StoredLibraryValidator.requireRow(id, "T", "A", "archived", null, date, null),
            () -> StoredLibraryValidator.requireRow(id, "T", "A", "Unread", null, date, null),
            () -> StoredLibraryValidator.requireRow(id, "T", "A", "unread", 3, date, null),
            () -> StoredLibraryValidator.requireRow(id, "T", "A", "reading", 1, date, null),
            () -> StoredLibraryValidator.requireRow(id, "T", "A", "completed", 0, date, date),
            () -> StoredLibraryValidator.requireRow(id, "T", "A", "completed", 6, date, date),
            () -> StoredLibraryValidator.requireRow(id, "T", "A", "completed", 3, date, null),
            () -> StoredLibraryValidator.requireRow(id, "T", "A", "unread", null, date, date),
            () -> StoredLibraryValidator.requireRow(id, "T", "A", "reading", null, date, date),
            () -> StoredLibraryValidator.requireRow("0000000A-0000-4000-8000-00000000000A", "T", "A", "unread", null,
                    date, null),
            () -> StoredLibraryValidator.requireRow(id.substring(1), "T", "A", "unread", null, date, null),
            () -> StoredLibraryValidator.requireRow(id.replace('-', 'g'), "T", "A", "unread", null, date, null),
            () -> StoredLibraryValidator.requireRow(id, "T", "A", "unread", null, "2026-01-02", null),
            () -> StoredLibraryValidator.requireRow(id, "T", "A", "unread", null, "2026-01-02T03:04:05Z", null),
            () -> StoredLibraryValidator.requireRow(id, "T", "A", "completed", 3, date, "2026-02-30T03:04:05.678Z"),
        };

        for (Runnable violation : violations) {
            assertThatThrownBy(violation::run).isInstanceOfSatisfying(StoredLibraryValidator.Refusal.class,
                    refusal -> assertThat(refusal.reason()).isEqualTo(Reason.INVALID_ROW));
        }
    }

    @Test
    void everyReasonHasItsOwnFixedTextWithoutPathOrData() {
        Set<String> texts = new HashSet<>();
        for (Reason reason : Reason.values()) {
            texts.add(reason.text());
            assertThat(reason.text()).startsWith("Stored library: ").doesNotContain("/").doesNotContain(MARK)
                    .doesNotContainPattern("\\d");
        }

        assertThat(texts).hasSize(Reason.values().length);
    }

    @Test
    void normaliseDropsCommentsIfNotExistsAndWhiteSpaceRuns() {
        String stored = "CREATE TABLE books (a TEXT)";
        String schema = "CREATE TABLE IF NOT EXISTS   books /* note */ (a TEXT) -- trailing\n;";

        assertThat(StoredLibraryValidator.normalise(schema)).isEqualTo(StoredLibraryValidator.normalise(stored));
    }
}
