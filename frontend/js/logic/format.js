import { STATUS_LABELS, RATING_PREFIX, NOT_RATED } from './copy.js';

/**
 * Label of a status value.
 * @param {string} status unread, reading or completed
 * @returns {string} the label, or an empty string for an unknown status
 */
export function statusLabel(status) {
  return Object.hasOwn(STATUS_LABELS, status) ? STATUS_LABELS[status] : '';
}

/**
 * Local calendar date of an ISO UTC instant, as YYYY-MM-DD.
 * @param {string} iso ISO 8601 UTC value
 * @returns {string} the date in the local time zone, or an empty string if invalid
 */
export function localDate(iso) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const year = String(date.getFullYear()).padStart(4, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Count text: "1 book", otherwise "n books" without a thousands separator.
 * @param {number} n number of books
 * @returns {string} the count text
 */
export function countText(n) {
  return n === 1 ? '1 book' : `${n} books`;
}

/**
 * Rating text of a book: empty unless completed.
 * @param {{status: string, rating: (number|null)}} book the book
 * @returns {string} "Rating: n of 5", "Not rated" or an empty string
 */
export function ratingText(book) {
  if (book.status !== 'completed') {
    return '';
  }
  return book.rating === null ? NOT_RATED : `${RATING_PREFIX}${book.rating} of 5`;
}

/**
 * Whether a book belongs in the list for a filter.
 * @param {{status: string}} book the book
 * @param {string} filter all, unread, reading or completed
 * @returns {boolean} true when the book matches
 */
export function matchesFilter(book, filter) {
  return filter === 'all' || book.status === filter;
}
