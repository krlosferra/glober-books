# GloberBooks Product Specification

- Owner: PM (`sdd-product-manager`)
- Version: 0.2
- Last updated: 2026-10-30
- Status: Draft (Phase 2, round 1 continued, after the Sponsor's decisions of 2026-10-30). Not yet approved: it awaits the Liaison verdict, the Security verdict and Sponsor approval at the Phase 2 gate. Decisions DEC-001 to DEC-024 are CONFIRMED by the Sponsor (2026-10-30); DEC-025 to DEC-028 are new PM proposals added in v0.2 and are PROPOSED. No decision is pending.
- Authority: below `constitution.md` v1.0, above `plan.md`. Companion: `requirements.md` (register), `handoffs/007` (discovery and decisions), `handoffs/009` (current review request).
- Conventions: "must" is binding. Messages quoted in this document are the informative English texts; tests assert `code`, HTTP status, `details[].field` and structure, never the text of `error` or `message`. "Code point" means a Unicode code point. "Rejected" means: the stated status and code are returned and no data changed.

## 1. Overview

**Problem Statement:** Readers need a simple way to track books they want to read, are reading, and have completed, and to remember how much they liked the ones they finished.

**Target Users:** Individual readers managing a personal reading list on their own machine. One user, one library, no accounts.

**Product:** a small REST API and a web page served by the same application. The web page is the user interface; the API is its only data channel.

**Success Metrics** (measurable; verification in Phase 4):
- M-1 A user adds a book in under 10 seconds: with the keyboard, three actions (type title, type author, press Enter) from page load (US-011).
- M-2 The library is visible instantly: with 1,000 books the list is on screen within 1 second of page load; the list API with 5,000 books of typical size (titles and authors of about 40 characters) answers within 1 second; any single-book operation answers within 300 ms. Reference environment: the project's development machine, application and browser on the same host; measured once and the figures recorded (DEC-020).
- M-3 Zero data loss on restart: every write acknowledged with a 2xx response is present after the application process is killed and restarted (US-009).
- M-4 100% of attempts to rate a book that is not completed are rejected (BR-007).
- M-5 Every acceptance criterion in this document maps to at least one automated test (constitution, Testing Requirements).

**Functional requirements covered:** FR1 Add Book, FR2 Track Status, FR3 Rate Books. FR4 Author Bio is RESERVED and not specified in v1 (DEC-003): the Sponsor holds it for the Phase 5 drift exercise. Its identifiers US-005 and BR-005 stay reserved and nothing in this document, the API or the page implements or anticipates it.

**Invariants** (must be true at all times, after every request, including failed ones):
- INV-1 A book's `rating` is non-null only if its `status` is `completed`.
- INV-2 A non-null `rating` is an integer from 1 to 5.
- INV-3 `status` is always exactly one of `unread`, `reading`, `completed`.
- INV-4 `date_completed` is non-null if and only if `status` is `completed`.
- INV-5 Two books with the same duplicate key (BR-003) exist only when the user explicitly confirmed adding the second one (`allow_duplicate: true`).
- INV-6 Every error response uses the envelope and codes of Appendix A.
- INV-7 The library never holds more than 5,000 books (DEC-013), also under concurrent requests.
- INV-8 A failed request changes nothing (BR-013).

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
- US-001.6 Characters forbidden by BR-006 (for example NUL, a tab or line feed inside the text, a zero-width space, bidirectional overrides, private-use and unassigned code points) are rejected with `400 VALIDATION_ERROR`; an unpaired surrogate escape or invalid UTF-8 anywhere in the body is rejected with `400 MALFORMED_REQUEST` (BR-006, DEC-027).
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
- US-002.6 A `status` value other than the three exact lower-case values (including `READING`, ` reading`, an empty value, `?status` without a value) is rejected with `400 INVALID_STATUS`. The value is judged after standard URL decoding.
- US-002.7 A repeated `status` parameter, or any query parameter other than `status` (parameter names are case-sensitive: `Status` is unknown), is rejected with `400 MALFORMED_REQUEST`; this check precedes the value check (BR-012).
- US-002.8 The response never contains more than 5,000 entries and no pagination members exist (Appendix B).

### US-003: Rate completed books (FR3)
**As a** reader **I want** to rate books I have completed **so that** I can remember which ones I enjoyed.

Acceptance Criteria:
- US-003.1 `PUT /api/v1/books/{id}/rating` with `{"rating": 4}` on a completed book returns 200 and the updated book with `rating` 4.
- US-003.2 Each integer 1, 2, 3, 4, 5 is accepted; 0, 6, -1, -0, 4.9, 5.0, 1e0, a number larger than any 64-bit integer (up to 100 characters long), `null` and a missing `rating` member are rejected with `400 INVALID_RATING` (BR-002).
- US-003.3 A valid rating sent for a book whose status is `unread` or `reading` is rejected with `400 RATING_NOT_ALLOWED` (BR-007) and the book is unchanged.
- US-003.4 An existing rating can be replaced by another valid value (DEC-002); sending the current value returns 200 and changes nothing.
- US-003.5 An empty object is rejected with `400 INVALID_RATING`.
- US-003.6 A value of the wrong JSON type (for example the string `"5"`, `true`, an array, an object) is rejected with `400 MALFORMED_REQUEST`; so is a number that is not valid JSON (`01`, `+5`, `NaN`) or longer than 100 characters.
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
- US-004.5 Matching ignores letter case, leading and trailing white space, Unicode compatibility forms and repeated internal white space (BR-003): "Clean Code" by "Robert Martin" matches " clean   code " by "ROBERT MARTIN" and the full-width form "Ｃlean Code".
- US-004.6 The same title with a different author, or the same author with a different title, is not a duplicate. Status, rating and dates do not take part in matching.
- US-004.7 `allow_duplicate: true` on a request with no matching book simply creates the book.
- US-004.8 Two simultaneous identical requests without `allow_duplicate` result in exactly one 201 and one 409 (BR-013).
- US-004.9 At capacity `LIBRARY_FULL` is reported before `DUPLICATE_BOOK`, with or without `allow_duplicate` (BR-012).

### US-005: Author Biography (FR4) — RESERVED, NOT SPECIFIED IN V1 (DEC-003)
The identifier is reserved. The Sponsor decided on 2026-10-30 to keep FR4 for the Phase 5 drift exercise (Scenario B: the spec changes before the code). No behavior, field, entity, error code, endpoint or page element of this story exists in v0.2, and no implementation may anticipate it. When the Phase 5 change request is approved, the PM adds the story, BR-005, the data model change (ASK FIRST for the new entity) and the contract in that order.

### US-006: Track status (FR2)
**As a** reader **I want** to change a book's status **so that** my list reflects my progress.

Acceptance Criteria:
- US-006.1 `PUT /api/v1/books/{id}/status` with `{"status": "reading"}` returns 200 and the updated book.
- US-006.2 Every transition between the three statuses is allowed (nine combinations including the same-state ones) (DEC-006).
- US-006.3 A change to `completed` from another status sets `date_completed` to the time of the change and leaves `rating` null (BR-008).
- US-006.4 A change from `completed` to `unread` or `reading` sets `rating` to null and `date_completed` to null in the same operation (BR-004, DEC-007). After a later change back to `completed`, `date_completed` is the new time and `rating` is null.
- US-006.5 A change to the status the book already has returns 200 and changes nothing, in particular not `date_completed` and not `rating` (idempotent).
- US-006.6 A status that is not exactly `unread`, `reading` or `completed` (including `Completed`, `"reading "`, the empty string, `null`, a missing member, an empty object) is rejected with `400 INVALID_STATUS` (BR-001).
- US-006.7 A non-string status (number, boolean, array, object) is rejected with `400 MALFORMED_REQUEST`.
- US-006.8 With a valid body, unknown id: `404 BOOK_NOT_FOUND`; malformed id: `400 MALFORMED_REQUEST`.
- US-006.9 `title`, `author`, `id` and `date_added` never change through this endpoint.

### US-007: View one book
**As a** reader **I want** to fetch a single book **so that** the page can refresh one entry without reloading the list.

Acceptance Criteria:
- US-007.1 `GET /api/v1/books/{id}` returns 200 and the book (same members as US-002.3).
- US-007.2 Unknown well-formed id: `404 BOOK_NOT_FOUND`. Malformed id (uppercase letters, wrong length, any non-UUID text): `400 MALFORMED_REQUEST`. A trailing slash or a `;` suffix after the id is a route that does not exist: `404 NOT_FOUND` (constitution API Standards).

Priority Should (DEC-009).

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
- US-009.3 Starting the application on an empty or missing database creates it; starting it on an existing database never removes or alters books.
- US-009.4 Loss of data on power failure or operating system crash is not guaranteed by this specification (stated limit).
- US-009.5 If the database cannot be opened or is not a valid GloberBooks database, the application refuses to start, reports the reason on the console without echoing book data, and leaves the file unmodified (DEC-028).

### US-010: Library view (UI)
**As a** reader **I want** a page that shows my library **so that** I see everything at a glance.

Acceptance Criteria:
- US-010.1 Opening the application root (`/`) shows a page with the title "GloberBooks", the add form (US-011), the status filter and the list; no other page exists, and no author biography element exists (DEC-003).
- US-010.2 While the list loads, a visible "Loading your books…" status is shown and announced politely to assistive technology.
- US-010.3 On success each entry shows title, author, status, `date_added` as a calendar date in the browser's local time zone (`YYYY-MM-DD`), and the rating as "Rating: n of 5" when present, "Not rated" for a completed book without rating, nothing for other statuses. A completed book also shows its completion date in the same format.
- US-010.4 The order is the API order (newest first). A count "n books" (singular "1 book", "0 books" for none) shows `total` of the current result.
- US-010.5 The filter offers All, Unread, Reading, Completed; the current choice is indicated by text and not by color alone; choosing one reloads the list with the matching `status` parameter (All sends none).
- US-010.6 Empty library (All selected, `total` 0): message "Your reading list is empty. Add your first book above." Empty filter result: message "No books with this status." plus a way back to All.
- US-010.7 If loading fails, the message of Section 5.9 for that failure and a "Try again" button are shown; the button repeats the request; the previous list is not shown as if it were current.
- US-010.8 Title, author and all other strings are displayed exactly as stored, as text; markup in a title is shown literally (US-014).
- US-010.9 With 1,000 books the list is visible within 1 second of load (M-2, DEC-020).

### US-011: Add a book and resolve duplicates (UI)
**As a** reader **I want** a quick form with a clear duplicate warning **so that** adding is fast and safe.

Acceptance Criteria:
- US-011.1 The form has labelled fields "Title" and "Author" and a "Status" choice defaulting to "Unread", and an "Add book" button. Keyboard focus is on Title at page load.
- US-011.2 Typing a title and an author and pressing Enter submits the form (three actions, M-1).
- US-011.3 Client-side checks (UX only) cover: empty or whitespace-only, and over the length limit counted in code points. Forbidden characters are left to the server (US-011.6). A failed check shows an inline message linked to the field and no request is sent.
- US-011.4 While a request is in progress the button is disabled, so a double click sends one request.
- US-011.5 On 201 the book appears at the top of the list when it matches the current filter, the count updates, the form is cleared and Status resets to Unread, a polite confirmation "Added: <title>" is announced (also when the book does not match the filter), and focus returns to Title.
- US-011.6 On `400` with `details`, each message is shown next to the field named in `field`; focus moves to the first field in error. `details` entries for unknown fields are shown in a general message area.
- US-011.7 On `409 DUPLICATE_BOOK` a modal dialog opens showing the existing book's title, author, status and added date from `details.existing_book`, with buttons "Add anyway" and "Cancel". Initial focus is on "Cancel"; Escape cancels.
- US-011.8 "Cancel" closes the dialog, keeps the typed values and returns focus to the form. "Add anyway" resends the same values with `"allow_duplicate": true`, then follows US-011.5.
- US-011.9 On `409 LIBRARY_FULL` a message "Your library is full (5,000 books). Delete a book to add another." is shown and the typed values are kept (DEC-013). This also applies after "Add anyway".
- US-011.10 Any other failure is shown per Section 5.9 and the typed values are kept.

### US-012: Change status, rate, clear rating and delete (UI)
**As a** reader **I want** controls on each book **so that** I can maintain my list without leaving the page.

Acceptance Criteria:
- US-012.1 Each entry has a single-choice status control whose accessible name is "Status of <title>" with the three values; choosing a different value sends `PUT .../status`.
- US-012.2 The rating control exists on every completed entry and on no other entry (so it appears after a successful change to `completed` and disappears after a successful change away from it).
- US-012.3 If the entry shows a rating and the user chooses `unread` or `reading`, a confirmation dialog first says "Moving <title> back to <Unread|Reading> removes its rating and completion date." with "Continue" and "Cancel"; initial focus on "Cancel"; "Continue" sends the request, "Cancel" restores the previous choice without a request (DEC-007). A completed entry without a rating changes without a dialog. The confirmation is client-side only: the API clears the data on its own (BR-004), also when called without any confirmation.
- US-012.4 The rating control is a labelled group named "Rating of <title>" offering 1 to 5, operable by keyboard, showing the current value as text, and a "Clear rating" button when a rating exists. Selecting a value sends `PUT .../rating`; "Clear rating" sends `DELETE .../rating`.
- US-012.5 Each entry has a "Delete" button; it opens a confirmation dialog "Delete <title>? This cannot be undone." with "Delete" and "Cancel"; initial focus on "Cancel". "Delete" sends `DELETE`; on success the entry disappears, the count updates, "Deleted: <title>" is announced and focus moves to the list heading.
- US-012.6 After any successful change the entry shows the server's returned values (not optimistic ones); while a request for an entry is in progress its controls are disabled.
- US-012.7 If a request fails, the control returns to the server's last known value and the message of Section 5.9 is shown next to the entry; on `404 BOOK_NOT_FOUND` the entry is removed and the list is reloaded.
- US-012.8 Changing status while a filter is active: an entry that no longer matches the filter stays visible until the next list load and shows the text "No longer matches this filter".
- US-012.9 No control edits an existing book's title or author (DEC-010).

### US-013: Error, loading and empty states (UI)
**As a** reader **I want** clear messages when something fails **so that** I know what to do next.

Acceptance Criteria:
- US-013.1 The page maps every `code` of Appendix A to the behavior of Section 5.9; the page chooses its text by `code`, never by the server's `error` text.
- US-013.2 A failure to reach the server (no response) shows "Cannot reach the server. Check that GloberBooks is running, then try again." and keeps the user's input.
- US-013.3 A response that is not a valid error envelope (not JSON, missing `code`, unknown `code`) is treated as `INTERNAL_ERROR`.
- US-013.4 Errors and results are announced in a polite live region; field errors are tied to their fields; the same message is not announced twice for one failure.
- US-013.5 No raw `error` text, status code, stack, URL or id is displayed to the user except the book's own data.
- US-013.6 No error leaves the page in a state where controls stay disabled permanently.

### US-014: Accessibility and safe rendering (UI)
**As a** reader (including with assistive technology) **I want** an accessible page **so that** I can use it however I work.

Acceptance Criteria:
- US-014.1 An automated accessibility check against the WCAG 2.1 A and AA rules reports zero violations of impact "serious" or "critical" on: the list, the add form with errors showing, and each dialog (DEC-026: the tool is chosen by the testing documents, not by this spec).
- US-014.2 Every control has a programmatic label; all operations work with the keyboard alone; focus is always visible; dialogs trap focus and return it to the control that opened them when closed.
- US-014.3 Text contrast is at least 4.5:1 (3:1 for large text and control boundaries); status and errors are never conveyed by color alone.
- US-014.4 The page has a document language, a page title, one `h1`, a main landmark; at a viewport 320 CSS pixels wide there is no horizontal scrolling of the page.
- US-014.5 Animations are absent or disabled when the user prefers reduced motion.
- US-014.6 Titles and authors containing `<script>alert(1)</script>`, `<img src=x onerror=alert(1)>`, quotes, ampersands and an extremely long unbroken word are displayed as inert text and do not break the layout; no content-security violation is reported in the browser console during any flow.
- US-014.7 The page makes no request to any origin other than its own.

## 3. Business Rules

Every rule has at least one passing scenario and one failing scenario (constitution, Testing Requirements).

### BR-001: Status validation
**Given** a request supplies a status (creating a book, changing status, or filtering the list)
**When** the value is exactly the text `unread`, `reading` or `completed` (lower case, no surrounding space)
**Then** it is accepted.
**Given** a value of the right JSON type (a string, or `null`, or missing where required) that is anything else, including `Completed`, `UNREAD`, `"unread "`, the empty string
**When** the request is evaluated
**Then** it is rejected with `400 INVALID_STATUS` and a `details` entry with `field` `status` (unless BR-012 step 6 turns it into `VALIDATION_ERROR`).
**Given** a status value of another JSON type (number, boolean, array, object)
**Then** the request is rejected with `400 MALFORMED_REQUEST`.
Exception: on `POST /api/v1/books` an absent `status` member means `unread`; an explicit `null` is `INVALID_STATUS`.

### BR-002: Rating value validation
**Given** a request assigns a rating
**When** the value is a JSON integer literal from 1 to 5
**Then** the value is valid.
**Given** a JSON number outside that range or not written as an integer literal (0, 6, -3, -0, 4.9, 5.0, 1e0, a number beyond the 64-bit range of at most 100 characters), `null`, or a missing `rating` member
**When** the request is evaluated
**Then** it is rejected with `400 INVALID_RATING` and a `details` entry with `field` `rating`.
**Given** a `rating` of another JSON type (string, boolean, array, object), or a number that is not valid JSON or longer than 100 characters
**Then** it is rejected with `400 MALFORMED_REQUEST`.

### BR-003: Duplicate detection and override
**Given** a book exists whose duplicate key equals the duplicate key of the incoming title and author
**When** a book is added without `allow_duplicate: true`
**Then** the request is rejected with `409 DUPLICATE_BOOK` carrying the existing book in `details.existing_book`, and nothing is stored.
**Given** the same situation
**When** the request carries `allow_duplicate: true`
**Then** the book is added (201) as a separate book.
**Definition:** the duplicate key of a (title, author) pair is the pair of the title key and the author key; a text key is obtained from the already trimmed and NFC-normalized text (BR-006) by normalizing to NFKC, converting to lower case without regard to locale, and collapsing every run of white space inside to a single space (DEC-001). Keys are computed in application logic and are not part of the API.
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

### BR-005: Bio validation — RESERVED (DEC-003)
Reserved for FR4. No behavior in v1.

### BR-006: Title and author text
**Given** a title or an author in a request
**When** it is processed
**Then**
1. it must be a JSON string, else `MALFORMED_REQUEST`; `null` or a missing member counts as empty;
2. white space at both ends is removed (characters with the Unicode White_Space property, which include no-break space, the Unicode line and paragraph separators and U+0085; a tab or line feed at an end is therefore trimmed, not rejected);
3. the text is converted to Unicode normalization form NFC;
4. the result is measured in Unicode code points (an emoji or a character outside the basic plane counts 1);
5. a title must be 1 to 500 and an author 1 to 200 code points;
6. it must not contain: control characters (category Cc, which includes tab, line feed and NUL), format characters (Cf) other than U+200D (zero-width joiner), line and paragraph separators (Zl, Zp), private-use code points, or code points unassigned in Unicode 15.0. The variation selector U+FE0F (category Mn) is allowed as a combining mark. White space of category Zs inside the text is allowed. Combining marks (Mn, Mc, Me) are allowed, within the length limit.
**And** a violation of items 5 or 6 (including an empty result) is rejected with `400 VALIDATION_ERROR` and one `details` entry per failing field (`field` `title` or `author`), whose message is the first failing check in this order: required (null or missing), empty, length, characters.
**And** an unpaired surrogate (for example the JSON escape `\uD800`) or invalid UTF-8 anywhere in the body is not text: the whole request is rejected with `400 MALFORMED_REQUEST` (BR-012 step 5, DEC-027).
**And** the stored and returned text is the trimmed NFC form; letter case and internal spacing are preserved.
Passing examples: "Les Misérables" / "Victor Hugo"; a title of 500 emoji (U+1F600); a title written with decomposed "é" (e plus U+0301) 500 times (counts 500 after NFC); a family emoji joined with U+200D. Failing examples (`VALIDATION_ERROR`): "", "   ", a single U+00A0, 501 characters, `a\u0000b`, `a\nb`, `a\tb`, `a​b` (zero-width space), `a‮b` (bidirectional override), `a‌b` (zero-width non-joiner), `ab` (private use), `a͸b` (unassigned), a tag character U+E0041. Failing example (`MALFORMED_REQUEST`): `"\uD800"`.

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
**When** the id (after URL decoding) is not a lowercase, hyphenated, 36-character UUID (pattern 8-4-4-4-12 of lowercase hexadecimal digits; the version digit is not checked)
**Then** it is rejected with `400 MALFORMED_REQUEST` before any lookup.
**Given** a well-formed id
**When** no book has it
**Then** the response is `404 BOOK_NOT_FOUND`.
Ids are generated by the system; clients cannot choose them.

### BR-012: Order of evaluation
**Given** a request violates several rules
**When** it is evaluated
**Then** the response reports the first violation in this order, and nothing later is evaluated:
1. Transport and security checks of the constitution, applied to every path (API, page, unknown), in this sub-order: (a) Host: `INVALID_HOST`; (b) `Origin`, then `Sec-Fetch-Site`: `FORBIDDEN`; (c) for every method other than GET and HEAD, content type absent or not `application/json` (a `charset` parameter other than UTF-8 counts as wrong): `UNSUPPORTED_MEDIA_TYPE`; (d) body over 16 KiB (16,384 bytes), also while streaming: `PAYLOAD_TOO_LARGE`.
2. Route and method, in this sub-order: unknown path `NOT_FOUND`; path known, method not defined for it (HEAD and OPTIONS included) `METHOD_NOT_ALLOWED`; on `/api/v1/` paths, an `Accept` header that excludes `application/json` `NOT_ACCEPTABLE`.
3. Path id format (BR-011): `MALFORMED_REQUEST`.
4. Query string, in this sub-order: shape (any parameter on an endpoint other than the list; on the list, an unknown or repeated parameter) `MALFORMED_REQUEST`; then the `status` value `INVALID_STATUS`.
5. Body, all `MALFORMED_REQUEST`: invalid UTF-8; unreadable JSON; a body that is not a JSON object; an unknown member (member names are case-sensitive); a duplicate member; content after the JSON value; an unpaired surrogate; a number literal longer than 100 characters; a wrong JSON type for a known member; a non-empty body on GET or DELETE.
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
**Given** two concurrent updates of the same book
**Then** both are applied one after the other and the later one wins; no mixed state is stored (DEC-018).

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
- There is exactly one entity in v1 (Book). No Author entity exists (DEC-003).
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
- Base path `/api/v1`. JSON members are snake_case. API responses (success and error) are `application/json; charset=UTF-8`, with non-ASCII characters written as UTF-8 text (not as escape sequences).
- Requests: every method other than GET and HEAD must send `Content-Type: application/json` (the only accepted parameter is `charset=UTF-8`, case-insensitive), also when it has no body (`DELETE` included), else `415 UNSUPPORTED_MEDIA_TYPE`. Request bodies are UTF-8 JSON objects of at most 16 KiB (16,384 bytes), else `413 PAYLOAD_TOO_LARGE`. GET and DELETE take no body. `Accept` on `/api/v1/` paths may be absent, `*/*`, `application/*` or include `application/json`; anything else is `406 NOT_ACCEPTABLE`. Whatever the `Accept` header, an error body is still JSON.
- Strict parsing: unknown members, duplicate members, content after the JSON value, and wrong JSON types are `400 MALFORMED_REQUEST`. A request within 16 KiB must never turn into another code because of a parser limit (a long string yields `VALIDATION_ERROR` by length, not a parse failure); the one exception is a number literal longer than 100 characters, which is `MALFORMED_REQUEST`.
- Query strings: only `GET /api/v1/books` accepts a query, and only `status` (once). A query on any other endpoint is `400 MALFORMED_REQUEST`.
- Only the paths and methods below exist. Any other path (including the page's unknown paths) is `404 NOT_FOUND`; another method on a defined path is `405 METHOD_NOT_ALLOWED` (HEAD and OPTIONS are never defined). Trailing slashes, suffixes, `;` path parameters and encoded variants are not routed (`404 NOT_FOUND`). The server never redirects.
- No CORS: no response carries an `Access-Control-*` header; a preflight request is answered like any other request by BR-012 (a cross-origin preflight is `403 FORBIDDEN`).
- No cookies or sessions: no response carries `Set-Cookie`. The server is reachable on `127.0.0.1` only; no authentication.
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
Errors: 400 `MALFORMED_REQUEST` (repeated or unknown parameter, body present), 400 `INVALID_STATUS` (bad `status` value; `details` `[{ "field": "status", "message": "must be one of: unread, reading, completed" }]`).

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
- 400 `MALFORMED_REQUEST`: unreadable JSON, unknown member, wrong JSON type (including `allow_duplicate` that is not a boolean), unpaired surrogate, invalid UTF-8.
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
`GET /` (and `GET /index.html`, the same page) serves the web page; the page's own scripts and stylesheets are served from fixed paths under the application root. It is not part of the versioned API and is governed by the constitution (Static resources, headers). Everything else, including directory paths, files of the build, test files, the database, `/error` and framework paths, is `404 NOT_FOUND` in the standard envelope. Methods other than GET are `405 METHOD_NOT_ALLOWED`.

### 5.9 Behavior of the web page for every error code (US-013)
| Code | HTTP | What the page does |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Show each `details[].message` beside the field named in `field`; keep input; focus the first field in error |
| `INVALID_STATUS` | 400 | Show "Choose Unread, Reading or Completed." beside the status control; restore the control's last known value |
| `INVALID_RATING` | 400 | Show "Choose a rating from 1 to 5." beside the rating control |
| `RATING_NOT_ALLOWED` | 400 | Show "Only completed books can be rated." and reload the entry (the book is not completed anymore) |
| `DUPLICATE_BOOK` | 409 | Open the duplicate dialog (US-011.7) |
| `LIBRARY_FULL` | 409 | Message of US-011.9 |
| `BOOK_NOT_FOUND` | 404 | "This book no longer exists." remove the entry and reload the list |
| `MALFORMED_REQUEST`, `UNSUPPORTED_MEDIA_TYPE`, `NOT_ACCEPTABLE`, `METHOD_NOT_ALLOWED`, `NOT_FOUND`, `PAYLOAD_TOO_LARGE` | 400, 415, 406, 405, 404, 413 | "The request could not be processed. Reload the page and try again." (these indicate a defect or a tampered page, not a user error) |
| `INVALID_HOST`, `FORBIDDEN` | 400, 403 | "This page cannot talk to the server from this address. Open GloberBooks from http://localhost or http://127.0.0.1." |
| `INTERNAL_ERROR` or unreadable response | 500, other | "Something went wrong on our side. Try again." |
| no response | n/a | Message of US-013.2 |

## 6. Out of Scope
- Authentication, accounts, multi-user support, sharing (BRD).
- Author biography (FR4) and any author concept, field, entity, endpoint, error code or page element: reserved for the Phase 5 drift exercise by Sponsor decision (DEC-003). Out of scope for now, not out of scope forever.
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

## 7. Decision Log

Status values: CONFIRMED (Sponsor, with the date shown; items traceable to the brief, the constitution or `security.md` were already approved and are also confirmed), RESERVED (confirmed by the Sponsor as not specified now), PROPOSED (added by the PM in v0.2, awaiting Sponsor confirmation at the Phase 2 gate). Original drafting date of DEC-001 to DEC-024: 2026-10-30; the Sponsor answered handoff 007 on 2026-10-30 with "accept all recommendations".

| ID | Date | Status | Decision | Rationale |
|---|---|---|---|---|
| DEC-001 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Duplicates are detected on title + author compared case-insensitively after trimming (brief), using the constitution's key (NFKC, locale-independent lower case) plus collapsing of internal white space runs | Users capitalize and space differently; a missed warning is harmless but a spurious one is noise; the refinement is cheap and testable |
| DEC-002 | 2026-10-30 | CONFIRMED (brief DEC-002; Sponsor 2026-10-30) | A rating can be changed after it was assigned | Opinions change |
| DEC-003 | 2026-10-30 | RESERVED (Sponsor 2026-10-30, Q1) | FR4 Author Bio is not specified in v1; US-005 and BR-005 stay reserved; no Author entity, field, endpoint, code or page element now; the Phase 5 change request introduces it | BRD says FR4 arrives through Drift Management; keeps the drift exercise meaningful; a new entity is ASK FIRST; the constitution forbids features not in the spec |
| DEC-004 | 2026-10-30 | CONFIRMED (constitution; Sponsor 2026-10-30) | Data is stored in a durable local file database; the BRD "local/in-memory" wording yields to the constitution | The success metric "zero data loss on restart" cannot be met by memory alone |
| DEC-005 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Duplicate override: the add request carries optional boolean `allow_duplicate`; the 409 carries the existing book in `details.existing_book` | Gives the UI dialog a mechanism, stays on one endpoint, keeps the first call free of side effects |
| DEC-006 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Status changes through `PUT /books/{id}/status`; any status may follow any status; same-status update is a successful no-op | FR2 needs an update path; restricting transitions has no business basis in the brief |
| DEC-007 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30, Q4) | Leaving `completed` clears rating and completion date in the same operation; the page asks for confirmation first when a rating is shown; the API needs no confirmation parameter | Invariants 1 and 4 force the clearing; a client-side dialog protects against accidental loss without a second API call |
| DEC-008 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30); refined by DEC-025 | Codes: `INVALID_STATUS`, `INVALID_RATING`, `RATING_NOT_ALLOWED` are HTTP 400 (BRD: 400 for bad ratings); `VALIDATION_ERROR` for title/author or several failing fields; wrong JSON types are `MALFORMED_REQUEST` | Keeps brief codes, uses the constitution's reserved codes, avoids a status code (422) outside the constitution list |
| DEC-009 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Operations added to complete "CRUD": `GET /books/{id}` (Should), `DELETE /books/{id}` (Must), `DELETE /books/{id}/rating` (Should); DELETE returns 200 with the removed representation | BRD scope says CRUD; the brief template has no delete or get; 204 is not in the constitution's status list; clearing a rating needs an explicit mechanism |
| DEC-010 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30, Q2) | Title and author are not editable in v1 | Not in FR1 to FR4; editing re-triggers duplicate checks and expands the surface; delete and re-add covers the need |
| DEC-011 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | `POST` returns the full book (201), no `message` member; rating cannot be given while adding | The page needs the new book at once; FR1 lists title, author, status only |
| DEC-012 | 2026-10-30 | CONFIRMED (brief lengths 1-500 and 1-200; constitution; SC-16; Sponsor 2026-10-30) | Text policy of BR-006 | Limits in code points after trimming; characters rejected per security control SC-16 |
| DEC-013 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30, Q3) | Maximum 5,000 books; `409 LIBRARY_FULL`; list response bounded by the same number (worst case below 16,000,000 bytes, typical under 2 MB, Appendix B) | Constitution requires a maximum and a response bound (SC-17, R-02 not accepted); "thousands of books" capacity assumption |
| DEC-014 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Order: `date_added` descending, ties by `id` ascending | Timestamps have millisecond resolution and can collide; a total order makes tests deterministic |
| DEC-015 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Non-UUID id: `400 MALFORMED_REQUEST`; unknown id: `404 BOOK_NOT_FOUND` | Constitution reserves `MALFORMED_REQUEST` for invalid path values; security wants a clean envelope, never 500 |
| DEC-016 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Enums are case-sensitive; an invalid `status` filter is `400 INVALID_STATUS`; repeated or unknown query parameters are `400 MALFORMED_REQUEST`; `null` status on POST is invalid | Constitution: lower-case case-sensitive enums, no coercion, strict parsing |
| DEC-017 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | `details`: array of `{field, message}` for `VALIDATION_ERROR`, `INVALID_STATUS`, `INVALID_RATING`; object `{existing_book}` for `DUPLICATE_BOOK`; absent elsewhere; messages never echo input | Constitution leaves the shape to the spec; one shape per code lets clients branch |
| DEC-018 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Duplicate check and insert are one atomic step; capacity check and insert likewise; concurrent updates are last-write-wins; no optimistic locking | Risk R-06 accepted by the Sponsor with a concurrent-request test; single user |
| DEC-019 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Durability: acknowledged writes survive process termination; no promise for power loss | Meets "zero data loss on restart"; stronger guarantees cost performance and are not needed for a personal list |
| DEC-020 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Timing targets: single-book operation 300 ms, list API 1 s at 5,000 typical books, page list visible in 1 s at 1,000 books, measured once on the reference environment and recorded; add by keyboard in three actions | BRD metrics "instantly" and "under 10 seconds" need numbers; measured once to avoid flaky tests |
| DEC-021 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Frontend scope: a single page with US-010 to US-014 behavior and the error table of 5.9 | BRD puts the frontend in scope without criteria |
| DEC-022 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30, Q5) | The exercise time budget (6 to 8 hours) is indicative; the cut line is MoSCoW | The exercise phases add up to 9.5 hours; the security and test requirements add work |
| DEC-023 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | `date_completed`: equals `date_added` on creation as completed, new time on each entry into completed, cleared on leaving | Keeps INV-4 true after any history |
| DEC-024 | 2026-10-30 | CONFIRMED (Sponsor 2026-10-30) | Fixed order of evaluation of checks (BR-012; sub-orders added in v0.2) | Without it, tests with several faults are ambiguous |
| DEC-025 | 2026-10-30 | PROPOSED (PM, v0.2) | Refinement of DEC-008: the spec's own value-domain codes `INVALID_STATUS` and `INVALID_RATING` (the latter named by the BRD) are used when a single status or rating value is of the right JSON type but not allowed; the reserved `VALIDATION_ERROR` is used for title and author and whenever two or more fields fail; the reserved `MALFORMED_REQUEST` is used for structure and JSON types; `RATING_NOT_ALLOWED` stays 400 | The constitution says a spec code with the same meaning takes precedence over the reserved code, and the BRD names the rating codes; a client can branch on one code per rule; one status class (400) is kept; Liaison ruling requested |
| DEC-026 | 2026-10-30 | PROPOSED (PM, v0.2) | US-014.1 names the standard (WCAG 2.1 A and AA rules, impact serious or critical) and not the checking tool | A tool name is a HOW detail owned by the constitution and `frontend-testing.md`; the criterion stays valid if the tool changes |
| DEC-027 | 2026-10-30 | PROPOSED (PM, v0.2) | Wire-level rules settled for testability: unpaired surrogate and invalid UTF-8 are `MALFORMED_REQUEST`; a `charset` other than UTF-8 is `UNSUPPORTED_MEDIA_TYPE`; a body on GET or DELETE, or a query on an endpoint that takes none, is `MALFORMED_REQUEST`; a number literal longer than 100 characters is `MALFORMED_REQUEST`; `Accept` rules; HEAD and OPTIONS are `METHOD_NOT_ALLOWED` (or the transport codes first); sub-order of the transport checks | Without exact answers an implementer must guess and the security tests (SC-16, SC-29, SC-30) have no expected value |
| DEC-028 | 2026-10-30 | PROPOSED (PM, v0.2) | An unopenable or invalid database makes the application refuse to start and leaves the file untouched (US-009.5) | A silent re-creation would destroy a reader's data; fail-closed matches the constitution's startup posture |

## Appendix A: Error catalog
Envelope: `error` (informative text), `code`, `timestamp` (fixed format), optional `details`. Text below is informative. There are no other codes; `BIO_TOO_LONG` of the brief is reserved with FR4 and does not exist in v1.

| Code | HTTP | Used when | `details` | Source |
|---|---|---|---|---|
| `VALIDATION_ERROR` | 400 | Title or author invalid; or two or more fields invalid on one request | array of 1 to 3 `{field, message}`, `field` in `title`, `author`, `status`, ordered in that sequence | constitution (reserved), BR-006, BR-012 |
| `INVALID_STATUS` | 400 | Status value not one of the three (body or filter) | array with exactly one entry, `field` `status` | BR-001 |
| `INVALID_RATING` | 400 | Rating not an integer literal 1 to 5, null or missing | array with exactly one entry, `field` `rating` | BR-002 |
| `RATING_NOT_ALLOWED` | 400 | Valid rating on a book that is not completed | none | BR-007 |
| `BOOK_NOT_FOUND` | 404 | Well-formed id with no book | none | BR-011 |
| `DUPLICATE_BOOK` | 409 | Add matches an existing book and no override | object `{existing_book}` | BR-003 |
| `LIBRARY_FULL` | 409 | Add when 5,000 books exist | none | BR-009 |
| `MALFORMED_REQUEST` | 400 | Bad JSON, non-object body, unknown or duplicate member, wrong type, malformed id, bad query shape, body on GET or DELETE, unpaired surrogate, invalid UTF-8 | none | constitution (reserved), BR-012 |
| `INVALID_HOST` | 400 | Host not allowed | none | constitution |
| `FORBIDDEN` | 403 | Origin or Fetch-Metadata not allowed | none | constitution |
| `NOT_FOUND` | 404 | Unknown route or static path | none | constitution |
| `METHOD_NOT_ALLOWED` | 405 | Method not defined for the path | none | constitution |
| `NOT_ACCEPTABLE` | 406 | `Accept` excludes `application/json` on an API path | none | constitution |
| `PAYLOAD_TOO_LARGE` | 413 | Body over 16 KiB | none | constitution |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | Content type missing, not `application/json`, or charset not UTF-8 (methods other than GET and HEAD) | none | constitution |
| `INTERNAL_ERROR` | 500 | Unexpected failure; generic text | none | constitution |

Informative texts: `VALIDATION_ERROR` "The request contains invalid values."; `INVALID_STATUS` "Status must be unread, reading or completed."; `INVALID_RATING` "Rating must be an integer from 1 to 5."; `RATING_NOT_ALLOWED` "Only completed books can be rated."; `BOOK_NOT_FOUND` "Book not found."; `DUPLICATE_BOOK` "A book with the same title and author already exists."; `LIBRARY_FULL` "The library is full."; `INTERNAL_ERROR` "Unexpected error." Detail messages (the only ones that exist): "is required", "must not be empty", "must be between 1 and 500 characters" (author: 200), "contains characters that are not allowed", "must be one of: unread, reading, completed", "must be an integer from 1 to 5". No message includes user input, member names other than the API `field`, class names or paths.

## Appendix B: Limits
| Item | Limit | Derivation / behavior |
|---|---|---|
| Title | 1 to 500 code points | BR-006 |
| Author | 1 to 200 code points | BR-006 |
| Request body | 16 KiB (16,384 bytes) | constitution; the largest valid body (title of 500 and author of 200 characters outside the basic plane written as escape pairs: 700 x 12 bytes = 8,400 bytes plus structure, under 9 KiB) fits; a valid body padded with white space to exactly 16,384 bytes is accepted |
| Number literal in a request | at most 100 characters | DEC-027 |
| Books in the library | 5,000 | BR-009 (DEC-013) |
| Entries in a list response | 5,000 | same as the library |
| One book in a response | at most about 3,100 bytes (2,800 bytes of text at 4 bytes per code point, about 300 of structure) | derived |
| Largest list response | below 16,000,000 bytes (5,000 x 3,100 = 15.5 MB, text written as UTF-8 and not as escape sequences); typical under 2 MB (about 400 bytes per book) | derived for the Security review (SC-17); the test asserts the limit |
| Query parameters | only `status`, once, on the list endpoint | US-002.7 |
| Ids | 36-character lowercase UUID | BR-011 |
| Error details | at most 3 entries; `existing_book` is one book | Appendix A |

## Appendix C: Traceability matrix

| FR / BRD rule | Requirements | User stories | Business rules | Endpoints | UI stories |
|---|---|---|---|---|---|
| FR1 Add Book | REQ-001, 010, 011, 025, 026, 027, 051 | US-001, US-004 | BR-001, BR-003, BR-006, BR-008, BR-009 | `POST /api/v1/books` | US-011 |
| FR1 / scope: view library | REQ-005, 006, 007, 012, 028 | US-002, US-007 | BR-010, BR-011 | `GET /api/v1/books`, `GET /api/v1/books/{id}` | US-010 |
| FR2 Track Status | REQ-002, 022, 023, 024, 048 | US-006 | BR-001, BR-004, BR-008 | `PUT /api/v1/books/{id}/status` | US-012 |
| FR3 Rate Books | REQ-003, 008, 009, 020, 021 | US-003 | BR-002, BR-007, BR-004 | `PUT`, `DELETE /api/v1/books/{id}/rating` | US-012 |
| FR4 Author Bio | REQ-004 | US-005 (reserved) | BR-005 (reserved) | none (reserved, DEC-003) | none (reserved) |
| CRUD: delete | REQ-013 | US-008 | BR-011 | `DELETE /api/v1/books/{id}` | US-012 |
| CRUD: edit (decided out) | REQ-014 | none (Won't, DEC-010) | none | none | US-012.9 |
| BRD Rating Integrity | REQ-020, 023 | US-003, US-006 | BR-007, BR-004 | PUT rating, PUT status | US-012 |
| BRD Validation Range | REQ-021 | US-003 | BR-002 | PUT rating | US-012 |
| BRD Data Integrity (duplicates) | REQ-010, 011, 031 | US-004 | BR-003, BR-013 | POST | US-011 |
| BRD Explicit error handling | REQ-032, 040, 049, 051, 053 | all | BR-012, Appendix A, Appendix D | all | US-013 |
| Success metric: zero data loss | REQ-033, 052 | US-009 | BR-013 | all writes | n/a |
| Success metric: add in under 10 s | REQ-034 | US-011 | n/a | POST | US-011 |
| Success metric: instant library | REQ-035 | US-002 | BR-009 | GET list | US-010 |
| Constitution: frontend standards | REQ-018, 019, 039 | US-013, US-014 | n/a | n/a | US-013, US-014 |
| Boundaries: capacity / response bound | REQ-027, 036, 050 | US-001, US-002 | BR-009 | POST, GET list | US-011 |
| Security controls SC-16, SC-17, SC-29, SC-30 | REQ-026, 036, 040, 051, 053 | US-001, US-002, US-013 | BR-006, BR-009, BR-012 | all | US-014 |

Gaps: the plan section and task columns are filled in Phase 3. Every endpoint has a story; every story is reachable from an FR, a BRD rule or a logged decision; FR4 has no downstream items by design (DEC-003, reserved for Phase 5).

## Appendix D: Security test cases with expected results

Expected values for the security tests (`security.md` SC-16, SC-17, SC-29, SC-30). "Body" cases use `Content-Type: application/json`, the allowed `Host` and no `Origin` unless stated. A check on the error envelope means: members exactly `error`, `code`, `timestamp` (plus `details` only where Appendix A allows), `Content-Type` JSON with UTF-8, no `Server` product or version, no `Access-Control-*`, no `Set-Cookie`, no `Location`, and no stack trace, SQL, class name, path or echo of the input in any text.

### D.1 Text and identifiers (SC-16)
| # | Request | Expected |
|---|---|---|
| T-01 | POST title `a\u0000b`, valid author | 400 `VALIDATION_ERROR`, `details[0].field` `title` |
| T-02 | POST title containing a line feed, then one containing a tab, in the middle | 400 `VALIDATION_ERROR`, field `title` (each) |
| T-03 | POST title `a​b`; `a‮b`; `a‌b`; `ab`; `a͸b`; U+E0041 | 400 `VALIDATION_ERROR`, field `title` (each) |
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
| T-14 | GET, PUT, DELETE on `/api/v1/books/3F2B8C1E-5A4D-4E6F-9B7A-1C2D3E4F5A6B` (upper case); `/api/v1/books/not-a-uuid`; `/api/v1/books/1%27%20OR%20%271%27%3D%271` | 400 `MALFORMED_REQUEST` (each, with a valid JSON body where required) |
| T-15 | GET `/api/v1/books/00000000-0000-4000-8000-000000000000` | 404 `BOOK_NOT_FOUND` |
| T-16 | POST with `id`, `date_added`, `date_completed` or `rating` member; with `Title`; with duplicate `title` member; with `{"title":"a","author":"b"}x` | 400 `MALFORMED_REQUEST` (each) |
| T-17 | PUT rating with `4.9`, `5.0`, `1e0`, `0`, `6`, `-0`, `null`, `{}`, `99999999999999999999999` | 400 `INVALID_RATING`, field `rating` (each) |
| T-18 | PUT rating with `"5"`, `true`, `[5]`, `{"a":1}`, `01`, `+5`, a number of 101 characters | 400 `MALFORMED_REQUEST` (each) |
| T-19 | PUT status with `"Completed"`, `""`, `null`, `{}`; with `5` or `true` | 400 `INVALID_STATUS` (the first four); 400 `MALFORMED_REQUEST` (the last two) |
| T-20 | GET list with `?status=READING`, `?status=`, `?status`; with `?status=a&status=b`, `?foo=1`, `?Status=reading` | 400 `INVALID_STATUS` (first three); 400 `MALFORMED_REQUEST` (last three) |
| T-21 | POST allow_duplicate `"true"`, `1`, `null` | 400 `MALFORMED_REQUEST` (each) |

### D.2 Capacity and response size (SC-17)
| # | Scenario | Expected |
|---|---|---|
| C-01 | 5,000 books exist; POST a valid new book | 409 `LIBRARY_FULL`, no `details`, count stays 5,000 |
| C-02 | 5,000 books exist; POST a duplicate with `allow_duplicate: true`; POST a duplicate without it | 409 `LIBRARY_FULL` (both) |
| C-03 | 5,000 books exist; GET list; PUT status to `completed` on an unread book; PUT rating 4 on that book; DELETE one book | 200 with `total` 5000; 200; 200; 200 |
| C-04 | after C-03 delete, POST a valid book | 201 |
| C-05 | 5,000 books with title 500 and author 200 code points outside the basic plane; GET list | 200, `total` 5000, body below 16,000,000 bytes |
| C-06 | two simultaneous POSTs of different valid books with 4,999 books | one 201, one 409 `LIBRARY_FULL`; 5,000 books after |
| C-07 | two simultaneous identical POSTs without `allow_duplicate` | one 201, one 409 `DUPLICATE_BOOK`; one book stored |
| C-08 | a loop of 20 add and delete pairs at 4,999 books | every response 201 or 200; the count never exceeds 5,000 |

### D.3 Error paths, routing and headers (SC-29, also run on a real port for SC-30)
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
| E-10 | Host `evil.example`; no Host (HTTP/1.0); `localhost` without port; `localhost:<other port>`; `[::1]:<port>`; `127.0.0.1:<port>, evil.example`; two Host headers | 400 `INVALID_HOST` (each; the container may answer the last two with its own 400 under R-13: status 400 and no disclosure) |
| E-11 | Host `LOCALHOST:<port>`; `127.0.0.1:<port>`; `localhost:<port>` | 200 on GET `/api/v1/books` |
| E-12 | `Origin: http://evil.example`; `Origin: null`; `Origin: http://localhost:<other port>` on POST and on GET | 403 `FORBIDDEN` (each) |
| E-13 | `Origin: http://localhost:<port>` or `http://127.0.0.1:<port>` or none; `Sec-Fetch-Site: same-origin` or `none` | accepted (normal result) |
| E-14 | `Sec-Fetch-Site: cross-site`; `same-site` | 403 `FORBIDDEN` |
| E-15 | GET `/data/globerbooks.db`; `/js/`; `/css/`; `/error`; `/actuator/health`; `/h2-console`; `/WEB-INF/web.xml`; `/.git/config`; `/.env`; a path to a `*.test.js` file; `/%2e%2e/%2e%2e/etc/passwd`; `/..%2f..%2fetc/passwd`; `/index.html;jsessionid=1` | 404 `NOT_FOUND` (each) with the standard envelope |
| E-16 | GET `/` and GET `/index.html` | 200, HTML page; the headers of the constitution are present |
| E-17 | POST `/` | 405 `METHOD_NOT_ALLOWED` |
| E-18 | GET `/api/v1/books` with a body; DELETE `/api/v1/books/{id}` with the body `{}` | 400 `MALFORMED_REQUEST` (each) |
| E-19 | GET `/api/v1/books/{id}?x=1` | 400 `MALFORMED_REQUEST` |
| E-20 | A request line or header block above 8 KiB; both `Content-Length` and `Transfer-Encoding` | status 400 or 431 (container level, R-13), no server product or version in the response; the application keeps serving |
| E-21 | A forced unexpected failure (the plan names the test hook) | 500 `INTERNAL_ERROR`, text "Unexpected error.", no detail; the application keeps serving |
| E-22 | A connection that sends no complete request within 5 seconds | the server closes it (constitution connection timeout); other requests are served meanwhile |
| E-23 | Every response above | passes the envelope check stated at the top of this appendix |

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

## Changelog
- v0.2 (2026-10-30): Applied the Sponsor's decisions of 2026-10-30 ("accept all recommendations"): DEC-001 to DEC-024 CONFIRMED; Q1 FR4 reserved (DEC-003; US-005 and BR-005 stay reserved, no Phase 5 behavior in the spec); Q2 no edit of title or author (DEC-010, US-012.9); Q3 5,000 books and `LIBRARY_FULL` (DEC-013); Q4 page confirmation when a rating would be lost (DEC-007, US-012.3); Q5 MoSCoW is the cut line (DEC-022). All PENDING and PROPOSED markers removed. Hostile review fixes: exact rules for text edge cases (unpaired surrogate and invalid UTF-8 are `MALFORMED_REQUEST`, trimming of Unicode white space, variation selectors, unassigned code points tied to Unicode 15.0), BR-012 sub-orders (transport, route, query), Content-Type, charset and Accept rules, HEAD and OPTIONS, body on GET and DELETE, query on other endpoints, number literal length, no CORS, no cookies, no redirects, static path handling (5.8), capacity under concurrency, database failure at start (US-009.5), response-size derivation, exact `details` shape per code (Appendix A), UI wording (US-011, US-012, US-010.4), US-014.1 without a tool name. New: Appendix D (security test cases), DEC-025 to DEC-028 (PROPOSED), US-001.10, US-004.9 rewritten, US-009.5, US-012.9.
- v0.1 (2026-10-30): First draft, Phase 2 round 1. Created from the brief, the constitution v1.0 and `security.md`. Decisions DEC-001 to DEC-024 logged (3 confirmed, 16 proposed, 5 pending).
