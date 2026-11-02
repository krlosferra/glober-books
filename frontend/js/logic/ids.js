const BOOK_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

/**
 * Whether a value is a lowercase hyphenated UUID (BR-011).
 * @param {unknown} id candidate id
 * @returns {boolean} true when valid
 */
export function isBookId(id) {
  return typeof id === 'string' && BOOK_ID.test(id);
}

/**
 * Path of one book. Throws for an invalid id (programming error only).
 * @param {string} id a valid book id
 * @returns {string} /api/v1/books/ followed by the encoded id
 */
export function bookPath(id) {
  if (!isBookId(id)) {
    throw new TypeError('invalid book id');
  }
  return `/api/v1/books/${encodeURIComponent(id)}`;
}
