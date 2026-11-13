/** Pure decisions for a status change of one entry (US-012.1, US-012.3). */
import { STATUS_OF_PREFIX } from './copy.js';

/**
 * Whether choosing a status first needs the confirmation dialog: only when the entry shows
 * a rating and the target is unread or reading (a completed entry without rating needs none).
 * @param {{rating: (number|null)}} book the entry's last known book
 * @param {string} target the chosen status
 * @returns {boolean} true when "Confirm status change" must open
 */
export function needsConfirmation(book, target) {
  return book.rating !== null && (target === 'unread' || target === 'reading');
}

/**
 * Whether a chosen value is a request to change the status.
 * @param {{status: string}} book the entry's last known book
 * @param {string} target the chosen value
 * @returns {boolean} true for a different, valid status
 */
export function isChange(book, target) {
  return ['unread', 'reading', 'completed'].includes(target) && target !== book.status;
}

/**
 * Accessible name of the status control.
 * @param {string} title the book title
 * @returns {string} "Status of <title>"
 */
export function statusControlName(title) {
  return `${STATUS_OF_PREFIX}${title}`;
}
