import { setText, setVisible } from './dom.js';
import { ADDED_PREFIX, COMPLETED_PREFIX, NO_LONGER_MATCHES_TEXT } from '../logic/copy.js';
import { statusLabel, localDate, ratingText } from '../logic/format.js';
import { statusControlName } from '../logic/statusChange.js';
import { isBookId } from '../logic/ids.js';
import { ratingGroupName, clearRatingName, deleteName, hasClearButton } from '../logic/entryOps.js';

/** Per-entry view state that the list state does not hold. */
export const IDLE = Object.freeze({ busy: false, message: '' });

function fillOptional(element, text) {
  setText(element, text);
  setVisible(element, text !== '');
}

/** Rating group: exists only on completed entries; shows the server's last known value (US-012.2, .4). */
function updateRating(item, book, ui) {
  const completed = book.status === 'completed';
  setVisible(item.querySelector('.entry-rating-mount'), completed);
  item.querySelector('.entry-rating-group').setAttribute('aria-label', ratingGroupName(book.title));
  item.querySelectorAll('.entry-rating-group input').forEach((radio) => {
    radio.checked = completed && Number(radio.value) === book.rating;
    radio.disabled = ui.busy;
  });
  const clear = item.querySelector('.entry-clear-rating');
  setText(clear, clearRatingName(book.title));
  setVisible(clear, hasClearButton(book));
  clear.disabled = ui.busy;
}

/**
 * Fill an entry element from an entry of the list state, in place (textContent only).
 * @param {HTMLElement} item the li made from the template
 * @param {{book: object, noLongerMatches: boolean}} entry the entry
 * @param {{busy: boolean, message: string}} ui per-entry view state
 */
export function updateEntry(item, entry, ui) {
  const { book } = entry;
  setText(item.querySelector('.entry-title'), book.title);
  setText(item.querySelector('.entry-author'), book.author);
  setText(item.querySelector('.entry-status'), statusLabel(book.status));
  setText(item.querySelector('.entry-added'), `${ADDED_PREFIX}${localDate(book.dateAdded)}`);
  const completed = book.dateCompleted === null ? ''
    : `${COMPLETED_PREFIX}${localDate(book.dateCompleted)}`;
  fillOptional(item.querySelector('.entry-completed'), completed);
  fillOptional(item.querySelector('.entry-rating'), ratingText(book));
  fillOptional(item.querySelector('.entry-mismatch'), entry.noLongerMatches ? NO_LONGER_MATCHES_TEXT : '');
  const select = item.querySelector('.entry-status-select');
  select.setAttribute('aria-label', statusControlName(book.title));
  select.value = book.status;
  select.disabled = ui.busy;
  const remove = item.querySelector('.entry-delete');
  setText(remove, deleteName(book.title));
  remove.disabled = ui.busy;
  updateRating(item, book, ui);
  setText(item.querySelector('.entry-message'), ui.message);
}

/**
 * Build one entry from the template.
 * @param {HTMLTemplateElement} template the entry template
 * @param {{book: object, noLongerMatches: boolean}} entry the entry
 * @param {{busy: boolean, message: string}} [ui] per-entry view state
 * @returns {HTMLElement} the li
 */
export function renderEntry(template, entry, ui = IDLE) {
  const item = template.content.firstElementChild.cloneNode(true);
  if (isBookId(entry.book.id)) {
    item.dataset.bookId = entry.book.id;
    // One radio group per entry: the name is made of the validated id only.
    item.querySelectorAll('.entry-rating-group input').forEach((radio) => {
      radio.setAttribute('name', `rating-${entry.book.id}`);
    });
  }
  updateEntry(item, entry, ui);
  return item;
}

/**
 * Find the rendered entry of a book by its id.
 * @param {HTMLElement} list the ul of entries
 * @param {string} id a valid book id
 * @returns {HTMLElement|null} the li, or null when it is not rendered
 */
export function findEntryItem(list, id) {
  return Array.from(list.children).find((item) => item.dataset.bookId === id) ?? null;
}
