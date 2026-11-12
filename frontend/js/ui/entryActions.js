import { byId } from './dom.js';
import { confirmStatusChange } from './dialogs.js';
import { findEntryItem, updateEntry, IDLE } from './entryView.js';
import { setStatus } from '../api/booksApi.js';
import { describeFailure } from '../logic/failures.js';
import { bookReplaced, bookRemoved } from '../logic/listState.js';
import { statusLabel } from '../logic/format.js';
import { isBookId } from '../logic/ids.js';
import { needsConfirmation, isChange } from '../logic/statusChange.js';

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
  ctx.ui.set(id, { ...uiOf(ctx, id), ...next });
  refresh(ctx, id);
}

/** Focus the status control of an entry unless the user already moved focus elsewhere (P-F2). */
function restoreFocus(ctx, id) {
  const item = findEntryItem(ctx.list, id);
  const active = document.activeElement;
  if (item !== null && (active === null || active === document.body || item.contains(active))) {
    item.querySelector('.entry-status-select').focus();
  }
}

function removeGone(ctx, id, text) {
  const removal = bookRemoved(ctx.deps.getState(), id);
  ctx.ui.delete(id);
  ctx.deps.update(removal.state);
  ctx.deps.announce(text);
  byId('books-heading').focus();
  if (removal.reload) {
    ctx.deps.reload();
  }
}

function failed(ctx, id, failure) {
  const decision = describeFailure(failure);
  const text = typeof decision.text === 'string' ? decision.text : '';
  if (decision.removeEntry === true) {
    removeGone(ctx, id, text);
  } else if (entryOf(ctx, id) !== null) {
    setUi(ctx, id, { busy: false, message: text });
    restoreFocus(ctx, id);
  }
}

function succeeded(ctx, id, book) {
  const replaced = bookReplaced(ctx.deps.getState(), book);
  ctx.deps.setState(replaced.state);
  setUi(ctx, id, { busy: false, message: '' });
  if (replaced.applied) {
    restoreFocus(ctx, id);
  }
}

/** Send the status; a rejection (refused id or status) is a generic failure, never thrown on (F-35-1). */
async function send(ctx, id, target) {
  setUi(ctx, id, { busy: true, message: '' });
  let result;
  try {
    result = await setStatus(id, target);
  } catch {
    result = { ok: false, failure: { kind: 'unreadable' } };
  }
  if (result.ok) {
    succeeded(ctx, id, result.data);
  } else {
    failed(ctx, id, result.failure);
  }
  if (ctx.ui.has(id)) {
    ctx.ui.set(id, { ...uiOf(ctx, id), busy: false });
  }
}

async function onChange(ctx, event) {
  const select = event.target;
  const item = select instanceof HTMLSelectElement ? select.closest('.entry') : null;
  const id = item === null ? '' : item.dataset.bookId;
  const entry = isBookId(id) ? entryOf(ctx, id) : null;
  if (entry === null || uiOf(ctx, id).busy || !isChange(entry.book, select.value)) {
    return;
  }
  const target = select.value;
  if (needsConfirmation(entry.book, target)
    && !(await confirmStatusChange(entry.book.title, statusLabel(target)))) {
    refresh(ctx, id);
    restoreFocus(ctx, id);
    return;
  }
  await send(ctx, id, target);
}

/**
 * Wire the status control of every entry (US-012.1, .3, .6, .7, .8) by one delegated listener.
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
  return { uiOf: (id) => uiOf(ctx, id) };
}
