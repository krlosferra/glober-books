import { byId } from './dom.js';
import { confirmStatusChange, confirmDelete } from './dialogs.js';
import { findEntryItem, updateEntry, IDLE } from './entryView.js';
import { setStatus, setRating, clearRating, getBook, deleteBook } from '../api/booksApi.js';
import { describeFailure } from '../logic/failures.js';
import { bookReplaced, bookRemoved } from '../logic/listState.js';
import { statusLabel } from '../logic/format.js';
import { isBookId } from '../logic/ids.js';
import { needsConfirmation, isChange } from '../logic/statusChange.js';
import { parseRating, isRatingChange, deletedText, focusKeyAfterReload } from '../logic/entryOps.js';

const entryOf = (ctx, id) => ctx.deps.getState().entries.find((e) => e.book.id === id) ?? null;
const uiOf = (ctx, id) => ctx.ui.get(id) ?? IDLE;

/** Redraw one rendered entry in place from the state and the per-entry view state. */
function refresh(ctx, id) {
  const entry = entryOf(ctx, id);
  const item = findEntryItem(ctx.list, id);
  if (entry !== null && item !== null) {
    updateEntry(item, entry, uiOf(ctx, id));
  }
}

function setUi(ctx, id, next) {
  if (entryOf(ctx, id) === null) {
    ctx.ui.delete(id); // no busy record for an entry that is gone (SEC-F-200)
    return;
  }
  ctx.ui.set(id, { ...uiOf(ctx, id), ...next });
  refresh(ctx, id);
}

/** The control of an entry a key names; a hidden clear button falls back to the first radio. */
function controlOf(item, key) {
  if (/^[1-5]$/.test(key)) {
    return item.querySelector(`.entry-rating-group input[value="${key}"]`);
  }
  const clear = item.querySelector('.entry-clear-rating');
  if (key === 'delete') {
    return item.querySelector('.entry-delete');
  }
  if (key === 'clear') {
    return clear.hidden ? item.querySelector('.entry-rating-group input') : clear;
  }
  return item.querySelector('.entry-status-select');
}

/** Focus the control used unless the user already moved focus elsewhere (P-F2). */
function restoreFocus(ctx, id, key) {
  const item = findEntryItem(ctx.list, id);
  const active = document.activeElement;
  if (item !== null && (active === null || active === document.body || item.contains(active))) {
    const used = controlOf(item, key);
    const target = focusKeyAfterReload(key, used !== null && used.closest('[hidden]') === null);
    controlOf(item, target).focus();
  }
}

/** Remove an entry, announce, focus the heading; reload the list only for the 404 path (US-010.10). */
function removeGone(ctx, id, text, reloadList = true) {
  const removal = bookRemoved(ctx.deps.getState(), id);
  ctx.ui.delete(id);
  ctx.deps.update(removal.state);
  ctx.deps.announce(text);
  byId('books-heading').focus();
  if (reloadList && removal.reload) {
    ctx.deps.reload();
  }
}

/** RATING_NOT_ALLOWED: fetch the entry again and show the server's values with the text (US-012.7). */
async function reloadEntry(ctx, id, text, key) {
  let result;
  try {
    result = await getBook(id);
  } catch {
    result = { ok: false, failure: { kind: 'unreadable' } };
  }
  if (result.ok) {
    ctx.deps.setState(bookReplaced(ctx.deps.getState(), result.data).state);
    setUi(ctx, id, { busy: false, message: text });
    restoreFocus(ctx, id, key);
  } else {
    failed(ctx, id, result.failure, 'status');
  }
}

function failed(ctx, id, failure, key) {
  const decision = describeFailure(failure);
  const text = typeof decision.text === 'string' ? decision.text : '';
  if (decision.removeEntry === true) {
    removeGone(ctx, id, text);
  } else if (entryOf(ctx, id) !== null) {
    setUi(ctx, id, { busy: false, message: text });
    restoreFocus(ctx, id, key);
    return decision.reloadEntry === true ? reloadEntry(ctx, id, text, key) : undefined;
  }
  return undefined;
}

function succeeded(ctx, id, book, key) {
  if (key === 'delete') {
    removeGone(ctx, id, deletedText(entryOf(ctx, id)?.book.title ?? book.title), false);
    return;
  }
  const replaced = bookReplaced(ctx.deps.getState(), book);
  ctx.deps.setState(replaced.state);
  setUi(ctx, id, { busy: false, message: '' });
  if (replaced.applied) {
    restoreFocus(ctx, id, key);
  }
}

/** Run one request of an entry; a rejection (refused id, status or rating) is a generic failure (F-35-1). */
async function send(ctx, id, request, key) {
  setUi(ctx, id, { busy: true, message: '' });
  let result;
  try {
    result = await request();
  } catch {
    result = { ok: false, failure: { kind: 'unreadable' } };
  }
  if (result.ok) {
    succeeded(ctx, id, result.data, key);
  } else {
    await failed(ctx, id, result.failure, key);
  }
  if (ctx.ui.has(id)) {
    ctx.ui.set(id, { ...uiOf(ctx, id), busy: false });
  }
}

/** The entry id and last known entry of an event target, or null when it is not a usable entry. */
function entryOfEvent(ctx, target) {
  const item = target instanceof Element ? target.closest('.entry') : null;
  const id = item === null ? '' : item.dataset.bookId;
  const entry = isBookId(id) ? entryOf(ctx, id) : null;
  return entry === null || uiOf(ctx, id).busy ? null : { id, entry };
}

async function onStatus(ctx, select, { id, entry }) {
  if (!isChange(entry.book, select.value)) {
    return;
  }
  const target = select.value;
  if (needsConfirmation(entry.book, target)
    && !(await confirmStatusChange(entry.book.title, statusLabel(target)))) {
    refresh(ctx, id);
    restoreFocus(ctx, id, 'status');
    return;
  }
  await send(ctx, id, () => setStatus(id, target), 'status');
}

async function onChange(ctx, event) {
  const found = entryOfEvent(ctx, event.target);
  if (found === null) {
    return;
  }
  if (event.target instanceof HTMLSelectElement) {
    await onStatus(ctx, event.target, found);
  } else if (event.target.closest('.entry-rating-group') !== null) {
    const rating = parseRating(event.target.value);
    if (isRatingChange(found.entry.book, rating)) {
      await send(ctx, found.id, () => setRating(found.id, rating), String(rating));
    } else {
      refresh(ctx, found.id);
    }
  }
}

async function onClick(ctx, event) {
  const target = event.target instanceof Element ? event.target : null;
  const clear = target === null ? null : target.closest('.entry-clear-rating');
  const remove = target === null ? null : target.closest('.entry-delete');
  const found = clear === null ? null : entryOfEvent(ctx, clear);
  if (found !== null) {
    await send(ctx, found.id, () => clearRating(found.id), 'clear');
  } else if (remove !== null) {
    await onDelete(ctx, remove);
  }
}

async function onDelete(ctx, button) {
  const found = entryOfEvent(ctx, button);
  if (found === null) {
    return;
  }
  const { id, entry } = found;
  if (!(await confirmDelete(entry.book.title))) {
    restoreFocus(ctx, id, 'delete');
    return;
  }
  await send(ctx, id, () => deleteBook(id), 'delete');
}

/**
 * Wire the status control, rating group and clear button of every entry (US-012.1, .3, .4, .6, .7, .8) by delegated listeners.
 * One request per entry at a time; the entry shows the server's returned values.
 * @param {object} deps collaborators from app.js
 * @param {function(): object} deps.getState current list state
 * @param {function(object): void} deps.setState store a state without rendering
 * @param {function(object): void} deps.update store a state and render the list
 * @param {function(string): void} deps.announce write the page-level message area
 * @param {function(): void} deps.reload reload the list for the active filter
 * @returns {{uiOf: function(string): {busy: boolean, message: string}}} per-entry view state for the list view
 */
export function initEntryActions(deps) {
  const ctx = { deps, list: byId('book-list'), ui: new Map() };
  ctx.list.addEventListener('change', (event) => onChange(ctx, event));
  ctx.list.addEventListener('click', (event) => onClick(ctx, event));
  return { uiOf: (id) => uiOf(ctx, id) };
}
