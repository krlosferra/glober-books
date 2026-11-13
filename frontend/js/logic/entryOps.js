/** Pure decisions for the rating group and the delete button of one entry (US-012.4, .5). */
import {
  RATING_OF_PREFIX, CLEAR_RATING_PREFIX, DELETE_PREFIX, BOOK_DELETED_PREFIX,
} from './copy.js';

/**
 * Accessible name of the rating group.
 * @param {string} title the book title
 * @returns {string} "Rating of <title>"
 */
export const ratingGroupName = (title) => `${RATING_OF_PREFIX}${title}`;

/**
 * Name of the clear button.
 * @param {string} title the book title
 * @returns {string} "Clear rating of <title>"
 */
export const clearRatingName = (title) => `${CLEAR_RATING_PREFIX}${title}`;

/**
 * Name of the delete button.
 * @param {string} title the book title
 * @returns {string} "Delete <title>"
 */
export const deleteName = (title) => `${DELETE_PREFIX}${title}`;

/**
 * Announcement after a successful delete.
 * @param {string} title the book title
 * @returns {string} "Book deleted: <title>"
 */
export const deletedText = (title) => `${BOOK_DELETED_PREFIX}${title}`;

/**
 * Parse the value of a rating radio: an integer 1 to 5 or null.
 * @param {unknown} value the radio value
 * @returns {number|null} the rating, or null for anything else
 */
export function parseRating(value) {
  const n = typeof value === 'string' && /^[1-5]$/.test(value) ? Number(value) : null;
  return n;
}

/**
 * Whether choosing a rating asks for a request: completed entry and a different valid value.
 * @param {{status: string, rating: (number|null)}} book the entry's last known book
 * @param {number|null} rating the chosen rating
 * @returns {boolean} true when PUT .../rating is due
 */
export function isRatingChange(book, rating) {
  return book.status === 'completed' && rating !== null && rating !== book.rating;
}

/**
 * Whether the clear button exists: only while a completed entry shows a rating.
 * @param {{status: string, rating: (number|null)}} book the entry's last known book
 * @returns {boolean} true when it is shown
 */
export const hasClearButton = (book) => book.status === 'completed' && book.rating !== null;
