/** Pure page-state model of the list (plan AP-7.1). No DOM; every event returns a new state. */
import { describeFailure } from './failures.js';
import { matchesFilter } from './format.js';
import { SERVER_FAILED_TEXT } from './copy.js';

const FILTERS = ['all', 'unread', 'reading', 'completed'];

/**
 * The initial state: All checked, loading, nothing loaded yet.
 * @returns {object} the state
 */
export function initialState() {
  return {
    filter: 'all', phase: 'loading', everLoaded: false, entries: [],
    count: 0, latestRequest: 0, failureText: '',
  };
}

/**
 * Start a list load: new request number, entries and count cleared (CR-2).
 * The request number to send with the result is `latestRequest` of the returned state.
 * @param {object} state current state
 * @param {string} filter all, unread, reading or completed (anything else means all)
 * @returns {object} the new state
 */
export function loadStarted(state, filter) {
  return {
    ...state,
    filter: FILTERS.includes(filter) ? filter : 'all',
    phase: 'loading',
    entries: [],
    count: 0,
    failureText: '',
    latestRequest: state.latestRequest + 1,
  };
}

/**
 * A load succeeded; ignored when it is not the latest request (US-010.10).
 * @param {object} state current state
 * @param {number} seq request number of the response
 * @param {{books: object[], total: number}} response the mapped list response
 * @returns {object} the new state (the same object when ignored)
 */
export function loadSucceeded(state, seq, response) {
  if (seq !== state.latestRequest) {
    return state;
  }
  return {
    ...state,
    phase: 'loaded',
    everLoaded: true,
    entries: response.books.map((book) => ({ book, noLongerMatches: false })),
    count: response.total,
    failureText: '',
  };
}

/**
 * A load failed; ignored when stale. Entries and count are cleared (US-010.7).
 * @param {object} state current state
 * @param {number} seq request number of the failed request
 * @param {unknown} failure a failure from api/http.js
 * @returns {object} the new state (the same object when ignored)
 */
export function loadFailed(state, seq, failure) {
  if (seq !== state.latestRequest) {
    return state;
  }
  const { text } = describeFailure(failure);
  const failureText = typeof text === 'string' && text !== '' ? text : SERVER_FAILED_TEXT;
  return {
    ...state, phase: 'failed', entries: [], count: 0, failureText,
  };
}

/**
 * A book was created. Inserted at the top, count plus one, only when a load has succeeded,
 * none is in progress and the book matches the filter (US-011.5, US-010.7).
 * @param {object} state current state
 * @param {object} book the created book
 * @returns {object} the new state (the same object when not inserted)
 */
export function bookAdded(state, book) {
  if (state.phase !== 'loaded' || !matchesFilter(book, state.filter)) {
    return state;
  }
  return { ...state, entries: [{ book, noLongerMatches: false }, ...state.entries], count: state.count + 1 };
}

/**
 * A book was replaced by the server's version. For an entry that is no longer in the list
 * (cleared by a newer load) nothing changes, and the announcement is still due.
 * @param {object} state current state
 * @param {object} book the server's book
 * @returns {{state: object, announce: boolean, applied: boolean}} announce is always true
 */
export function bookReplaced(state, book) {
  if (!state.entries.some((e) => e.book.id === book.id)) {
    return { state, announce: true, applied: false };
  }
  const entries = state.entries.map((e) => (e.book.id === book.id
    ? { book, noLongerMatches: !matchesFilter(book, state.filter) } : e));
  return { state: { ...state, entries }, announce: true, applied: true };
}

/**
 * A book was removed (delete or the 404 path). Count minus one when the entry is listed.
 * @param {object} state current state
 * @param {string} id the book id
 * @returns {{state: object, announce: boolean, applied: boolean, reload: boolean}} reload is false
 *   while a load is in progress (it already covers the removal)
 */
export function bookRemoved(state, id) {
  const reload = state.phase !== 'loading';
  if (!state.entries.some((e) => e.book.id === id)) {
    return { state, announce: true, applied: false, reload };
  }
  return {
    state: { ...state, entries: state.entries.filter((e) => e.book.id !== id), count: state.count - 1 },
    announce: true, applied: true, reload,
  };
}

/**
 * What the page shows: loading, failed, empty (All, count 0), filterEmpty or list (US-010.6).
 * @param {object} state current state
 * @returns {string} the view name
 */
export function viewOf(state) {
  if (state.phase !== 'loaded') {
    return state.phase;
  }
  if (state.count > 0) {
    return 'list';
  }
  return state.filter === 'all' ? 'empty' : 'filterEmpty';
}
