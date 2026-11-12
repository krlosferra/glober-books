/** Client-side text checks (UX only; the server stays the authority, BR-006 items 2 to 5). */

export const TITLE_MAX = 500;
export const AUTHOR_MAX = 200;

const WHITE_SPACE = /^\p{White_Space}$/u;

function isSpaceUnit(unit) {
  return WHITE_SPACE.test(unit);
}

/**
 * Remove Unicode White_Space at both ends (every such character is in the basic plane).
 * @param {string} text input
 * @returns {string} the trimmed text
 */
export function trimWhiteSpace(text) {
  let start = 0;
  let end = text.length;
  while (start < end && isSpaceUnit(text[start])) {
    start += 1;
  }
  while (end > start && isSpaceUnit(text[end - 1])) {
    end -= 1;
  }
  return text.slice(start, end);
}

/**
 * Count Unicode code points (not UTF-16 units, not graphemes).
 * @param {string} text input
 * @returns {number} the count
 */
export function countCodePoints(text) {
  let count = 0;
  for (const _point of text) {
    count += 1;
  }
  return count;
}

/**
 * Check a title or author: trim, NFC, count code points.
 * @param {unknown} value the typed value; anything but a string counts as empty
 * @param {number} max the limit in code points
 * @returns {{status: 'ok'|'empty'|'length', length: number}} the outcome
 */
export function checkText(value, max) {
  if (typeof value !== 'string') {
    return { status: 'empty', length: 0 };
  }
  const length = countCodePoints(trimWhiteSpace(value).normalize('NFC'));
  if (length === 0) {
    return { status: 'empty', length };
  }
  return { status: length > max ? 'length' : 'ok', length };
}

/**
 * Check a title (1 to 500 code points).
 * @param {unknown} value typed value
 * @returns {{status: string, length: number}} the outcome
 */
export function checkTitle(value) {
  return checkText(value, TITLE_MAX);
}

/**
 * Check an author (1 to 200 code points).
 * @param {unknown} value typed value
 * @returns {{status: string, length: number}} the outcome
 */
export function checkAuthor(value) {
  return checkText(value, AUTHOR_MAX);
}
