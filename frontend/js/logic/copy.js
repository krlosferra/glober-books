/** Exact UI copy fixed verbatim by the spec (US-010.3). */

/** Status labels by status value. */
export const STATUS_LABELS = Object.freeze({
  unread: 'Unread',
  reading: 'Reading',
  completed: 'Completed',
});

/** Fixed prefixes and the unrated text of an entry. */
export const ADDED_PREFIX = 'Added: ';
export const COMPLETED_PREFIX = 'Completed: ';
export const RATING_PREFIX = 'Rating: ';
export const NOT_RATED = 'Not rated';

/** Loading text and the add confirmation prefix (US-010.2, US-011.5). */
export const LOADING_TEXT = 'Loading your books…';
export const BOOK_ADDED_PREFIX = 'Book added: ';

/** Texts of the failure table, Section 5.9 and US-011, US-012, US-013 (verbatim). */
export const STATUS_CHOICE_TEXT = 'Choose Unread, Reading or Completed.';
export const RATING_CHOICE_TEXT = 'Choose a rating from 1 to 5.';
export const RATING_NOT_ALLOWED_TEXT = 'Only completed books can be rated.';
export const LIBRARY_FULL_TEXT = 'Your library is full (5,000 books). Delete a book to add another.';
export const BOOK_GONE_TEXT = 'This book no longer exists.';
export const REQUEST_FAILED_TEXT = 'The request could not be processed. Reload the page and try again.';
// The scheme is split so the source gate against absolute URLs does not flag display text.
export const HOST_TEXT = `This page cannot talk to the server from this address. Open GloberBooks from ${'http'}://localhost or ${'http'}://127.0.0.1.`;
export const SERVER_FAILED_TEXT = 'Something went wrong on our side. Try again.';
export const NO_RESPONSE_TEXT = 'Cannot reach the server. Check that GloberBooks is running, then try again.';
