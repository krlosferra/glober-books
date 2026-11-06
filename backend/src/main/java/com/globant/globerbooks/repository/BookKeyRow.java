package com.globant.globerbooks.repository;

import java.time.Instant;

/**
 * The columns the duplicate scan reads (BR-003): id, stored title, stored author and creation instant.
 * It is a JPQL constructor projection, so the scan never loads whole books. Titles and authors are user
 * text: {@link #toString()} hides every member.
 *
 * @param id the book id
 * @param title the stored title
 * @param author the stored author
 * @param dateAdded the creation instant
 */
public record BookKeyRow(String id, String title, String author, Instant dateAdded) {

    /**
     * Hides the members.
     *
     * @return a fixed text
     */
    @Override
    public String toString() {
        return "BookKeyRow[hidden]";
    }
}
