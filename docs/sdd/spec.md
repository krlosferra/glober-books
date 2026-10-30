# GloberBooks Product Specification

- Owner: PM (`sdd-product-manager`)
- Version: 0.4.1
- Last updated: 2026-10-30
- Status: Draft (Phase 2, round 3 of 3, final diff check). Not yet approved: it awaits the Liaison and Security diff checks and Sponsor approval at the Phase 2 gate. Decisions DEC-001 to DEC-032 are CONFIRMED by the Sponsor (2026-10-30); no decision is pending. The specification is frozen after this round: further findings become change requests during the build (`delivery-strategy.md`).
- Authority: below `constitution.md` v1.0, above `plan.md`. Companion: `requirements.md` (register), `handoffs/007` (discovery and decisions), `handoffs/015` (current review request, with the resolution of handoffs 013 and 014).
- Conventions: "must" is binding. API texts (`error`, `message`) are informative and tests never assert their wording; every text in quotation marks in the UI stories (US-010 to US-014) and in Section 5.9 is exact and is asserted by tests. "Code point" means a Unicode code point. "Rejected" means: the stated status and code are returned and no data changed. "Pre-application answer" means an answer the server gives while it is still parsing the request line or the headers, before the application sees the request (Appendix D, introduction).

## 1. Overview

**Problem Statement:** Readers need a simple way to track books they want to read, are reading, and have completed, and to remember how much they liked the ones they finished.

**Target Users:** Individual readers managing a personal reading list on their own machine. One user, one library, no accounts. GloberBooks protects against other web pages, not against other users logged in to the same machine: any local user can reach the open API (risk R-16, accepted by the Sponsor, DEC-031).

**Product:** a small REST API and a web page served by the same application. The web page is the user interface; the API is its only data channel.

**Success Metrics** (measurable; verification in Phase 4):
- M-1 A user adds a book in under 10 seconds: using the keyboard only, type the title, Tab, type the author, Enter, from page load, with no pointer input and no other control (US-011).
- M-2 The library is visible instantly: with 1,000 books the list is on screen within 1 second of page load; the list API with 5,000 books of typical size (titles and authors of about 40 characters) answers within 1 second; any single-book operation answers within 300 ms on typical data; with 4,999 books of maximum size (title 500 and author 200 code points) an add of a book that matches none completes within 2 seconds and an add that duplicates a stored book is answered `409 DUPLICATE_BOOK` within 2 seconds; neither returns `500`. Reference environment: the project's development machine, application and browser on the same host; measured once and the figures recorded (DEC-020).
- M-3 Zero data loss on restart: every write acknowledged with a 2xx response is present after the application process is killed and restarted (US-009).
- M-4 100% of attempts to rate a book that is not completed are rejected (BR-007).
- M-5 Every acceptance criterion in this document maps to at least one automated test (constitution, Testing Requirements).

**Functional requirements covered:** FR1 Add Book, FR2 Track Status, FR3 Rate Books. FR4 Author Biography is reserved and is not specified in this version. Its identifiers US-005 and BR-005 are reserved.

**Invariants** (must be true at all times, after every request, including failed ones):
- INV-1 A book's `rating` is non-null only if its `status` is `completed`.
- INV-2 A non-null `rating` is an integer from 1 to 5.
- INV-3 `status` is always exactly one of `unread`, `reading`, `completed`.
- INV-4 `date_completed` is non-null if and only if `status` is `completed`.
- INV-5 A request without `allow_duplicate: true` never results in two books with the same duplicate key (BR-003).
- INV-6 Every error response uses the envelope and codes of Appendix A (except pre-application answers, Appendix A note).
- INV-7 The library never holds more than 5,000 books (DEC-013), also under concurrent requests.
- INV-8 A failed request changes nothing (BR-013).

**Delivery priorities (DEC-030).** Every requirement is Must except REQ-047 (Should), REQ-014 (Won't) and REQ-004 (reserved). If time runs out, work is cut in this order and in no other: (1) time thresholds and recorded timings, and visual layout measurements, namely the 1 s list figures and the 300 ms single-book figure of M-2 and DEC-020, US-010.9, the recorded time of C-05, the 320-pixel clause of US-014.4 and the bounding-box measurement of US-014.6; (2) US-014.3 and US-014.5 (the contrast rules inside the automated check of US-014.1 are not cut). Only the items above are cut, and no security item. Never cut: invariants, business rules, every security criterion and every other Appendix D case, in particular test E-22 (the slow-request protection, the 5-second connection timeout), the containment rules of the stylesheet (US-014.6, checked by reading the stylesheet), the 2-second add bound of M-2 and C-11 together with the assertions of C-11 that no response is `500` and that a failed request changed nothing, the 300 ms bound on parser failures (Section 5.0, T-31) and the 2-second response of E-33. A cut item is reported as open in the delivery report, never dropped silently.

## 2. User Stories

Acceptance criteria are numbered `US-nnn.k` and are binary.

### US-001: Add Book (FR1)
**As a** reader **I want** to add a book with title, author and status **so that** I can track my reading list.

Acceptance Criteria:
- US-001.1 `POST /api/v1/books` with a valid `title` and `author` and no `status` returns 201 and a book whose `status` is `unread`, `rating` is null and `date_completed` is null.
- US-001.2 The response carries a server-generated `id` (lowercase UUID) and `date_added` (fixed timestamp format of Section 4); a client-supplied `id`, `date_added`, `date_completed` or `rating`, or any other unknown member, is rejected with `400 MALFORMED_REQUEST`.
- US-001.3 `status` is optional; when given it must be one of `unread`, `reading`, `completed` (BR-001). A book created as `completed` has `date_completed` equal to `date_added` and `rating` null (BR-008).
- US-001.4 `title` is required, 1 to 500 code points and `author` is required, 1 to 200 code points, counted per BR-006. Boundary cases: 500 accepted, 501 rejected; 200 accepted, 201 rejected; empty or whitespace-only rejected; `null` or missing rejected; each with `400 VALIDATION_ERROR` and the field name in `details`.
- US-001.5 Leading and trailing white space is removed from `title` and `author` before storage and before measuring; the response shows the stored (trimmed, NFC) values. Letter case and internal spacing are preserved.
- US-001.6 Text forbidden by BR-006 (for example NUL, a tab or line feed inside the text, a zero-width space, bidirectional overrides, private-use, unassigned and tag code points, text that renders blank, too many combining marks, a misplaced joiner) is rejected with `400 VALIDATION_ERROR`; an unpaired surrogate escape or invalid UTF-8 anywhere in the body is rejected with `400 MALFORMED_REQUEST` (BR-006, DEC-027).
- US-001.7 When the library already holds 5,000 books the request is rejected with `409 LIBRARY_FULL`, whether or not `allow_duplicate` is true (DEC-013, BR-009).
- US-001.8 A matching existing book triggers the duplicate flow of US-004, not an error of this story.
- US-001.9 After a 201 response, `GET /api/v1/books/{id}` returns the same book (read-your-writes).
- US-001.10 A body that is `{}` is rejected with `400 VALIDATION_ERROR` and `details` holding one entry for `title` and one for `author`.

### US-002: List and filter books (FR1, FR2 support)
**As a** reader **I want** to see all my books, newest first, and filter by status **so that** I can review my reading progress.

Acceptance Criteria:
- US-002.1 `GET /api/v1/books` returns 200 with `books` (array) and `total` (number of entries in `books`).
- US-002.2 Books are ordered by `date_added` descending; books with an identical `date_added` are ordered by `id` ascending (BR-010).
- US-002.3 Each entry contains exactly `id`, `title`, `author`, `status`, `rating`, `date_added`, `date_completed`; `rating` and `date_completed` are null (not omitted) when absent.
- US-002.4 `GET /api/v1/books?status=reading` returns only books with that status, same order; the same for `unread` and `completed`. `total` counts the filtered result.
- US-002.5 An empty library, or a filter that matches nothing, returns 200 with `"books": []` and `"total": 0` (not 404).
- US-002.6 A `status` value other than the three exact lower-case values (including `READING`, ` reading`, an empty value, `?status` without a value) is rejected with `400 INVALID_STATUS`. The value is judged after standard query decoding (Section 5.0): `+` is a space, so `?status=%72eading` is accepted and `?status=reading+` is `INVALID_STATUS`.
- US-002.7 A repeated `status` parameter, or any query parameter other than `status` (parameter names are case-sensitive: `Status` is unknown), is rejected with `400 MALFORMED_REQUEST`; this check precedes the value check (BR-012).
- US-002.8 The response never contains more than 5,000 entries and no pagination members exist (Appendix B).

### US-003: Rate completed books (FR3)
**As a** reader **I want** to rate books I have completed **so that** I can remember which ones I enjoyed.

Acceptance Criteria:
- US-003.1 `PUT /api/v1/books/{id}/rating` with `{"rating": 4}` on a completed book returns 200 and the updated book with `rating` 4.
- US-003.2 Each integer 1, 2, 3, 4, 5 is accepted; 0, 6, -1, -0, 4.9, 5.0, 1e0, 1E1, a number larger than any 64-bit integer (up to 100 characters long as written), `null` and a missing `rating` member are rejected with `400 INVALID_RATING` (BR-002).
- US-003.3 A valid rating sent for a book whose status is `unread` or `reading` is rejected with `400 RATING_NOT_ALLOWED` (BR-007) and the book is unchanged. An invalid rating value sent for such a book is `INVALID_RATING` (BR-012: field values before state rules).
- US-003.4 An existing rating can be replaced by another valid value (DEC-002); sending the current value returns 200 and changes nothing.
- US-003.5 An empty object is rejected with `400 INVALID_RATING`.
- US-003.6 A value of the wrong JSON type (for example the string `"5"`, `true`, an array, an object) is rejected with `400 MALFORMED_REQUEST`; so is a number that is not valid JSON (`01`, `+5`, `NaN`) or whose literal, as written, is longer than 100 characters.
- US-003.7 `DELETE /api/v1/books/{id}/rating` returns 200 and the book with `rating` null; it is idempotent (a book without a rating, completed or not, also returns 200) (DEC-009).
- US-003.8 With a valid body, a well-formed unknown id returns `404 BOOK_NOT_FOUND`; a malformed id returns `400 MALFORMED_REQUEST` (BR-011). An invalid body for an unknown id reports the body error (BR-012 order: field values before existence).
- US-003.9 A rating is never changed by any operation other than the two above, except by the clearing of BR-004.

### US-004: Prevent duplicate books (FR1, BRD Data Integrity)
**As a** reader **I want** to be warned about duplicate books **so that** I do not add the same book twice, yet can still add a different edition on purpose.

Acceptance Criteria:
- US-004.1 `POST /api/v1/books` for a title and author whose duplicate key equals that of an existing book is rejected with `409 DUPLICATE_BOOK` and nothing is stored.
- US-004.2 The 409 body carries `details.existing_book`, the full book representation of the matching book; if several match, the one with the earliest `date_added` (ties: lowest `id`) (DEC-005).
- US-004.3 The same request with `"allow_duplicate": true` returns 201 and stores a new, separate book with its own `id`; the existing book is untouched.
- US-004.4 `allow_duplicate` is optional, boolean, default false; `"allow_duplicate": false` behaves as absent; any other type (including `null`) is rejected with `400 MALFORMED_REQUEST`. `allow_duplicate` is not part of the stored or returned book.
- US-004.5 Matching ignores letter case, leading and trailing white space, Unicode compatibility forms and repeated internal white space (BR-003): "Clean Code" by "Robert Martin" matches " clean   code " by "ROBERT MARTIN" and the full-width form "Ｃlean Code". Matching uses lower-casing, not case folding: "Straße" and "STRASSE" do not match.
- US-004.6 The same title with a different author, or the same author with a different title, is not a duplicate. Status, rating and dates do not take part in matching.
- US-004.7 `allow_duplicate: true` on a request with no matching book simply creates the book.
- US-004.8 Two simultaneous identical requests without `allow_duplicate` result in exactly one 201 and one 409 (BR-013).
- US-004.9 At capacity `LIBRARY_FULL` is reported before `DUPLICATE_BOOK`, with or without `allow_duplicate` (BR-012).

### US-005: reserved
The identifier is reserved. This version has no behavior for it.

### US-006: Track status (FR2)
**As a** reader **I want** to change a book's status **so that** my list reflects my progress.

Acceptance Criteria:
- US-006.1 `PUT /api/v1/books/{id}/status` with `{"status": "reading"}` returns 200 and the updated book.
- US-006.2 Every transition between the three statuses is allowed (nine combinations including the same-state ones) (DEC-006).
- US-006.3 A change to `completed` from another status sets `date_completed` to the time of the change and leaves `rating` null (BR-008).
- US-006.4 A change from `completed` to `unread` or `reading` sets `rating` to null and `date_completed` to null in the same operation (BR-004, DEC-007). After a later change back to `completed`, `date_completed` is the new time and `rating` is null.
- US-006.5 A change to the status the book already has (`unread` to `unread`, `reading` to `reading`, `completed` to `completed`) returns 200 and changes nothing, in particular not `date_completed` and not `rating` (idempotent).
- US-006.6 A status that is not exactly `unread`, `reading` or `completed` (including `Completed`, `"reading "`, the empty string, `null`, a missing member, an empty object) is rejected with `400 INVALID_STATUS` (BR-001).
- US-006.7 A non-string status (number, boolean, array, object) is rejected with `400 MALFORMED_REQUEST`.
- US-006.8 With a valid body, unknown id: `404 BOOK_NOT_FOUND`; malformed id: `400 MALFORMED_REQUEST`.
- US-006.9 `title`, `author`, `id` and `date_added` never change through this endpoint.

### US-007: View one book
**As a** reader **I want** to fetch a single book **so that** the page can refresh one entry without reloading the list.

Acceptance Criteria:
- US-007.1 `GET /api/v1/books/{id}` returns 200 and the book (same members as US-002.3).
- US-007.2 Unknown well-formed id: `404 BOOK_NOT_FOUND`. Malformed id (uppercase letters, wrong length, any non-UUID text, any text containing `%`): `400 MALFORMED_REQUEST`, except that a segment which Section 5.0 makes not routable (an encoded `/`, `\` or `;`, a literal `\` or `;`, a dot segment) is `404 NOT_FOUND`, because the route is decided before the id (BR-012 steps 2 and 3). A trailing slash, a `;` suffix or an encoded variant of a fixed path segment is a route that does not exist: `404 NOT_FOUND` (constitution API Standards, Section 5.0).

Priority Must (DEC-030; the page and US-001.9, US-008.1 depend on it).

### US-008: Delete a book
**As a** reader **I want** to remove a book **so that** my list stays correct (a mistaken entry, a book I no longer care about). Title and author cannot be edited in v1 (DEC-010), so deleting and re-adding is the way to correct them.

Acceptance Criteria:
- US-008.1 `DELETE /api/v1/books/{id}` returns 200 and the representation of the deleted book (DEC-009); the book no longer appears in the list, and `GET /api/v1/books/{id}` returns 404.
- US-008.2 A second delete of the same id returns `404 BOOK_NOT_FOUND`.
- US-008.3 Deleting a book frees one slot of the capacity limit.
- US-008.4 Deletion is permanent; the API has no undo and no archive.
- US-008.5 Unknown id: `404 BOOK_NOT_FOUND`; malformed id: `400 MALFORMED_REQUEST`; a request body that is not empty: `400 MALFORMED_REQUEST`.

### US-009: Data durability
**As a** reader **I want** my list to be there next time **so that** I never lose my records.

Acceptance Criteria:
- US-009.1 After an acknowledged add, status change, rating change or delete (2xx), stopping the application normally and starting it again shows the same data.
- US-009.2 The same holds when the application process is terminated forcibly right after the response (DEC-019).
- US-009.3 If the database file is absent or has zero length, the application creates it. If it is a valid GloberBooks database (US-009.5), the application opens it and never removes or alters books. The permission checks of US-009.6 apply first: a database file created for a test must have mode 0600 and its directory mode 0700.
- US-009.4 Loss of data on power failure or operating system crash is not guaranteed by this specification (stated limit).
- US-009.5 A file is a valid GloberBooks database when it is a database of the kind used by the application, contains the Book structure of Section 4 and no other table, view, trigger or virtual table, and its rows satisfy the formats and limits of Section 4 (id, timestamp format, status, rating, title and author length counted in code points), INV-1 to INV-4 and the 5,000 limit. In every other case (not a database, damaged, other structure, no tables, a view, trigger or virtual table, rows violating a format, a limit or an invariant, not readable or writable) the application exits with a non-zero status without serving any request, reports a reason on the console that does not contain book data, and leaves the bytes of the database file unchanged (files the storage creates next to it are not part of this criterion) (DEC-028).
- US-009.6 On a POSIX system the application refuses to start (same reporting as US-009.5, database file untouched) when the data directory or the database file is a symbolic link, is owned by another user, or is accessible by group or others. It creates a missing directory with mode 0700 and a missing file with mode 0600 before the first connection. On Windows this check is not made (stated platform limit).
- US-009.7 If another running instance of the application already uses the same database file, the second start fails with a non-zero exit status and the same reporting as US-009.5, and the first instance keeps serving. The first instance holds its claim on the file from before its first query until it stops, not only during a transaction. A second instance on the same database is not supported (DEC-032).

### US-010: Library view (UI)
**As a** reader **I want** a page that shows my library **so that** I see everything at a glance.

Acceptance Criteria:
- US-010.1 Opening the application root (`/`) shows a page whose document title and single level-1 heading read "GloberBooks", with the add form (US-011), the status filter and the list; no other page exists. The list is a list named "Books" whose items are the entries; the heading above it reads "Your books" (level 2).
- US-010.2 While the list loads, a visible "Loading your books…" text is shown inside an element with `role="status"` or `aria-live="polite"`.
- US-010.3 On success each entry (one list item) shows the title, the author, the status as the label "Unread", "Reading" or "Completed", "Added: YYYY-MM-DD" (the `date_added` calendar date in the browser's local time zone) and, for a completed book, "Completed: YYYY-MM-DD" (same rule). The rating is shown as "Rating: n of 5" when present, "Not rated" for a completed book without rating, and nothing for other statuses.
- US-010.4 The order is the API order (newest first). A count "n books" (singular "1 book", "0 books" for none; digits without thousands separator, for example "1000 books") is shown. The count is the `total` of the last successful load plus the books added since then that match the active filter minus the books deleted since then; it is therefore unchanged by an add that does not match the filter, and an entry marked by US-012.8 still counts until the next load. The count is shown whenever the list is shown and is not shown in the load-failure state (US-010.7).
- US-010.5 The filter is a radio group named "Filter by status" with the options "All", "Unread", "Reading", "Completed" (the checked option is the current choice; "All" is checked at page load). Choosing an option loads the list with the matching `status` parameter ("All" sends none).
- US-010.6 Empty library (All checked and the count of US-010.4 is 0, whether from a load or after the last entry was deleted): message "Your reading list is empty. Add your first book above." Empty filter result (another option checked and the count of US-010.4 is 0): message "No books with this status." plus a button "Show all books" that checks "All" and loads the list. Neither message is shown in the load-failure state (US-010.7).
- US-010.7 If loading fails, the message of Section 5.9 for that failure and a "Try again" button are shown; the button repeats the request with the active filter; no book entry (zero list items) is present in the page until a load succeeds. Until a list load has succeeded, and again after a load has failed, a book added through the form is not inserted into the page: it appears at the next successful load, and its confirmation (US-011.5) is still announced.
- US-010.8 Title, author and all other strings are displayed as text; the text content of the element equals the stored value; markup in a title is shown literally (US-014).
- US-010.9 With 1,000 books, all 1,000 entries are present in the page 1,000 ms after the `load` event (measured once on the reference environment, DEC-020). With 5,000 books of typical size the time until all entries are present is measured once and recorded; it has no pass threshold (SEC-F-054, DEC-020).
- US-010.10 The list is requested only when the page loads, when the filter changes, when "Try again" is pressed, and after a `404 BOOK_NOT_FOUND` (US-012.7); no other action reloads it. An add inserts the returned book at the top of the list only when it matches the active filter (US-011.5); a successful delete removes the entry; neither requests the list. When several list requests overlap, the page shows the result of the request sent last (for the option checked now); the response to an earlier request is discarded.

### US-011: Add a book and resolve duplicates (UI)
**As a** reader **I want** a quick form with a clear duplicate warning **so that** adding is fast and safe.

Acceptance Criteria:
- US-011.1 The form has labelled fields "Title" and "Author", a "Status" single-choice control with the options "Unread", "Reading", "Completed" defaulting to "Unread", and an "Add book" button. Keyboard focus is on Title at page load.
- US-011.2 Using the keyboard only (type the title, Tab, type the author, Enter) submits the form (M-1).
- US-011.3 Client-side checks (UX only) cover: empty or whitespace-only, and over the length limit counted in code points. Forbidden characters are left to the server (US-011.6). A failed check shows an inline message linked to the field and no request is sent.
- US-011.4 While a request from the form is in progress the button is disabled and any further submission by any means (button, Enter) sends no request, so a double click or a repeated Enter sends one request.
- US-011.5 On 201 the book appears at the top of the list when it matches the active filter and a list load has succeeded (US-010.7), and the count follows US-010.4, the form is cleared and Status resets to "Unread", a polite confirmation "Book added: <title>" is announced (also when the book does not match the filter), and focus returns to Title.
- US-011.6 On `400 VALIDATION_ERROR` with `details`, the message of each entry for `title` or `author` is shown next to that field (the text shown equals the `message` of the response) and for an entry with `field` `status` the text "Choose Unread, Reading or Completed." is shown beside the Status control; focus moves to the first field in error. On `400 INVALID_STATUS` the same Status text is shown. (These two paths cannot be reached through the page's own controls, so a page test with a stubbed response covers them.)
- US-011.7 On `409 DUPLICATE_BOOK` a modal dialog named "Possible duplicate" opens. Its text is "This book may already exist. Add anyway?" followed by the existing book's title, author, status label and "Added: YYYY-MM-DD" from `details.existing_book`, with buttons "Add anyway" and "Cancel". Initial focus is on "Cancel"; Escape cancels.
- US-011.8 "Cancel" closes the dialog, keeps the typed values and returns focus to the Title field. "Add anyway" closes the dialog, then resends the same values with `"allow_duplicate": true`, then follows US-011.5.
- US-011.9 On `409 LIBRARY_FULL` a message "Your library is full (5,000 books). Delete a book to add another." is shown and the typed values are kept (DEC-013). This also applies after "Add anyway".
- US-011.10 Any other failure is shown per Section 5.9 and the typed values are kept.

### US-012: Change status, rate, clear rating and delete (UI)
**As a** reader **I want** controls on each book **so that** I can maintain my list without leaving the page.

Acceptance Criteria:
- US-012.1 Each entry has a single-choice status control whose accessible name is "Status of <title>" with the three values "Unread", "Reading", "Completed"; choosing a different value sends `PUT .../status`. Titles are not unique (US-004.3), so accessible names may repeat; tests scope by the list item.
- US-012.2 The rating control exists on every completed entry and on no other entry (so it appears after a successful change to `completed` and disappears after a successful change away from it).
- US-012.3 If the entry shows a rating and the user chooses `unread` or `reading`, a confirmation dialog named "Confirm status change" first says "Moving <title> back to <Unread|Reading> removes its rating and completion date." with "Continue" and "Cancel"; initial focus on "Cancel"; "Continue" sends the request, "Cancel" restores the previous choice without a request (DEC-007). A completed entry without a rating changes without a dialog. The confirmation is client-side only: the API clears the data on its own (BR-004), also when called without any confirmation.
- US-012.4 The rating control is a labelled group named "Rating of <title>" offering the options "1" to "5", operable by keyboard, showing the current value as text, and a button "Clear rating of <title>" when a rating exists. Selecting a value sends `PUT .../rating`; "Clear rating of <title>" sends `DELETE .../rating`.
- US-012.5 Each entry has a button named "Delete <title>"; it opens a confirmation dialog named "Delete book" with the text "Delete <title>? This cannot be undone." and buttons "Delete" and "Cancel"; initial focus on "Cancel". "Delete" sends `DELETE`; on success the entry disappears, the count follows US-010.4, "Book deleted: <title>" is announced and focus moves to the "Your books" heading (programmatically focusable).
- US-012.6 After any successful change the entry shows the server's returned values (not optimistic ones); while a request for an entry is in progress its controls are disabled.
- US-012.7 If a request fails, the control returns to the server's last known value and the message of Section 5.9 is shown next to the entry; on `404 BOOK_NOT_FOUND` the entry is removed, the list is reloaded (US-010.10) and "This book no longer exists." is announced in the page-level message area (US-013.4).
- US-012.8 Changing status while a filter is active: an entry that no longer matches the filter stays visible until the next list load and shows the text "No longer matches this filter".
- US-012.9 No control edits an existing book's title or author (DEC-010).

### US-013: Error, loading and empty states (UI)
**As a** reader **I want** clear messages when something fails **so that** I know what to do next.

Acceptance Criteria:
- US-013.1 The page maps every `code` of Appendix A to the behavior of Section 5.9; the page chooses its text by `code`, never by the server's `error` text.
- US-013.2 A failure to reach the server (no response) shows "Cannot reach the server. Check that GloberBooks is running, then try again." and keeps the user's input.
- US-013.3 A response that is not a valid error envelope (not JSON, missing `code`, unknown `code`) is treated as `INTERNAL_ERROR`.
- US-013.4 Errors and results are announced by placing their text in an element with `role="status"` or `aria-live="polite"`; field errors are tied to their fields; one failure causes one update of the live region. The page has one page-level message area, an element with `role="status"` and the accessible name "Messages"; it holds the confirmations of US-011.5 and US-012.5, the announcement of US-012.7, and the messages of US-011.9, US-011.10 and US-013.2. Messages tied to a field or an entry stay beside that field or entry.
- US-013.5 No raw `error` text, status code, stack, URL or id is displayed to the user except the book's own data.
- US-013.6 No error leaves the page in a state where controls stay disabled permanently.

### US-014: Accessibility and safe rendering (UI)
**As a** reader (including with assistive technology) **I want** an accessible page **so that** I can use it however I work.

Acceptance Criteria:
- US-014.1 An automated accessibility check against the WCAG 2.1 A and AA rules reports zero violations of impact "serious" or "critical" on the page with at least three books on the list (one completed and rated), the add form showing two field errors, and each dialog open (DEC-026: the tool is chosen by the testing documents, not by this spec).
- US-014.2 Every control has a programmatic label; all operations work with the keyboard alone; focus is always visible (the focused control has an indicator with at least 3:1 contrast against its surroundings); dialogs trap focus and return it to the control that opened them when closed, except where a story names another target (US-011.5, US-011.8, US-012.5).
- US-014.3 Text contrast is at least 4.5:1 (3:1 for large text and control boundaries); status and errors are never conveyed by color alone.
- US-014.4 The page has the document language `en`, a page title, one `h1`, a main landmark; at a viewport 320 CSS pixels wide there is no horizontal scrolling of the page.
- US-014.5 Animations are absent or disabled when the user prefers reduced motion.
- US-014.6 Titles and authors containing `<script>alert(1)</script>`, `<img src=x onerror=alert(1)>`, quotes, ampersands, an unbroken word of 500 code points, and a title of 500 code points made of base characters each followed by 8 combining marks (the worst case BR-006 allows) are displayed as inert text; no content-security violation is reported in the browser console during any flow (a failed request for `/favicon.ico` is not a page failure and is excluded). Containment: shown in the list and in each dialog, the text lies inside the bounding box of its list item or dialog, and the buttons of the dialog and of the list item remain visible and operable (asserted by a bounding-box measurement). The stylesheet clips or wraps overflowing text in the list item and in each dialog (long words broken, content clipped to the box); this is checked by reading the stylesheet and is not part of the cut (Section 1).
- US-014.7 The page makes no request to any origin other than its own.
- US-014.8 The page takes no data from the address (`location.search`, `location.hash`), `window.name`, `document.referrer` or `postMessage`, and stores nothing in `localStorage`, `sessionStorage`, cookies or IndexedDB.

## 3. Business Rules

Every rule has at least one passing scenario and one failing scenario (constitution, Testing Requirements).

### BR-001: Status validation
**Given** a request supplies a status (creating a book, changing status, or filtering the list)
**When** the value is exactly the text `unread`, `reading` or `completed` (lower case, no surrounding space)
**Then** it is accepted.
**Given** a value of the right JSON type (a string, or `null`, or missing where required) that is anything else, including `Completed`, `UNREAD`, `"unread "`, the empty string
**When** the request is evaluated
**Then** it is rejected with `400 INVALID_STATUS` and a `details` entry with `field` `status`. On a request where only `status` fails, the code is `INVALID_STATUS`; where `status` and another field fail, the code is `VALIDATION_ERROR` (BR-012 step 6).
**Given** a status value of another JSON type (number, boolean, array, object)
**Then** the request is rejected with `400 MALFORMED_REQUEST`.
Exception: on `POST /api/v1/books` an absent `status` member means `unread`; an explicit `null` is `INVALID_STATUS`.

### BR-002: Rating value validation
**Given** a request assigns a rating
**When** the value is a JSON integer literal from 1 to 5
**Then** the value is valid.
**Given** a JSON number outside that range or not written as an integer literal (0, 6, -3, -0, 4.9, 5.0, 1e0, 1E1, a number beyond the 64-bit range of at most 100 characters as written), `null`, or a missing `rating` member
**When** the request is evaluated
**Then** it is rejected with `400 INVALID_RATING` and a `details` entry with `field` `rating`. Because a rating request has one field, an invalid rating is always `INVALID_RATING`, also for a book that is not completed (BR-012 step 6 precedes step 8).
**Given** a `rating` of another JSON type (string, boolean, array, object), or a number that is not valid JSON or whose literal as written is longer than 100 characters
**Then** it is rejected with `400 MALFORMED_REQUEST`.

### BR-003: Duplicate detection and override
**Given** a book exists whose duplicate key equals the duplicate key of the incoming title and author
**When** a book is added without `allow_duplicate: true`
**Then** the request is rejected with `409 DUPLICATE_BOOK` carrying the existing book in `details.existing_book`, and nothing is stored.
**Given** the same situation
**When** the request carries `allow_duplicate: true`
**Then** the book is added (201) as a separate book.
**Definition:** the duplicate key of a (title, author) pair is the pair of the title key and the author key; a text key is obtained from the already trimmed and NFC-normalized text (BR-006) by normalizing to NFKC, converting to lower case with the Unicode default lower-case mapping (not case folding, and without regard to locale), collapsing every run of white space inside to a single space, and trimming the result (DEC-001). So "Clean Code" and "CLEAN CODE" are duplicates and "Straße" and "STRASSE" are not. Keys are computed in application logic and are not part of the API.
**Scope:** only creation is checked. Status, rating and dates are ignored. All books count, whatever their status. Because title and author cannot change after creation (DEC-010), no other operation can create a duplicate.
**Given** no matching book **When** a book is added **Then** it is added (201).

### BR-004: Leaving the completed state clears dependent data
**Given** a book with status `completed` (with or without a rating)
**When** its status is changed to `unread` or `reading`
**Then** in the same operation its `rating` becomes null and its `date_completed` becomes null; the response shows both as null (DEC-007).
**Given** a book with status `unread` or `reading`
**When** its status is changed to the other of the two
**Then** `rating` and `date_completed` stay null and nothing else changes.
The API needs no confirmation parameter; the page's confirmation (US-012.3) is a client-side protection only.

### BR-005: reserved
Reserved. No behavior in this version.

### BR-006: Title and author text
**Given** a title or an author in a request
**When** it is processed
**Then**
1. it must be a JSON string, else `MALFORMED_REQUEST`; `null` or a missing member counts as empty;
2. white space at both ends is removed (characters with the Unicode White_Space property, which include no-break space, the Unicode line and paragraph separators and U+0085; a tab or line feed at an end is therefore trimmed, not rejected);
3. the text is converted to Unicode normalization form NFC;
4. the result is measured in Unicode code points (an emoji or a character outside the basic plane counts 1);
5. a title must be 1 to 500 and an author 1 to 200 code points;
6. it must not contain: control characters (category Cc, which includes tab, line feed and NUL); format characters (Cf) other than U+200C and U+200D; tag characters (U+E0000 to U+E007F); line and paragraph separators (Zl, Zp); private-use code points; noncharacters; or code points unassigned in Unicode 15.0. U+FE0F is category Mn and is allowed. White space of category Zs is allowed inside the text;
6b. visible content: the text must contain at least one code point of category L, N, P or S other than U+115F, U+1160, U+3164, U+FFA0 and U+2800, and must not begin with a code point of category Mn, Mc or Me;
6c. marks: a mark run is a maximal sequence of consecutive code points each of which is of category Mn, Mc or Me, or is U+200C or U+200D. A mark run has at most 8 code points and contains at most one variation selector (U+FE00 to U+FE0F, U+E0100 to U+E01EF); a variation selector is category Mn and counts toward the 8;
6d. joiners: U+200C and U+200D must not be the first or last code point of the text, must not be next to another U+200C or U+200D (either kind, the same kind twice included), and must not be next to a code point of category Zs.

Items 6 to 6d are judged on the text after items 2 and 3 (trimmed, NFC). "Consecutive" and "next to" mean adjacent code points in that text. The Unicode version is 15.0; changing it is a spec change.
**And** a violation of items 5 to 6d (including an empty result) is rejected with `400 VALIDATION_ERROR` and one `details` entry per failing field (`field` `title` or `author`), whose message is the first failing check in this order: required (null or missing), empty, length, characters (the message "contains characters that are not allowed" covers items 6 to 6d).
**And** an unpaired surrogate (for example the JSON escape `\uD800`, or a pair in the wrong order) or invalid UTF-8 anywhere in the body is not text: the whole request is rejected with `400 MALFORMED_REQUEST` (BR-012 step 5, DEC-027).
**And** the stored and returned text is the trimmed NFC form; letter case and internal spacing are preserved.
Passing examples: "Les Misérables" / "Victor Hugo"; a title of 500 emoji (U+1F600); a title written with decomposed "é" (e plus U+0301) 500 times (counts 500 after NFC); a family emoji joined with U+200D; the Persian word "می‌خواهم" (U+200C between two letters); "ab", U+3164, "cd"; "x" followed by 8 times U+0301; "x" followed by 7 times U+0301, U+200D and "y" (a run of 8); the literal character U+FFFD between letters. Failing examples (`VALIDATION_ERROR`): "", "   ", a single U+00A0, 501 characters, U+0000 inside, a line feed inside, a tab inside, U+200B, U+202E, U+E000, U+0378 (unassigned), U+E0041 (tag), U+FFFE, U+FDD0, a title that is only U+3164 or only U+2800 or only U+0301, a title that begins with U+0301, "x" followed by 9 times U+0301, "x" followed by 8 times U+0301, U+200D and U+0301 (a run of 10 marks and a joiner), "a" followed by U+E0100 and U+E0101, "a" followed by U+FE0F, U+200D and U+FE0F (two variation selectors in one run), U+200C or U+200D as first or last character, U+200C next to U+200D, U+200D next to U+200D, a joiner next to a space. Failing example (`MALFORMED_REQUEST`): `"\uD800"`.

### BR-007: Rating only for completed books
**Given** a book with status `completed`
**When** a valid rating is assigned
**Then** it is stored.
**Given** a book with status `unread` or `reading`
**When** a valid rating is assigned
**Then** the request is rejected with `400 RATING_NOT_ALLOWED` (DEC-008, the status the BRD gives for rating rules) and the book is unchanged.
**And** no request can create a book with a rating, or leave a non-completed book with one (INV-1).

### BR-008: Completion date
**Given** a book is created with status `completed`
**Then** `date_completed` equals `date_added`.
**Given** a book changes from `unread` or `reading` to `completed`
**Then** `date_completed` is the time of the change.
**Given** a completed book is set to `completed` again
**Then** `date_completed` does not change.
**Given** a book is not completed
**Then** `date_completed` is null (INV-4, DEC-023).
Time values come from the application's clock and use the fixed format of Section 4.

### BR-009: Library capacity
**Given** the library holds fewer than 5,000 books
**When** a valid book is added
**Then** it is added.
**Given** the library holds 5,000 books
**When** any add is attempted (with or without `allow_duplicate`)
**Then** the request is rejected with `409 LIBRARY_FULL` and nothing is stored (DEC-013).
**And** the list endpoint never returns more than 5,000 entries; reads, status changes, rating changes and deletes are not affected by capacity.
**And** under concurrent adds the library never exceeds 5,000 books (the count check and the insert are one indivisible step).

### BR-010: List ordering and filtering
**Given** a library with several books
**When** the list is requested
**Then** books are sorted by `date_added` descending, then `id` ascending for equal `date_added`.
**And** with `status=<value>` only books with exactly that status are returned, in the same order; without it all books are returned.
**And** a repeated or unknown query parameter gives `400 MALFORMED_REQUEST`; then a `status` value that breaks BR-001 gives `400 INVALID_STATUS`.

### BR-011: Identifiers
**Given** a request path containing a book id
**When** the id segment, judged exactly as received and never decoded, is not a lowercase, hyphenated, 36-character UUID (pattern 8-4-4-4-12 of lowercase hexadecimal digits; the version digit is not checked), so that an id containing `%` is malformed (a segment that Section 5.0 makes not routable, such as `%2e%2e` or `%2f`, never reaches this check: it is `404 NOT_FOUND`)
**Then** it is rejected with `400 MALFORMED_REQUEST` before any lookup.
**Given** a well-formed id
**When** no book has it
**Then** the response is `404 BOOK_NOT_FOUND`.
Ids are generated by the system; clients cannot choose them. The route and the method are decided before the id is judged (BR-012 steps 2 and 3).

### BR-012: Order of evaluation
**Given** a request violates several rules
**When** it is evaluated
**Then** the response reports the first violation in this order, and nothing later is evaluated:
1. Transport and security checks of the constitution, applied to every path (API, page, unknown) and every method, in this sub-order: (a) Host: `INVALID_HOST`; (b) `Origin`, then `Sec-Fetch-Site`: `FORBIDDEN`; (c) for every method other than GET and HEAD, `Content-Type` absent or not matching the grammar of Section 5.0 (media type `application/json`; only the parameter `charset=utf-8`; a single header): `UNSUPPORTED_MEDIA_TYPE`; (d) body over 16 KiB (16,384 bytes), also while streaming: `PAYLOAD_TOO_LARGE`. The cap of (d) applies to every request that carries a body, whatever the method and route, including a chunked body that no handler would read. A request rejected by an earlier check is not read further; the server may close the connection after the response.
2. Route and method, in this sub-order: path not routable or unknown `NOT_FOUND`; path known, method not defined for it (HEAD, OPTIONS, TRACE and unknown method names included) `METHOD_NOT_ALLOWED` with an `Allow` header; on `/api/v1/` paths, an `Accept` header that does not accept `application/json` (Section 5.0) `NOT_ACCEPTABLE`. In route matching, `{id}` matches any single non-empty path segment; the id is judged in step 3, after the route and method are known. Example: `GET /api/v1/books/NOT-A-UUID/status` is `405`, `PUT /api/v1/books/NOT-A-UUID/status` is `400 MALFORMED_REQUEST`, `GET /api/v1/books/not-a-uuid/other` is `404`.
3. Path id format (BR-011): `MALFORMED_REQUEST`.
4. Query string (Section 5.0 grammar), in this sub-order: shape (any non-empty query on an endpoint other than the list, including the static paths; on the list, an unknown or repeated parameter, an empty pair, a malformed percent-encoding or a decoded invalid UTF-8 sequence) `MALFORMED_REQUEST`; then the `status` value `INVALID_STATUS`.
5. Body, all `MALFORMED_REQUEST`: invalid UTF-8; unreadable JSON, including a zero-byte body, a byte order mark, comments, trailing commas and every failure of the JSON parser or its limits; a body that is not a JSON object (`null`, array, string, number); an unknown member (member names are case-sensitive and compared after unescaping); a duplicate member (also when written with escapes); content after the JSON value; an unpaired surrogate; a number literal longer than 100 characters; a wrong JSON type for a known member; any body byte (white space included) on GET or DELETE.
6. Field values: if exactly one field fails and it is `status` or `rating`, the code of that rule (`INVALID_STATUS`, `INVALID_RATING`); in every other case, including two or more failing fields on one request, `VALIDATION_ERROR` with all failing fields in `details` (order of entries: `title`, `author`, `status`).
7. Existence of the book: `BOOK_NOT_FOUND`.
8. State rules, in this order: `RATING_NOT_ALLOWED`; `LIBRARY_FULL`; `DUPLICATE_BOOK`.
Unexpected failures are `INTERNAL_ERROR` (DEC-024).

### BR-013: Atomicity and concurrency
**Given** any write request
**When** it fails at any step
**Then** the data is exactly as before the request.
**Given** a status change that must clear the rating and the completion date
**Then** both changes become visible together with the status, never separately.
**Given** two add requests for the same duplicate key arrive at the same time without `allow_duplicate`
**Then** exactly one is stored and the other receives `409 DUPLICATE_BOOK` (the check and the insert are one indivisible step).
**Given** two add requests arrive at the same time when 4,999 books exist
**Then** exactly one is stored and the other receives `409 LIBRARY_FULL`.
**Given** two concurrent updates of the same book, including a status change racing a rating change
**Then** both are applied one after the other and the later one wins; no mixed state is stored, and INV-1 and INV-4 hold in every final state (DEC-018).
**Given** a second instance of the application is started on a database file that a running instance uses
**Then** the second start fails (US-009.7). The guarantees of this rule are stated for the one running instance; any other writer of the database file is outside this specification (DEC-032).

## 4. Data Model (Conceptual)

### Book
| Member | Type | Required | Rules |
|---|---|---|---|
| `id` | text, lowercase UUID version 4, 36 characters | system | generated by the system, immutable, unique |
| `title` | text | yes (client) | BR-006, 1 to 500 code points, stored trimmed and NFC; immutable after creation (DEC-010) |
| `author` | text | yes (client) | BR-006, 1 to 200 code points, stored trimmed and NFC; immutable after creation (DEC-010) |
| `status` | enum `unread` \| `reading` \| `completed` | no (default `unread`) | BR-001 |
| `rating` | integer 1 to 5 or null | no | INV-1, INV-2; null unless completed |
| `date_added` | UTC timestamp `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'` | system | set at creation, immutable |
| `date_completed` | same format or null | system | BR-008, INV-4 |

- The duplicate key is derived data (BR-003); it is not a member of the Book and is not exposed.
- There is exactly one entity in v1 (Book).
- State matrix (INV-1, INV-4):

| status | rating | date_completed |
|---|---|---|
| `unread` | null | null |
| `reading` | null | null |
| `completed` | null or 1 to 5 | timestamp |

- Capacity: at most 5,000 books (BR-009). Text fields and request limits: Appendix B.
- Persistence: all Book data is stored durably in a local file database (constitution); US-009 states the behavior.

## 5. API Contract

### 5.0 Conventions (all endpoints)
- Base path `/api/v1`. JSON members are snake_case. API responses (success and error) are `application/json; charset=UTF-8`, with non-ASCII characters written as UTF-8 text (not as escape sequences). Every response the application produces carries the security headers of the constitution (Security Standards).
- **Routing.** Only the paths and methods below exist. Paths are matched exactly as received. Not routable (`404 NOT_FOUND`): any other path; a trailing slash; a suffix; a `;` in the path; a percent-encoded variant of a fixed segment (for example `/api/v1/%62ooks`); an encoded `/`, `\` or `;`, a literal `\`, and a dot segment (literal or encoded) in any segment (a dot segment is a segment equal to `.` or `..` once every `%2e` or `%2E` in it is decoded; the hexadecimal digits of every escape are case-insensitive; `%2e%2e%2e` is not a dot segment and, in the id position, is `400`). The `{id}` segment matches any other single non-empty segment and is judged as received (BR-011): any `%` in it makes it malformed (`400 MALFORMED_REQUEST`). Percent-encoding validity in the path matters only through these two rules. The server never redirects.
- **Methods.** Another method on a defined path, including HEAD, OPTIONS, TRACE, CONNECT, unknown and lower-case method names, ends as `405 METHOD_NOT_ALLOWED` (or as a pre-application answer, Appendix D); HEAD and OPTIONS are never defined. No response echoes the request. Every `405` carries an `Allow` header listing exactly the methods defined for that path and never HEAD or OPTIONS: `/api/v1/books`: `GET, POST`; `/api/v1/books/{id}`: `GET, DELETE`; `/api/v1/books/{id}/status`: `PUT`; `/api/v1/books/{id}/rating`: `PUT, DELETE`; the page and its asset paths: `GET`.
- **Content type.** Every method other than GET and HEAD must send `Content-Type`, also when it has no body (`DELETE` included). The media type `application/json` is compared case-insensitively. The only allowed parameter is `charset` with the unquoted value `utf-8` (case-insensitive); white space is allowed around `;` and after the value, not around `=`. Any other parameter, a quoted `charset` value, a list, another type, or more than one `Content-Type` header is `415 UNSUPPORTED_MEDIA_TYPE`. (A request without `charset` is accepted; the constitution's `charset=UTF-8` form is accepted too.)
- **Accept** (API paths only). The header may be absent or empty (all white space): accepted. Otherwise it is a comma-separated list of ranges `type/subtype` (`*` allowed) with optional `;name=value` parameters. Empty list elements (`a,,b`, a trailing comma) are ignored; a range without `/`, or with `*` as type and another subtype (`*/json`), is unparseable. The parameter named `q` (name compared case-insensitively) must have a value of the form `0`, `0.` followed by up to three digits, `1`, or `1.` followed by up to three zeros, and any other value makes the header unparseable (a `q` parameter written without `=` has no value and is unparseable, so the header is treated as malformed under the rule below); parameters other than `q` are ignored. A range whose `q` value is zero is ignored; exclusion is by the listed range only, so `application/json;q=0, */*` is acceptable (a stated deviation from RFC 9110 specificity). The request is acceptable when a remaining range is `application/json`, `application/*` or `*/*` (case-insensitive). Otherwise, or when the header cannot be parsed, it is `406 NOT_ACCEPTABLE`. Whatever the `Accept` header, an error body is still JSON. The page ignores `Accept`.
- **Request bodies** are UTF-8 JSON objects of at most 16 KiB (16,384 bytes), else `413 PAYLOAD_TOO_LARGE`. GET and DELETE take no body: any body byte, white space included, is `MALFORMED_REQUEST` (this holds for the page's `GET` too). A request with `Expect: 100-continue` and an oversized `Content-Length` is answered `413` and the server does not send `100 Continue`. `Upgrade` headers are ignored.
- **Strict parsing.** Unknown members, duplicate members (compared after unescaping), content after the JSON value, and wrong JSON types are `400 MALFORMED_REQUEST`. A zero-byte body, a byte order mark, comments, trailing commas, single-quoted strings, unquoted names, a raw control character inside a string, `NaN`, `Infinity`, and a top-level value that is not an object are invalid. Every failure of the JSON parser, of its limits (nesting depth, string, name or number length) or of the UTF-8 decoder is `400 MALFORMED_REQUEST`; none is `INTERNAL_ERROR`; none takes longer than 300 ms. The decoder reports malformed input and never replaces it (a literal U+FFFD in valid UTF-8 is an ordinary character). A string up to the 16 KiB cap must not trip a parser limit (a long string yields `VALIDATION_ERROR` by length, not a parse failure). A number literal is measured in characters as written (sign, digits, fraction, exponent): up to 100 characters it is judged by its value (a 100-character integer literal is `INVALID_RATING` in the rating position), above 100 it is `MALFORMED_REQUEST`.
- **Query strings.** Only `GET /api/v1/books` accepts a query, and only `status` (once). A query is a list of `&`-separated `name=value` pairs; the empty query (a `?` with nothing after it) means no query on every path. Names and values are decoded once (`%XX`; `+` is a space) before comparison; `;` is an ordinary character. A non-empty query on any other endpoint, including `/` and the page's asset paths, is `400 MALFORMED_REQUEST`; so is, on the list, an empty pair (`?&`, `?status=reading&`), an unknown or repeated name, a malformed percent-encoding (`%zz`, `%4`) and a decoded invalid UTF-8 sequence. A `status` pair without `=` has an empty value (`INVALID_STATUS`).
- No CORS: no response carries an `Access-Control-*` header; a preflight request is answered like any other request by BR-012 (a cross-origin preflight is `403 FORBIDDEN`).
- No cookies or sessions: no response carries `Set-Cookie`. The server is reachable on `127.0.0.1` only; no authentication. The server never sends `101`, `206`, `304`, `416`, `204` or `422`.
- Successful responses carry no `message` member; the data is the response.
- Error envelope (all errors): `{ "error": string, "code": string, "timestamp": string, "details"?: ... }`. `details` exists only for the codes in the table of Appendix A and has the shape stated there. Messages never echo input.
- **Book representation** (used by every success response and inside `details.existing_book`), exactly these seven members:

```json
{
  "id": "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b",
  "title": "Clean Code",
  "author": "Robert Martin",
  "status": "completed",
  "rating": 5,
  "date_added": "2026-02-19T10:00:00.000Z",
  "date_completed": "2026-02-20T15:30:00.000Z"
}
```

### 5.1 `GET /api/v1/books` — list (US-002)
Query: `status` (optional, one of the three values).
Success 200:
```json
{
  "books": [ { "id": "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b", "title": "Clean Code", "author": "Robert Martin", "status": "completed", "rating": 5, "date_added": "2026-02-19T10:00:00.000Z", "date_completed": "2026-02-20T15:30:00.000Z" } ],
  "total": 1
}
```
Empty: `{ "books": [], "total": 0 }`.
Errors: 400 `MALFORMED_REQUEST` (query shape, body present), 400 `INVALID_STATUS` (bad `status` value; `details` `[{ "field": "status", "message": "must be one of: unread, reading, completed" }]`).

### 5.2 `POST /api/v1/books` — add (US-001, US-004)
Request:
```json
{ "title": "Clean Code", "author": "Robert Martin", "status": "unread", "allow_duplicate": false }
```
`title`, `author` required; `status` optional (default `unread`); `allow_duplicate` optional (default false).
Success 201: the Book representation (`rating` null, `date_completed` null unless created `completed`).
Errors:
- 400 `VALIDATION_ERROR`: title or author invalid, or two or more fields invalid. Example:
```json
{ "error": "The request contains invalid values.", "code": "VALIDATION_ERROR", "timestamp": "2026-02-19T10:00:00.000Z",
  "details": [ { "field": "title", "message": "must be between 1 and 500 characters" } ] }
```
- 400 `INVALID_STATUS`: only `status` is invalid. `details`: `[{ "field": "status", "message": "must be one of: unread, reading, completed" }]`.
- 400 `MALFORMED_REQUEST`: unreadable JSON, non-object body, unknown member, wrong JSON type (including `allow_duplicate` that is not a boolean), unpaired surrogate, invalid UTF-8.
- 409 `DUPLICATE_BOOK`:
```json
{ "error": "A book with the same title and author already exists.", "code": "DUPLICATE_BOOK", "timestamp": "2026-02-19T10:00:00.000Z",
  "details": { "existing_book": { "id": "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b", "title": "Clean Code", "author": "Robert Martin", "status": "unread", "rating": null, "date_added": "2026-02-19T10:00:00.000Z", "date_completed": null } } }
```
- 409 `LIBRARY_FULL` (no `details`).

### 5.3 `GET /api/v1/books/{id}` — one book (US-007)
Success 200: the Book representation. Errors: 400 `MALFORMED_REQUEST` (id, query or body present), 404 `BOOK_NOT_FOUND`.

### 5.4 `PUT /api/v1/books/{id}/status` — change status (US-006)
Request: `{ "status": "completed" }`
Success 200: the Book representation after the change, for example the book above with `"status": "completed"`, `"rating": null`, `"date_completed": "2026-02-20T15:30:00.000Z"`. Leaving `completed` returns `"rating": null` and `"date_completed": null`.
Errors: 400 `INVALID_STATUS` (details `[{ "field": "status", ... }]`), 400 `MALFORMED_REQUEST` (id, body, wrong type, unknown member), 404 `BOOK_NOT_FOUND`.

### 5.5 `PUT /api/v1/books/{id}/rating` — set or replace the rating (US-003)
Request: `{ "rating": 5 }`
Success 200: the Book representation with the new `rating`.
Errors: 400 `INVALID_RATING` (details `[{ "field": "rating", "message": "must be an integer from 1 to 5" }]`), 400 `RATING_NOT_ALLOWED` (no `details`; the book is not completed), 400 `MALFORMED_REQUEST`, 404 `BOOK_NOT_FOUND`.

### 5.6 `DELETE /api/v1/books/{id}/rating` — remove the rating (US-003.7)
No body (the `Content-Type` header is still required). Success 200: the Book representation with `rating` null. Errors: 400 `MALFORMED_REQUEST` (id, query, or a non-empty body), 404 `BOOK_NOT_FOUND`.

### 5.7 `DELETE /api/v1/books/{id}` — delete (US-008)
No body. Success 200: the Book representation of the deleted book (status 204 is not in the constitution's list). Errors: 400 `MALFORMED_REQUEST`, 404 `BOOK_NOT_FOUND`.

### 5.8 Static content
`GET /` and `GET /index.html` (the same page) serve the web page. The page's own assets are the stylesheet and ES-module script files of the frontend (constitution, Project Structure and Frontend Standards): a fixed, finite set of paths under the application root whose names and number are left to the plan. Tests discover them from the served page (its `link` and `script` elements and module imports) and do not assume names. Only those paths are served: each answers `GET` with `200` under the rules of this section, and every other path, directory paths included, is `404 NOT_FOUND`. It is not part of the versioned API and is governed by the constitution (Static resources, headers). Everything else, including directory paths, `/index.html/`, `/favicon.ico`, files of the build, test files, the database, `/error` and framework paths, is `404 NOT_FOUND` in the standard envelope. Methods other than GET are `405 METHOD_NOT_ALLOWED` with `Allow: GET`. Static responses carry no `ETag`, `Last-Modified` or `Accept-Ranges`; `Range`, `If-None-Match` and `If-Modified-Since` are ignored and the full body is returned with `200`. A non-empty query or any body byte on a static `GET` is `400 MALFORMED_REQUEST` (Section 5.0).

### 5.9 Behavior of the web page for every error code (US-013)
| Code | HTTP | What the page does |
|---|---|---|
| `VALIDATION_ERROR` | 400 | US-011.6: the `details[].message` beside the field named in `field` (`title`, `author`); for `status` the text "Choose Unread, Reading or Completed."; keep input; focus the first field in error |
| `INVALID_STATUS` | 400 | Show "Choose Unread, Reading or Completed." beside the Status control of the entry or of the add form; for an entry restore the control's last known value |
| `INVALID_RATING` | 400 | Show "Choose a rating from 1 to 5." beside the rating control |
| `RATING_NOT_ALLOWED` | 400 | Show "Only completed books can be rated." and reload the entry with `GET /api/v1/books/{id}` (the book is not completed anymore) |
| `DUPLICATE_BOOK` | 409 | Open the duplicate dialog (US-011.7) |
| `LIBRARY_FULL` | 409 | Message of US-011.9 |
| `BOOK_NOT_FOUND` | 404 | US-012.7: remove the entry, reload the list, announce "This book no longer exists." in the page-level message area (US-013.4) |
| `MALFORMED_REQUEST`, `UNSUPPORTED_MEDIA_TYPE`, `NOT_ACCEPTABLE`, `METHOD_NOT_ALLOWED`, `NOT_FOUND`, `PAYLOAD_TOO_LARGE` | 400, 415, 406, 405, 404, 413 | "The request could not be processed. Reload the page and try again." (these indicate a defect or a tampered page, not a user error) |
| `INVALID_HOST`, `FORBIDDEN` | 400, 403 | "This page cannot talk to the server from this address. Open GloberBooks from http://localhost or http://127.0.0.1." |
| `INTERNAL_ERROR` or unreadable response | 500, other | "Something went wrong on our side. Try again." |
| no response | n/a | Message of US-013.2 |

## 6. Out of Scope
- Authentication, accounts, multi-user support, sharing (BRD).
- Author biography (FR4): not specified in this version.
- Editing title or author of an existing book (DEC-010): a mistaken entry is deleted and re-added.
- Cloud or remote databases; in-memory-only storage; import and export.
- Integration with Goodreads, Amazon or any book metadata service; automatic lookup of covers or authors.
- Book cover uploads and any image handling.
- Social features: reviews, comments, public lists.
- Mobile application (the web page works in a browser; no native app).
- Advanced search (text search, tags, sorting options, multi-criteria filters) and pagination in v1 (a spec change would add it).
- Reading dates other than `date_added` and `date_completed`, progress percentage, notes, reviews text, genres, series.
- Undo or restore of a deleted book, soft delete, history of changes.
- Setting a rating while adding a book; setting `date_completed` manually.
- Real-time sync between several browser tabs (last write wins; a reload shows the current state).
- Languages other than English; user-selectable themes.
- Books above 5,000 in one library (DEC-013).
- A second application instance on the same database file (DEC-032).

**Accepted limitations** (Sponsor, 2026-10-30; DEC-029, DEC-031):
- Protection of the list against other users of the same machine (R-16): the API is open to every local user.
- Visible natural-language instructions inside a title or author remain possible and may be read by tools that read the page (R-17). Hidden channels are reduced, not removed, by BR-006 (R-18): within the caps about 250 bytes of hidden data per field remain possible.
- BR-006 rejects every format character (category Cf) except U+200C and U+200D, so the left-to-right and right-to-left marks (U+200E, U+200F), the Arabic letter mark (U+061C), the soft hyphen (U+00AD) and the word joiner (U+2060) cannot be stored, also when they appear in text pasted from Arabic, Hebrew or PDF sources.
- Rule 6d rejects text that ends a word with U+200D or U+200C before a space or at the end (for example Malayalam written with the legacy chillu sequence; the atomic code points are accepted), and the duplicate key does not ignore joiners, filler characters or ignorable marks, so such text can bypass the advisory duplicate warning. U+1D159 counts as visible content although most fonts show no glyph.
- The address of the page takes no query string: a link such as `/?utm_source=x` gets `400 MALFORMED_REQUEST` (JSON) instead of the page (Section 5.8); `/` and `/index.html` with no query or an empty `?` work.
- Subdivision flag emoji (England, Scotland, Wales) cannot be stored because tag characters are rejected (BR-006).
- Confusable and mixed-script look-alike text is allowed; the duplicate key uses lower-casing, not case folding, so "Straße" and "STRASSE" are not duplicates and final-sigma forms can differ; both only affect an advisory warning.
- "Unassigned" means unassigned in Unicode 15.0; moving to another Unicode version is a spec change.
- HEAD and OPTIONS answer `405` although general-purpose HTTP servers are expected to support HEAD (DEC-027).
- A cross-site top-level navigation to `/` (a link on another site) carries `Sec-Fetch-Site: cross-site` and gets `403`; typing the address or using a bookmark works.
- Text escape-encoded in a more expanded form than the typical one can exceed 16,384 bytes and is then `413` (Appendix B).

## 7. Decision Log

Status values: CONFIRMED (Sponsor, with the date shown; items traceable to the brief, the constitution or `security.md` were already approved and are also confirmed), RESERVED (confirmed by the Sponsor as not specified now), PROPOSED (awaiting Sponsor confirmation; none remains in v0.4). Original drafting date of DEC-001 to DEC-024: 2026-10-30; the Sponsor answered handoff 007 on 2026-10-30 with "accept all recommendations", decided the open points of reviews 010 and 011 on 2026-10-30 (DEC-025 to DEC-032 in v0.3) and those of reviews 013 and 014 on 2026-10-30 (v0.4). "Amended v0.3" and "amended v0.4" mark entries changed by the round 2 and round 3 resolutions.

| ID | Date | Status | Decision | Rationale |
|---|---|---|---|---|
| DEC-001 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30); amended v0.3 | Duplicates are detected on title + author compared case-insensitively after trimming (brief), using the constitution's key (NFKC, locale-independent lower case) plus collapsing of internal white space runs, then trimming again; lower case means the Unicode default lower-case mapping, not case folding | Users capitalize and space differently; a missed warning is harmless but a spurious one is noise; the refinement is cheap and testable; the key must be deterministic (NFKC can add a leading space; case folding is out of reach of the stated key) |
| DEC-002 | 2026-10-30 | CONFIRMED (brief DEC-002; Sponsor 2026-10-30) | A rating can be changed after it was assigned | Opinions change |
| DEC-003 | 2026-10-30 | RESERVED (Sponsor 2026-10-30) | FR4 is not specified in this version; US-005 and BR-005 are reserved identifiers | Sponsor decision of 2026-10-30 (scope) |
| DEC-004 | 2026-10-30 | CONFIRMED (constitution; Sponsor 2026-10-30); amended v0.3 | Data is stored in a durable local file database | The BRD allows local storage and the constitution chooses a local file; the success metric "zero data loss on restart" excludes memory-only storage |
| DEC-005 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Duplicate override: the add request carries optional boolean `allow_duplicate`; the 409 carries the existing book in `details.existing_book` | Gives the UI dialog a mechanism, stays on one endpoint, keeps the first call free of side effects |
| DEC-006 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Status changes through `PUT /books/{id}/status`; any status may follow any status; same-status update is a successful no-op | FR2 needs an update path; restricting transitions has no business basis in the brief |
| DEC-007 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30, Q4) | Leaving `completed` clears rating and completion date in the same operation; the page asks for confirmation first when a rating is shown; the API needs no confirmation parameter | Invariants 1 and 4 force the clearing; a client-side dialog protects against accidental loss without a second API call |
| DEC-008 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30); refined by DEC-025 | Codes: `INVALID_STATUS`, `INVALID_RATING`, `RATING_NOT_ALLOWED` are HTTP 400 (BRD: 400 for bad ratings); `VALIDATION_ERROR` for title/author or several failing fields; wrong JSON types are `MALFORMED_REQUEST` | Keeps brief codes, uses the constitution's reserved codes, avoids a status code (422) outside the constitution list |
| DEC-009 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30); priority amended by DEC-030 | Operations added to complete "CRUD": `GET /books/{id}`, `DELETE /books/{id}`, `DELETE /books/{id}/rating`; DELETE returns 200 with the removed representation | BRD scope says CRUD; the brief template has no delete or get; 204 is not in the constitution's status list; clearing a rating needs an explicit mechanism |
| DEC-010 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30, Q2) | Title and author are not editable in v1 | Not in FR1 to FR4; editing re-triggers duplicate checks and expands the surface; delete and re-add covers the need |
| DEC-011 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30); amended v0.3 | No success response has a `message` member; `POST` returns the full book (201); `PUT` status and `PUT` rating return the book (the BRD example returns a `message`); rating cannot be given while adding | The page needs the changed book at once; FR1 lists title, author, status only; recorded deviation from the BRD example contract |
| DEC-012 | 2026-10-30 | CONFIRMED (brief lengths 1-500 and 1-200; constitution; SC-16; Sponsor 2026-10-30); amended by DEC-029 | Text policy of BR-006 | Limits in code points after trimming; characters rejected per security control SC-16 |
| DEC-013 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30, Q3) | Maximum 5,000 books; `409 LIBRARY_FULL`; list response bounded by the same number (worst case below 16,000,000 bytes, typical under 2 MB, Appendix B) | Constitution requires a maximum and a response bound (SC-17, R-02 not accepted); "thousands of books" capacity assumption |
| DEC-014 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Order: `date_added` descending, ties by `id` ascending | Timestamps have millisecond resolution and can collide; a total order makes tests deterministic |
| DEC-015 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Non-UUID id: `400 MALFORMED_REQUEST`; unknown id: `404 BOOK_NOT_FOUND` | Constitution reserves `MALFORMED_REQUEST` for invalid path values; security wants a clean envelope, never 500 |
| DEC-016 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Enums are case-sensitive; an invalid `status` filter is `400 INVALID_STATUS`; repeated or unknown query parameters are `400 MALFORMED_REQUEST`; `null` status on POST is invalid | Constitution: lower-case case-sensitive enums, no coercion, strict parsing |
| DEC-017 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | `details`: array of `{field, message}` for `VALIDATION_ERROR`, `INVALID_STATUS`, `INVALID_RATING`; object `{existing_book}` for `DUPLICATE_BOOK`; absent elsewhere; messages never echo input | Constitution leaves the shape to the spec; one shape per code lets clients branch |
| DEC-018 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Duplicate check and insert are one atomic step; capacity check and insert likewise; concurrent updates are last-write-wins; no optimistic locking | Risk R-06 accepted by the Sponsor with a concurrent-request test; single user |
| DEC-019 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Durability: acknowledged writes survive process termination; no promise for power loss | Meets "zero data loss on restart"; stronger guarantees cost performance and are not needed for a personal list |
| DEC-020 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Timing targets: single-book operation 300 ms, list API 1 s at 5,000 typical books, page list visible in 1 s at 1,000 books, measured once on the reference environment and recorded; add by keyboard with the actions of M-1 | BRD metrics "instantly" and "under 10 seconds" need numbers; measured once to avoid flaky tests |
| DEC-021 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30); amended v0.3, v0.4 | Frontend scope: a single page with US-010 to US-014 behavior and the error table of 5.9; v0.3 adds the page state model (US-010.4, US-010.10) and the exact UI wording that tests rely on; v0.4 completes the model (load-failure state, empty state after a delete, overlapping loads) and defines the page-level message area (US-013.4) | BRD puts the frontend in scope without criteria; two implementations of an undefined state model would pass different tests |
| DEC-022 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30, Q5); refined by DEC-030 | The exercise time budget (6 to 8 hours) is indicative; the cut line is MoSCoW | The exercise phases add up to 9.5 hours; the security and test requirements add work |
| DEC-023 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | `date_completed`: equals `date_added` on creation as completed, new time on each entry into completed, cleared on leaving | Keeps INV-4 true after any history |
| DEC-024 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Fixed order of evaluation of checks (BR-012; sub-orders) | Without it, tests with several faults are ambiguous |
| DEC-025 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30, as amended by the reviewers); amended v0.3 | Refinement of DEC-008: the spec's own value-domain codes `INVALID_STATUS` and `INVALID_RATING` (the latter named by the BRD) are used when a single status or rating value is of the right JSON type but not allowed; the reserved `VALIDATION_ERROR` is used for title and author and whenever two or more fields fail; the reserved `MALFORMED_REQUEST` is used for structure and JSON types; `RATING_NOT_ALLOWED` stays 400. Conditions met: tests T-34 to T-38, one sentence in BR-001 and BR-002, page mapping in US-011.6 and 5.9 | The constitution says a spec code with the same meaning takes precedence over the reserved code, and the BRD names the rating codes; one status class (400) is kept; the price is two possible codes for the `status` field, accepted because tested |
| DEC-026 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30, as amended) | US-014.1 names the standard (WCAG 2.1 A and AA rules, impact serious or critical) and the data state, not the checking tool | A tool name is a HOW detail owned by the constitution and `frontend-testing.md`; the criterion stays valid if the tool changes |
| DEC-027 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30, as amended); amended v0.3, v0.4 (dot segment defined, `Accept` `q` grammar and deviation from RFC 9110 specificity, white space around `charset`, pre-application statuses confined to container answers) | Wire-level rules settled for testability, now exact: path matching as received and id judged as received (BR-011, Section 5.0); `Content-Type` and `Accept` grammar; query grammar (also on the static paths); body faults; parser failures are `MALFORMED_REQUEST` and never `INTERNAL_ERROR`; `Allow` on every 405; no `Range`, conditional or upgrade handling; HEAD and OPTIONS are `METHOD_NOT_ALLOWED` (or the transport codes first); `charset` is optional on requests (the constitution writes `charset=UTF-8`) and the request grammar is stricter than HTTP in places (quoted `charset`, repeated header); HEAD is not supported although RFC 9110 expects it of general-purpose servers; sub-order of the transport checks | Without exact answers an implementer must guess and the security tests (SC-16, SC-29, SC-30, SC-31, SC-40) have no expected value; a smaller surface is chosen wherever HTTP leaves a choice |
| DEC-028 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30, as amended); amended v0.3, v0.4 (no views, triggers or virtual tables; formats and lengths checked; claim held for the whole run in US-009.7) | An unopenable or invalid database makes the application refuse to start and leaves the file untouched; "valid" is defined by observable facts (US-009.5); an untrustworthy data location also stops the start on POSIX (US-009.6) | A silent re-creation would destroy a reader's data; fail-closed matches the constitution's startup posture |
| DEC-029 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30); amended v0.4 | Text policy (BR-006 items 6 to 6d): U+200C and U+200D are allowed under a positional rule (not first, not last, not next to another joiner of either kind or to a Zs character); tag characters stay rejected and the loss of subdivision flag emoji is an accepted limitation; combining marks are capped per run, where a run counts marks and joiners together (at most 8 code points and at most one variation selector in a run, no leading mark); text that renders blank is rejected | Correct spelling of Persian, Urdu, Kurdish and several Indic scripts needs U+200C; invisible first or last characters, hidden-payload channels and layout overflow are cut (SC-16, R-17) |
| DEC-030 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30), final; amended v0.4 | Makes DEC-022 executable: the promotion of REQ-009 (`DELETE .../rating`) and REQ-012 (`GET /books/{id}`) to Must is confirmed because Must stories depend on them (US-001.9, US-008.1, US-012.4, 5.9); the cut order is the one stated in Section 1 (Delivery priorities). v0.4: the cut list removes only measurements; test E-22, the containment rules of the stylesheet, the 2-second add bound and the 300 ms parser bound are never cut (the residual risk R-19, "the cut list may drop security checks", is not accepted and is resolved by this change) | A cut line with Must items depending on Should items cannot be executed; the Sponsor chose what drops first and that security checks are never cut |
| DEC-031 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30); amended v0.4 | Residual risks accepted and stated: R-16 (other local OS users can use the open API), R-17 (visible instructions inside titles), R-18 (about 250 bytes of hidden data per field remain possible under the caps of BR-006), and the limits of Section 6 (confusables, lower-case instead of case folding, Unicode 15.0 fixed, cross-site navigation to `/`, a query string on the page address gets a JSON `400`, expanded escape forms over 16 KiB, rejected format characters other than U+200C and U+200D, legacy joiner sequences and the duplicate key's treatment of joiners) | Single-user local tool; the controls that are cheap were applied, the rest is stated |
| DEC-032 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | A second instance on the same database file is not supported; the second start fails (US-009.7) | The concurrency guarantees (BR-009, BR-013, INV-5, INV-7) hold for one running instance; refusing the second is simple and testable |

## Appendix A: Error catalog
Envelope: `error` (informative text), `code`, `timestamp` (fixed format), optional `details`. Text below is informative. There are no other codes. Answers that the server gives before the application sees the request (Appendix D, introduction) may use 400, 405, 414, 431, 501 or 505 with a body that is not an envelope; 414, 431, 501 and 505 occur nowhere else, and the application itself uses only the statuses of this table.

| Code | HTTP | Used when | `details` | Source |
|---|---|---|---|---|
| `VALIDATION_ERROR` | 400 | Title or author invalid; or two or more fields invalid on one request | array of 1 to 3 `{field, message}`, `field` in `title`, `author`, `status`, ordered in that sequence | constitution (reserved), BR-006, BR-012 |
| `INVALID_STATUS` | 400 | Status value not one of the three (body or filter) | array with exactly one entry, `field` `status` | BR-001 |
| `INVALID_RATING` | 400 | Rating not an integer literal 1 to 5, null or missing | array with exactly one entry, `field` `rating` | BR-002 |
| `RATING_NOT_ALLOWED` | 400 | Valid rating on a book that is not completed | none | BR-007 |
| `BOOK_NOT_FOUND` | 404 | Well-formed id with no book | none | BR-011 |
| `DUPLICATE_BOOK` | 409 | Add matches an existing book and no override | object `{existing_book}` | BR-003 |
| `LIBRARY_FULL` | 409 | Add when 5,000 books exist | none | BR-009 |
| `MALFORMED_REQUEST` | 400 | Bad JSON or any parser or decoder failure, empty or non-object body, unknown or duplicate member, wrong type, malformed id, bad query shape (including a query on the static paths and malformed percent-encoding in the query), body on GET or DELETE, unpaired surrogate, invalid UTF-8 | none | constitution (reserved), BR-012 |
| `INVALID_HOST` | 400 | Host not allowed | none | constitution |
| `FORBIDDEN` | 403 | Origin or Fetch-Metadata not allowed | none | constitution |
| `NOT_FOUND` | 404 | Unknown or not routable path, static path not served | none | constitution |
| `METHOD_NOT_ALLOWED` | 405 | Method not defined for the path (with `Allow`) | none | constitution |
| `NOT_ACCEPTABLE` | 406 | `Accept` does not accept `application/json` on an API path | none | constitution |
| `PAYLOAD_TOO_LARGE` | 413 | Body over 16 KiB | none | constitution |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | `Content-Type` missing or outside the grammar of Section 5.0 (methods other than GET and HEAD) | none | constitution |
| `INTERNAL_ERROR` | 500 | Unexpected failure; generic text | none | constitution |

Informative texts: `VALIDATION_ERROR` "The request contains invalid values."; `INVALID_STATUS` "Status must be unread, reading or completed."; `INVALID_RATING` "Rating must be an integer from 1 to 5."; `RATING_NOT_ALLOWED` "Only completed books can be rated."; `BOOK_NOT_FOUND` "Book not found."; `DUPLICATE_BOOK` "A book with the same title and author already exists."; `LIBRARY_FULL` "The library is full."; `INTERNAL_ERROR` "Unexpected error." Detail messages (the fixed list from which the API chooses; informative like the other texts): "is required", "must not be empty", "must be between 1 and 500 characters" (author: 200), "contains characters that are not allowed", "must be one of: unread, reading, completed", "must be an integer from 1 to 5". No message includes user input, member names other than the API `field`, class names or paths.

## Appendix B: Limits
| Item | Limit | Derivation / behavior |
|---|---|---|
| Title | 1 to 500 code points | BR-006 |
| Author | 1 to 200 code points | BR-006 |
| Request body | 16 KiB (16,384 bytes), counted on the bytes as sent | constitution. A typical valid body is under 9 KiB (title of 500 and author of 200 characters outside the basic plane written as escape pairs: 700 x 12 bytes = 8,400 bytes plus structure). Valid text written in a more expanded form can be larger: 500 Hangul syllables written as three escaped decomposed letters each take 9,000 bytes for the title alone and about 12.7 KB with a full author, and one counted code point such as U+1F82 written as four escaped code points takes 24 bytes, so 700 of them (16,800 bytes) are answered `413` although the counted length is valid; this is a stated limit. Any valid body padded with white space to exactly 16,384 bytes is accepted |
| Number literal in a request | at most 100 characters as written | DEC-027 |
| Combining marks | a run of marks and joiners has at most 8 code points and at most one variation selector | BR-006 item 6c |
| Books in the library | 5,000 | BR-009 (DEC-013) |
| Entries in a list response | 5,000 | same as the library |
| One book in a response | at most about 3,100 bytes (2,800 bytes of text at 4 bytes per code point, about 300 of structure) | derived |
| Largest list response | below 16,000,000 bytes (5,000 x 3,100 = 15.5 MB, text written as UTF-8 and not as escape sequences; control characters are banned so no 6-byte escapes occur) ; typical under 2 MB (about 400 bytes per book) | derived for the Security review (SC-17); the test asserts the limit |
| Query parameters | only `status`, once, on the list endpoint | US-002.7 |
| Ids | 36-character lowercase UUID | BR-011 |
| Error details | at most 3 entries; `existing_book` is one book | Appendix A |
| Add at 4,999 books of maximum size | an add of a book that matches none completes within 2 seconds; an add that duplicates a stored book is answered `409 DUPLICATE_BOOK` within 2 seconds; neither is `500` (at 5,000 books `LIBRARY_FULL` is answered before the duplicate check, so 4,999 is the costly case) | M-2, C-11 |

## Appendix C: Traceability matrix

Functional tests for acceptance criteria that Appendix D does not list (for example the normalization examples of US-004.5, the same-state updates of US-006.5, the ties of US-002.2, US-008.3) are specified in Phase 3 from the acceptance criteria (M-5); UI stories are verified by page and E2E tests specified in Phase 3.

| FR / BRD rule | Requirements | User stories | Business rules | Endpoints | UI stories | Appendix D cases |
|---|---|---|---|---|---|---|
| FR1 Add Book | REQ-001, 010, 011, 016, 025, 026, 027, 051 | US-001, US-004 | BR-001, BR-003, BR-006, BR-008, BR-009 | `POST /api/v1/books` | US-011 | T-01 to T-13, T-16, T-21 to T-24, T-24b, T-25 to T-30, T-34, T-35, T-39, T-40, C-01 to C-04, C-06 to C-11, S-01 |
| FR1 / scope: view library | REQ-005, 006, 007, 012, 015, 028, 029 | US-002, US-007 | BR-010, BR-011 | `GET /api/v1/books`, `GET /api/v1/books/{id}` | US-010 | T-14, T-14b, T-15, T-20, C-03, C-05, E-06, E-24, E-24b, E-25 to E-27, S-06 |
| FR2 Track Status | REQ-002, 017, 022, 023, 024, 048 | US-006 | BR-001, BR-004, BR-008 | `PUT /api/v1/books/{id}/status` | US-012 | T-19, T-38, S-02 to S-04, S-08, S-09, S-11 |
| FR3 Rate Books | REQ-003, 008, 009, 017, 020, 021 | US-003 | BR-002, BR-007, BR-004 | `PUT`, `DELETE /api/v1/books/{id}/rating` | US-012 | T-17, T-18, T-31, T-32, T-36, T-37, S-01, S-02, S-05, S-08, S-09 |
| FR4 Author Biography | REQ-004 | US-005 (reserved) | BR-005 (reserved) | none (reserved, DEC-003) | none (reserved) | none |
| CRUD: delete | REQ-013 | US-008 | BR-011 | `DELETE /api/v1/books/{id}` | US-012 | S-06, S-09, C-03, C-08 |
| CRUD: edit (decided out) | REQ-014 | none (Won't, DEC-010) | none | none | US-012.9 | T-16 (unknown members) |
| BRD Rating Integrity | REQ-020, 023 | US-003, US-006 | BR-007, BR-004 | PUT rating, PUT status | US-012 | S-01, S-02, S-08, S-09, T-36 |
| BRD Validation Range | REQ-021 | US-003 | BR-002 | PUT rating | US-012 | T-17, T-18, T-31, T-32, T-37 |
| BRD Data Integrity (duplicates) | REQ-010, 011, 031 | US-004 | BR-003, BR-013 | POST | US-011 | C-07, C-10, T-40 |
| BRD Explicit error handling | REQ-030, 032, 040, 049, 051, 053 | all | BR-012, Appendix A, Appendix D | all | US-013 | E-01 to E-39, T-34 to T-39, S-07 |
| Success metric: zero data loss | REQ-033, 052, 054, 055 | US-009 | BR-013 | all writes | n/a | C-12, C-13, S-10 |
| Success metric: add in under 10 s | REQ-034 | US-011 | n/a | POST | US-011 | Phase 3 (E2E) |
| Success metric: instant library | REQ-035 | US-002 | BR-009 | GET list | US-010 | C-05, C-11 |
| Constitution: frontend standards | REQ-018, 019, 039, 056, 057 | US-013, US-014 | n/a | n/a | US-013, US-014 | Phase 3 (page and E2E tests) |
| Boundaries: capacity / response bound | REQ-027, 036, 050 | US-001, US-002 | BR-009 | POST, GET list | US-011 | C-01 to C-11 |
| Security controls SC-16, SC-17, SC-29, SC-30 | REQ-026, 036, 040, 051, 053 | US-001, US-002, US-013 | BR-006, BR-009, BR-012 | all | US-014 | T-01 to T-40 (with T-14b, T-24b), C-01 to C-13, E-01 to E-39 (with E-24b) |
| Constraints (constitution, BRD out of scope) | REQ-037, 038, 041 to 047 | all | n/a | n/a | n/a | none (checked by review and the Definition of Done) |

Gaps: the plan section and task columns are filled in Phase 3. Every endpoint has a story; every story is reachable from an FR, a BRD rule or a logged decision; FR4 has no downstream items by design (DEC-003). The start-up criteria US-009.3, US-009.5 and US-009.6 have no Appendix D row; Phase 3 specifies their fixtures (absent, empty, foreign, damaged, invariant-violating files; symbolic link; wrong mode or owner).

## Appendix D: Security test cases with expected results

Expected values for the security tests (`security.md` SC-16, SC-17, SC-29, SC-30, SC-31, SC-40). Requests use `Content-Type: application/json`, the allowed `Host` and no `Origin` unless stated. "Real port" means the case is also run against the application on its real port (SC-30). **Raw-socket cases** (a normal HTTP client cannot produce them): T-05, T-28, T-29 (bytes), T-31 (bodies), T-33, T-39 (body on GET and DELETE), E-09 (chunked), E-10 (HTTP/1.0, duplicate Host), E-15 (rows with `..`, `%` or `;`), E-18, E-20, E-22, E-24, E-24b, E-25, E-26, E-28, E-31 to E-33, E-34 (two `Content-Type` headers), E-36, E-37, E-38 (body on GET). Any other row whose request target contains a dot segment, an invalid escape or a character that is not allowed in a URL is sent verbatim on a raw socket, because a client library would normalize or refuse it. Every other row may use a normal HTTP client.

**Envelope check:** every error response of D.1 to D.4 that the application produces has the members exactly `error`, `code`, `timestamp` (plus `details` only where Appendix A allows) and `Content-Type` JSON with UTF-8; every response that the application produces, error or success, carries the security headers of the constitution (`Content-Security-Policy` as specified there, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Cross-Origin-Opener-Policy`, `Cross-Origin-Resource-Policy`, `Permissions-Policy`, `Cache-Control: no-store`), no `Server` product or version, no `Access-Control-*`, no `Set-Cookie`, no `Location`; no error text contains a stack trace, SQL, class name, path or echo of the input.

**Pre-application answers** are the cases the server rejects while parsing the request line or headers, before the application sees the request (footnote: `security.md` R-13). For them the status is 400, 405, 414, 431, 501 or 505 (414, 431, 501 and 505 are the statuses outside Appendix A; a pre-application 405 need not carry `Allow`), the body need not be an envelope or carry the security headers, but it contains no product, no version and no echo of the request, and the application keeps serving. They are limited to: HTTP/1.0 or 1.1 without `Host`, duplicate `Host` headers, an invalid request line, an over-long line or header, both `Content-Length` and `Transfer-Encoding`, a bad `Content-Length`, an invalid method token, a method token the server refuses itself (TRACE, unknown or lower-case names), a malformed percent-encoding in the path or query where the server rejects it first, and an encoded `/` or `\`, a literal `\` or a dot segment that leaves the root, where the server rejects it first.

### D.1 Text, identifiers and values (SC-16, SC-31)
| # | Request | Expected |
|---|---|---|
| T-01 | POST title `a\u0000b`, valid author | 400 `VALIDATION_ERROR`, `details[0].field` `title` |
| T-02 | POST title containing a line feed, then one containing a tab, in the middle | 400 `VALIDATION_ERROR`, field `title` (each) |
| T-03 | POST title with U+200B inside; with U+202E; with U+E000; with U+0378; with U+E0041 | 400 `VALIDATION_ERROR`, field `title` (each) |
| T-04 | POST title `"\uD800"` (unpaired surrogate) | 400 `MALFORMED_REQUEST`, no `details` |
| T-05 | POST body with the byte 0xFF inside a string | 400 `MALFORMED_REQUEST` |
| T-06 | POST title of 500 code points U+1F600; then 501 | 201; then 400 `VALIDATION_ERROR` field `title` |
| T-07 | POST title of 500 decomposed "é" (1,000 code points as sent) | 201, title returned with 500 code points |
| T-08 | POST title `"  Clean Code  "` and one with leading U+00A0 | 201, returned titles trimmed |
| T-09 | POST title a single U+00A0; title `""`; title `null`; no title | 400 `VALIDATION_ERROR`, field `title` (each) |
| T-10 | POST title with U+200D inside an emoji sequence | 201 |
| T-11 | POST with a title of 16,000 characters (within 16 KiB) | 400 `VALIDATION_ERROR` (not a parse error) |
| T-12 | POST title `'; DROP TABLE books;--`, then `<script>alert(1)</script>`, then `${7*7}`, then `#{7*7}`, `%s%n` | 201 each; later GET returns each text unchanged; the number of books grows by exactly one each time |
| T-13 | POST title 501 characters containing `${7*7}` | 400 `VALIDATION_ERROR`; no message contains `${7*7}` or `49` |
| T-14 | Where `{bad}` is each of: the id in upper case, `not-a-uuid`, `1%27%20OR%20%271%27%3D%271`: GET and DELETE on `/api/v1/books/{bad}`; PUT on `/api/v1/books/{bad}/status` and on `/api/v1/books/{bad}/rating`; DELETE on `/api/v1/books/{bad}/rating` (valid JSON body where required) | 400 `MALFORMED_REQUEST` (each) |
| T-14b | GET `/api/v1/books/not-a-uuid/status` | 405 `METHOD_NOT_ALLOWED` (route and method before the id) |
| T-15 | GET `/api/v1/books/00000000-0000-4000-8000-000000000000` | 404 `BOOK_NOT_FOUND` |
| T-16 | POST with `id`, `date_added`, `date_completed` or `rating` member; with `Title`; with duplicate `title` member; with `{"title":"a","author":"b"}x` | 400 `MALFORMED_REQUEST` (each) |
| T-17 | PUT rating with `4.9`, `5.0`, `1e0`, `0`, `6`, `-0`, `null`, `{}`, `99999999999999999999999` | 400 `INVALID_RATING`, field `rating` (each) |
| T-18 | PUT rating with `"5"`, `true`, `[5]`, `{"a":1}`, `01`, `+5`, a number of 101 characters | 400 `MALFORMED_REQUEST` (each) |
| T-19 | PUT status with `"Completed"`, `""`, `null`, `{}`; with `5` or `true` | 400 `INVALID_STATUS` (the first four); 400 `MALFORMED_REQUEST` (the last two) |
| T-20 | GET list with `?status=READING`, `?status=`, `?status`; with `?status=a&status=b`, `?foo=1`, `?Status=reading` | 400 `INVALID_STATUS` (first three); 400 `MALFORMED_REQUEST` (last three) |
| T-21 | POST allow_duplicate `"true"`, `1`, `null` | 400 `MALFORMED_REQUEST` (each) |
| T-22 | POST title of 300 times U+1D15E (NFC gives 600 code points) | 400 `VALIDATION_ERROR`, field `title` (count after NFC) |
| T-23 | POST title U+3164 only; U+2800 only; U+0301 only; U+0301 followed by "abc"; U+FE0F only; and, as the accepted row, "ab", U+3164, "cd" | 400 `VALIDATION_ERROR` field `title` (the first five); 201 (the last: a blank filler between visible characters is allowed) |
| T-24 | POST title "x" + 9 times U+0301; "x" + 8 times U+0301; "a" + U+E0100 + U+E0101; "a" + U+E0100 (the base is "x" because NFC composes "e" with the first U+0301, which would leave 8 marks) | 400 field `title`; 201; 400 field `title`; 201 |
| T-24b | POST title: "x" + 8 x U+0301 + U+200D + U+0301 (run of 10); "x" + (8 x U+0301 + U+200D) x 50 + "y"; "a" + U+FE0F + U+200D + U+FE0F; "a" + U+0301 + U+FE0F + U+0301 + U+FE0F; then, accepted: "x" + 7 x U+0301 + U+200D + "y" (run of 8); U+2764 + U+FE0F + U+200D + U+1F525 | 400 `VALIDATION_ERROR` field `title` (the first four); 201 (the last two) |
| T-25 | POST title (each failing row places the joiners inside the text, never first or last, except the rows that test first or last) with U+200C between two Persian letters ("می‌خواهم"); U+200C first; U+200C last; U+200C followed by U+200D; U+200D followed by U+200D; a joiner next to a space | 201 (first); 400 `VALIDATION_ERROR` field `title` (the other five) |
| T-26 | POST title with U+FFFD between letters; title U+FFFE; U+FDD0; U+0378 | 201; 400 field `title` (each of the other three) |
| T-27 | POST with the title being the text backslash, u, D83D, backslash, u, DE00 (12 bytes written verbatim in the JSON text, not the decoded character), repeated 500 times; the reversed pair `\uDE00\uD83D`; `\uD83D` followed by a letter | 201 (title of 500 code points); 400 `MALFORMED_REQUEST` (each of the others) |
| T-28 | POST body bytes `C0 80`, `ED A0 80`, `F4 90 80 80`, `E2 82` (truncated) inside a string | 400 `MALFORMED_REQUEST` (each); the stored library unchanged |
| T-29 | POST body starting with the byte order mark `EF BB BF`; with `/*x*/`; with a trailing comma; with single-quoted strings; with unquoted member names; with a raw line feed (0x0A) inside a string | 400 `MALFORMED_REQUEST` (each) |
| T-30 | `{"title":"a","title":"b","author":"c"}`; a body of the same shape whose second member name is written as backslash, u, 0074 followed by the letters itle (so it equals title only after unescaping: the duplicate exists only after unescaping); `{"title":"a","author":"b","allow_duplicate":true,"allow_duplicate":false}` | 400 `MALFORMED_REQUEST` (each; duplicates are detected after unescaping) |
| T-31 | PUT rating body `{"rating":` followed by 8,000 nested `[` and the matching `]` (inside 16 KiB); a body of 2,000 nested `{"x":` objects as an unknown member value (inside 16 KiB); `{"rating":1e999999999}`; `{"rating":1e-999999999}`; `{"rating":0.0000000000000000000000000000000000000001}` | 400 `MALFORMED_REQUEST` (the two nested bodies); 400 `INVALID_RATING` (the three numbers); each answered within 300 ms, never 500 |
| T-32 | PUT rating `NaN`, `Infinity`, `5.`, `.5`, `0x5`, `5e`; `1E1`; PUT rating `{"status":"completed","rating":5}`; `{"rating":5,"status":"completed"}`; `{"rating":4,"rating":5}` | 400 `MALFORMED_REQUEST` (the six invalid numbers and the three bodies); 400 `INVALID_RATING` (`1E1`: a valid JSON number that is not an integer literal) |
| T-33 | Hostile text for the log: a path id with `%0d%0a`, `%e2%80%a8`, `%1b[2J`; `?status=` with the same; a title with the same characters; each in a failing and in a succeeding request | the response is as in T-14, T-20, T-12; the captured log has one line per event, no raw CR, LF, U+2028, U+2029, U+0085 or ESC, and no title text |
| T-34 | POST `{"title":"","author":"a","status":"Done"}` | 400 `VALIDATION_ERROR` with `details` entries `title` then `status` |
| T-35 | POST `{"title":"a","author":"b","status":"Done"}` | 400 `INVALID_STATUS`, one `details` entry `status` |
| T-36 | PUT rating `7` on an unread book; then PUT rating `4` on the same book | 400 `INVALID_RATING` (not `RATING_NOT_ALLOWED`); then 400 `RATING_NOT_ALLOWED` |
| T-37 | PUT rating with an integer literal of exactly 100 digits; with 101 digits | 400 `INVALID_RATING`; 400 `MALFORMED_REQUEST` |
| T-38 | PUT status on an unknown well-formed id with body `{"status":"Done"}` | 400 `INVALID_STATUS` (field values before existence) |
| T-39 | POST with a zero-byte body; PUT status with a zero-byte body; POST bodies `null`, `[]`, `"x"`, `5`; GET `/api/v1/books` with a body of one space; DELETE `/api/v1/books/{id}` with a body of one space | 400 `MALFORMED_REQUEST` (each) |
| T-40 | POST "Straße" by "a", then "STRASSE" by "a"; POST "Clean Code" by "b", then "CLEAN CODE" by "b" | 201, 201; 201, then 409 `DUPLICATE_BOOK` |

### D.2 Capacity, response size and concurrency (SC-17)
| # | Scenario | Expected |
|---|---|---|
| C-01 | 5,000 books exist; POST a valid new book | 409 `LIBRARY_FULL`, no `details`, count stays 5,000 |
| C-02 | 5,000 books exist; POST a duplicate with `allow_duplicate: true`; POST a duplicate without it | 409 `LIBRARY_FULL` (both) |
| C-03 | 5,000 books exist; GET list; PUT status to `completed` on an unread book; PUT rating 4 on that book; DELETE one book | 200 with `total` 5000; 200; 200; 200 |
| C-04 | after the delete of C-03, POST a valid book; then POST another | 201; then 409 `LIBRARY_FULL` |
| C-05 | 5,000 books with title 500 and author 200 code points outside the basic plane; GET list (on the real port; the time is recorded, no gate) | 200, `total` 5000, body below 16,000,000 bytes |
| C-06 | two simultaneous POSTs of different valid books with 4,999 books | one 201, one 409 `LIBRARY_FULL`; 5,000 books after |
| C-07 | two simultaneous identical POSTs without `allow_duplicate` | one 201, one 409 `DUPLICATE_BOOK`; one book stored |
| C-08 | a loop of 20 add and delete pairs at 4,999 books | every response 201 or 200; the count never exceeds 5,000 |
| C-09 | 4,990 books; 20 concurrent POSTs of different valid books | exactly 10 times 201 and 10 times 409 `LIBRARY_FULL`; 5,000 books after |
| C-10 | 20 concurrent identical POSTs without `allow_duplicate` (empty library); then 20 concurrent identical POSTs with `allow_duplicate: true` | 1 time 201 and 19 times 409 `DUPLICATE_BOOK`; then 20 times 201 (21 books) |
| C-11 | 4,999 books of maximum size: (a) one POST of a book that matches none; (b) DELETE of that book, then one POST that duplicates a stored book without `allow_duplicate`; then, with 4,949 books of maximum size, (c) 50 concurrent POSTs of different valid books together with 50 concurrent list requests | (a) 201 within 2 s; (b) 200, then 409 `DUPLICATE_BOOK` within 2 s; (c) all 50 POSTs 201 and all 50 lists 200 (4,999 books after), no response with an undocumented status, none a 500 or a refused connection, a failed request changed nothing, the process is still running and serves normally afterwards |
| C-12 | A second instance started on the database file of a running instance | the second start fails with a non-zero exit status (US-009.7); the first keeps serving and its data are unchanged |
| C-13 | Terminate the process forcibly right after each of POST, PUT status, PUT rating, DELETE returns 2xx (50 iterations of a loop with a restart) | after restart the acknowledged change is present (US-009.2, M-3) |

### D.3 Error paths, routing and headers (SC-29, SC-40, also run on a real port for SC-30)
| # | Request | Expected |
|---|---|---|
| E-01 | GET `/api/v1/unknown`; GET `/api/v1/books/`; GET `/api/v1/books/{id}/`; GET `/api/v1/books/{id};x=1` | 404 `NOT_FOUND` (each) |
| E-02 | GET `/api/v1/books/{id}/status`; GET `/api/v1/books/{id}/rating`; PUT or DELETE `/api/v1/books`; POST, PUT or PATCH `/api/v1/books/{id}` | 405 `METHOD_NOT_ALLOWED` (each, with the JSON content type for non-GET) |
| E-03 | HEAD `/api/v1/books` | 405 (no body to assert) |
| E-04 | OPTIONS `/api/v1/books` with `Origin: http://evil.example` and `Access-Control-Request-Method: POST` | 403 `FORBIDDEN`, no `Access-Control-*` header |
| E-05 | OPTIONS `/api/v1/books` with no `Origin` and no content type; then with `Content-Type: application/json` | 415 `UNSUPPORTED_MEDIA_TYPE`; then 405 `METHOD_NOT_ALLOWED` |
| E-06 | GET `/api/v1/books` with `Accept: text/xml`; with `Accept: application/json`; with no `Accept`; with `*/*` | 406 `NOT_ACCEPTABLE`; 200; 200; 200 |
| E-07 | POST with `Content-Type: text/plain`; none; `application/jsonx`; `application/json; charset=ISO-8859-1`; DELETE `/api/v1/books/{id}` with no content type | 415 `UNSUPPORTED_MEDIA_TYPE` (each) |
| E-08 | POST with `Content-Type: application/json;charset=utf-8` and a valid body | 201 |
| E-09 | POST with a body of 16,384 bytes (valid JSON padded with spaces); with 16,385 bytes; a chunked body crossing 16,384 bytes; a 16,385-byte body to an unknown path | 201; 413 `PAYLOAD_TOO_LARGE`; 413; 413 |
| E-10 | Host `evil.example`; `localhost` without port; `localhost:<other port>`; `[::1]:<port>`; one header `127.0.0.1:<port>, evil.example`. Then, as pre-application answers: no Host (HTTP/1.0); two Host headers | 400 `INVALID_HOST` (exactly, from the application, for the first five); status 400 or 505, no disclosure, application keeps serving (the last two) |
| E-11 | Host `LOCALHOST:<port>`; `127.0.0.1:<port>`; `localhost:<port>` | 200 on GET `/api/v1/books` |
| E-12 | `Origin: http://evil.example`; `Origin: null`; `Origin: http://localhost:<other port>` on POST and on GET | 403 `FORBIDDEN` (each) |
| E-13 | `Origin: http://localhost:<port>` or `http://127.0.0.1:<port>` or none; `Sec-Fetch-Site: same-origin` or `none` | accepted (normal result) |
| E-14 | `Sec-Fetch-Site: cross-site`; `same-site` | 403 `FORBIDDEN` |
| E-15 | GET `/data/globerbooks.db`; `/js/`; `/css/`; `/error`; `/actuator/health`; `/h2-console`; `/WEB-INF/web.xml`; `/.git/config`; `/.env`; a path to a `*.test.js` file; `/%2e%2e/%2e%2e/etc/passwd`; `/..%2f..%2fetc/passwd`; `/index.html;jsessionid=1`; `/index.html/` | 404 `NOT_FOUND` (each) with the standard envelope. A pre-application answer (status 400, no product, no version, no echo) is accepted for a row that contains an encoded `/` or `\`, a literal `\`, `%00`, or a dot segment that leaves the root |
| E-16 | GET `/` and GET `/index.html` | 200, HTML page; the headers of the constitution are present |
| E-17 | POST `/` with `Content-Type: application/json` | 405 `METHOD_NOT_ALLOWED` |
| E-18 | GET `/api/v1/books` with a body; DELETE `/api/v1/books/{id}` with the body `{}` | 400 `MALFORMED_REQUEST` (each) |
| E-19 | GET `/api/v1/books/{id}?x=1` | 400 `MALFORMED_REQUEST` |
| E-20 | A request line above 8 KiB; one header above 8 KiB; both `Content-Length` and `Transfer-Encoding` | pre-application answer: status 400, 414 or 431; no server product or version in the response; the application keeps serving |
| E-21 | A request that makes the application fail unexpectedly (how it is provoked is defined in the plan; the way of provoking it does not exist in the shipped application, and a request to it there gets 404 `NOT_FOUND`) | 500 `INTERNAL_ERROR`, text "Unexpected error.", no detail; the application keeps serving |
| E-22 | A connection that sends no complete request within 5 seconds | the server closes it (constitution connection timeout); other requests are served meanwhile |
| E-23 | Every response above that the application produces | passes the envelope check stated at the top of this appendix, including the security headers |
| E-24 | GET `/api/v1/books/%33f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b` (any `%` in the id segment); GET `/api/v1/%62ooks`; GET `/api/v1/books/%2e%2e` | 400 `MALFORMED_REQUEST`; 404 `NOT_FOUND`; 404 `NOT_FOUND` (an encoded dot segment is not routable). A pre-application answer (status 400, no product, no version, no echo) is accepted for a row that contains an encoded `/` or `\`, `%00`, or a dot segment that leaves the root |
| E-24b | GET `/api/v1/books/.%2E`; GET `/api/v1/books/<valid id>%2f`; `<valid id>%2F`; `<valid id>%5c`; `<valid id>%3b`; `<valid id>\x` (literal backslash, sent verbatim) | 404 `NOT_FOUND` (each). A pre-application answer (status 400, no product, no version, no echo) is accepted for a row that contains an encoded `/` or `\`, a literal `\`, or a dot segment that leaves the root |
| E-25 | GET `/api/v1/books?status=%zz`; GET `/api/v1/books?status=%FF`; GET `/api/v1/books/%zz`; GET `/api/v1/books/%`; GET `/%00` | 400 `MALFORMED_REQUEST` (the first four); 404 `NOT_FOUND` (the last). A pre-application answer (status 400, no product, no version, no echo) is accepted for any of these rows |
| E-26 | GET `/api/v1/books?`; `?&`; `?status=reading&`; `?status=reading;x=1`; `?status[]=reading`; `?%73tatus=reading`; `?status=%72eading`; `?status=reading+` | 200 all books (empty query); 400 `MALFORMED_REQUEST`; 400 `MALFORMED_REQUEST`; 400 `INVALID_STATUS` (the value is `reading;x=1`); 400 `MALFORMED_REQUEST`; 200 filtered; 200 filtered; 400 `INVALID_STATUS` |
| E-27 | POST `/api/v1/books/not-a-uuid`; GET `/api/v1/books/not-a-uuid/status`; GET `/api/v1/books/not-a-uuid/other` | 405; 405; 404 `NOT_FOUND` (route and method first, id format second) |
| E-28 | TRACE `/api/v1/books` with the allowed Host and JSON content type; TRACE without content type; a request with method `FOO`; method `get` (lower case) | 405 `METHOD_NOT_ALLOWED` (or a pre-application answer with status 400, 405 or 501), never a body that echoes the request, never a product or version; 415 `UNSUPPORTED_MEDIA_TYPE` for the second (content type first) |
| E-29 | The 405 responses of E-02, E-03, E-05, E-17 | carry `Allow` listing exactly the methods defined for that path (for `/api/v1/books`: `GET, POST`; for `/`: `GET`) and never HEAD or OPTIONS |
| E-30 | GET `/` with `Range: bytes=0-5`; with `Range: bytes=99999999-`; with `If-None-Match: *`; with `If-Modified-Since: <a future date>`; the response headers of GET `/` | 200 with the full body each time (no 206, 304 or 416); no `ETag`, `Last-Modified` or `Accept-Ranges` header |
| E-31 | GET `/` with `Upgrade: websocket` and `Connection: Upgrade`; with `Upgrade: h2c`; POST with a 16,385-byte `Content-Length` and `Expect: 100-continue` | 200 page (no 101); 200; 413 `PAYLOAD_TOO_LARGE` and no `100 Continue` was sent |
| E-32 | A chunked body of 16,385 bytes to an unknown path; to `GET /api/v1/books`; to `DELETE /api/v1/books/{id}` | 413 `PAYLOAD_TOO_LARGE` (each) |
| E-33 | A request rejected early (415) that declares `Content-Length: 10485760` of which 1 MiB is sent | the response arrives within 2 seconds; the server may close the connection; other requests are served meanwhile |
| E-34 | `Content-Type`: `Application/JSON`; `application/json ; charset=utf-8`; `application/json; charset="utf-8"`; `application/json; charset=utf-8; foo=bar`; `application/json; boundary=x`; `application/json, text/plain`; two `Content-Type` headers (on a POST with a valid body) | 201; 201; 415; 415; 415; 415; 415 |
| E-35 | `Accept` on `GET /api/v1/books`: `application/json;q=0`; `text/html,application/json;q=0.5`; `text/xml, */*;q=0.1`; `*/*;q=0`; `;;;`; `text/html, application/json;q=0`; empty value; `application/json;q=1.5`; `application/json;Q=0`; `text/html,,application/json`; `application/json;q=0, */*` | 406; 200; 200; 406; 406; 406; 200; 406; 406; 200; 200 |
| E-36 | Request line above 8 KiB; one header above 8 KiB; 2,000 query pairs `a=b&` (inside 8 KiB) on the list | pre-application answer (status 400, 414 or 431) for the first two; 400 `MALFORMED_REQUEST` for the third |
| E-37 | Invalid request line (`GET  /  HTTP/1.1` with two spaces, `GET / HTTP/9.9`, a target containing a space); HTTP/1.1 without `Host`; two `Content-Length` headers; `Content-Length: -1`; `Content-Length` plus `Transfer-Encoding` | pre-application answer: status 400 (505 for the version), no product, no version; the application keeps serving |
| E-38 | GET `/?x=1`; GET `/index.html?`; GET `/` with a body of one space; GET one of the page's asset paths with `?x=1` | 400 `MALFORMED_REQUEST`; 200; 400 `MALFORMED_REQUEST`; 400 `MALFORMED_REQUEST` |
| E-39 | GET `/favicon.ico` | 404 `NOT_FOUND` in the envelope |

### D.4 Sequences (invariants)
| # | Sequence | Expected |
|---|---|---|
| S-01 | Add (unread); PUT rating 4 | 400 `RATING_NOT_ALLOWED`; book unchanged |
| S-02 | Add completed; PUT rating 4; PUT status reading; GET | rating null, date_completed null |
| S-03 | S-02 then PUT status completed | date_completed is the new time, rating null |
| S-04 | Add completed; PUT status completed again | 200, date_completed unchanged |
| S-05 | Add completed; PUT rating 5; DELETE rating; DELETE rating | 200, rating null; 200, rating null |
| S-06 | Add; DELETE; DELETE again | 200 with the book; 404 `BOOK_NOT_FOUND` |
| S-07 | Any failed request in D.1 to D.3 | the library content is identical before and after |
| S-08 | A completed book with rating 3; run PUT status `reading` and PUT rating 4 concurrently, 200 times (a fresh book each time) | the final state is always status `reading`, rating null, date_completed null; the status call is 200; the rating call is 200 or 400 `RATING_NOT_ALLOWED`; never a non-null rating with status `reading` (INV-1) |
| S-09 | A book with status `reading`; PUT status `completed`, DELETE rating and DELETE of the book concurrently on that book | every response is 200 or 404 `BOOK_NOT_FOUND`; INV-1 and INV-4 hold in every final state |
| S-10 | A failure is forced after the first of the two changes of BR-004 (how is defined in the plan) | 500 `INTERNAL_ERROR`; the book is exactly as before (status, rating, date_completed) |
| S-11 | PUT status `unread` on an unread book; PUT status `reading` on a reading book | 200 each; no field changed |

## Changelog
- v0.4.1 (2026-10-30): Phase 2 closing edit after the final diff checks (handoffs 016 and 017). Security D-1: Appendix D rows T-27 (row 1) and T-30 (row 2) rewritten in words and plain ASCII, with no decoded character. Liaison O-1 (same), O-2 (Section 1: "Only the items above are cut, and no security item.", same change in `requirements.md`), O-3 (Accept grammar: a `q` parameter without `=` is unparseable). No behavior, expected status or code changed.
- v0.4 (2026-10-30): Round 3 of Phase 2 (final diff-check version; the spec is frozen after the gate and later findings are change requests). Applied the Liaison round 2 findings N-01 to N-12 (handoff 013, P-1 to P-12) and the Security round 2 conditions and findings SEC-F-057 to SEC-F-065 (handoff 014); the finding-by-finding table is in handoff 015. Sponsor decisions of 2026-10-30 recorded: E-22 stays a required test and the cut list removes only measurements (never the stylesheet containment rules, the 2-second add bound or the 300 ms parser bound), DEC-030 final with REQ-009 and REQ-012 as Must, R-18 accepted, R-19 not accepted and resolved by the cut-list change, usability limits and the JSON `400` for `/?utm_source=x` recorded in Section 6, the rejection of other format characters accepted as a limitation. Changes: mark runs count joiners (BR-006 6c, T-24b) and joiner adjacency is exact (6d); M-2, Appendix B and C-11 restated at 4,999 books; encoded and dot segment definitions and the id exception in 5.0, BR-011, US-007.2; `Accept` `q` grammar and `charset` white space; asset set of 5.8; page state model and message area (US-010.4, .6, .7, .10, US-011.5, .8, US-012.7, US-013.4, US-014.2); start-up validation (US-009.3, .5, .7); pre-application statuses confined to container answers (Appendix A note, Appendix D introduction); raw-socket list completed; Appendix C gaps closed. Appendix D now has 106 rows (T-01 to T-40 with T-14b and T-24b, C-01 to C-13, E-01 to E-39 with E-24b, S-01 to S-11); 102 acceptance criteria in 13 active stories (unchanged).
- v0.3 (2026-10-30): Round 2 of Phase 2. Resolved the Liaison review (handoff 010: F-01 to F-21) and the Security review (handoff 011: SEC-F-043 to SEC-F-056); the finding-by-finding table is in handoff 012. Applied the Sponsor decisions of 2026-10-30: text policy (DEC-029: joiners allowed under a positional rule, tag characters rejected, marks capped), R-16 and R-17 accepted (DEC-031), single instance (DEC-032), cut order (DEC-030), DEC-025 to DEC-028 confirmed as amended. Encoded-path rule and id judged as received (BR-011, 5.0); wire grammar for content type, accept, query, bodies, methods, `Allow`, static headers (5.0); BR-012 steps clarified; UI state model and exact UI wording (US-010 to US-014); US-009.3 and US-009.5 rewritten, US-009.6 and US-009.7 added; US-014.8 added; INV-5 reworded; BR-003 key definition completed; BR-006 items 6b to 6d; Appendix B body row corrected; Appendix C gains a test column; Appendix D gains T-14b, T-22 to T-40, C-09 to C-13, E-24 to E-39, S-08 to S-11 and tightened E-10, E-20, E-21, E-23; reserved identifiers reduced to the word reserved; REQ-009 and REQ-012 promoted (DEC-030); decisions DEC-029 to DEC-032 added.
- v0.2 (2026-10-30): Applied the Sponsor's decisions of 2026-10-30 ("accept all recommendations"): DEC-001 to DEC-024 CONFIRMED; Q1 FR4 reserved (DEC-003); Q2 no edit of title or author (DEC-010, US-012.9); Q3 5,000 books and `LIBRARY_FULL` (DEC-013); Q4 page confirmation when a rating would be lost (DEC-007, US-012.3); Q5 MoSCoW is the cut line (DEC-022). Hostile review fixes: exact rules for text edge cases, BR-012 sub-orders, Content-Type, charset and Accept rules, HEAD and OPTIONS, body on GET and DELETE, query on other endpoints, number literal length, no CORS, no cookies, no redirects, static path handling (5.8), capacity under concurrency, database failure at start (US-009.5), response-size derivation, exact `details` shape per code (Appendix A), UI wording, US-014.1 without a tool name. New: Appendix D, DEC-025 to DEC-028 (PROPOSED), US-001.10, US-004.9 rewritten, US-009.5, US-012.9.
- v0.1 (2026-10-30): First draft, Phase 2 round 1. Created from the brief, the constitution v1.0 and `security.md`. Decisions DEC-001 to DEC-024 logged (3 confirmed, 16 proposed, 5 pending).
