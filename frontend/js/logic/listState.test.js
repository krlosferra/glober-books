import test from 'node:test';
import assert from 'node:assert/strict';
import {
  initialState, loadStarted, loadSucceeded, loadFailed, bookAdded, bookReplaced, bookRemoved, viewOf,
} from './listState.js';
import * as copy from './copy.js';
import { NO_RESPONSE_TEXT } from './copy.js';

const book = (id, status = 'unread') => ({
  id, title: `t${id}`, author: 'a', status, rating: null, dateAdded: '2026-01-01T00:00:00.000Z', dateCompleted: null,
});
const loaded = (filter, books, total = books.length) => {
  const s = loadStarted(initialState(), filter);
  return loadSucceeded(s, s.latestRequest, { books, total });
};

test('copy of the list states equals the spec text', () => {
  assert.equal(copy.FILTER_LEGEND, 'Filter by status');
  assert.equal(copy.EMPTY_LIBRARY_TEXT, 'Your reading list is empty. Add your first book above.');
  assert.equal(copy.EMPTY_FILTER_TEXT, 'No books with this status.');
  assert.equal(copy.SHOW_ALL_LABEL, 'Show all books');
  assert.equal(copy.TRY_AGAIN_LABEL, 'Try again');
  assert.equal(copy.LOADING_TEXT, 'Loading your books…');
});

test('US-010.5: initial state is All and loading; a load records the filter', () => {
  const s = initialState();
  assert.equal(s.filter, 'all');
  assert.equal(viewOf(s), 'loading');
  assert.equal(loadStarted(s, 'reading').filter, 'reading');
  assert.equal(loadStarted(s, 'bogus').filter, 'all');
});

test('US-010.2 CR-2: a new load clears entries and count and shows only loading', () => {
  const s = loadStarted(loaded('all', [book('1'), book('2')]), 'completed');
  assert.deepEqual(s.entries, []);
  assert.equal(s.phase, 'loading');
  assert.equal(viewOf(s), 'loading');
  assert.equal(s.everLoaded, true);
});

test('US-010.10: only the latest response counts, success or failure', () => {
  const s1 = loadStarted(initialState(), 'all');
  const s2 = loadStarted(s1, 'reading');
  const stale = loadSucceeded(s2, s1.latestRequest, { books: [book('1')], total: 1 });
  assert.equal(stale, s2);
  assert.equal(loadFailed(s2, s1.latestRequest, { kind: 'network' }), s2);
  const ok = loadSucceeded(s2, s2.latestRequest, { books: [book('2', 'reading')], total: 1 });
  assert.equal(ok.entries[0].book.id, '2');
  assert.equal(loadSucceeded(ok, s1.latestRequest, { books: [], total: 0 }), ok);
});

test('US-010.6: empty and filter-empty are decided by the count', () => {
  assert.equal(viewOf(loaded('all', [])), 'empty');
  assert.equal(viewOf(loaded('reading', [])), 'filterEmpty');
  assert.equal(viewOf(loaded('all', [book('1')])), 'list');
  const last = bookRemoved(loaded('all', [book('1')]), '1').state;
  assert.equal(viewOf(last), 'empty');
});

test('US-010.7: failure shows the 5.9 text, no entries, no count, no empty message', () => {
  const s = loaded('all', [book('1')]);
  const s2 = loadStarted(s, 'all');
  const f = loadFailed(s2, s2.latestRequest, { kind: 'network' });
  assert.equal(f.phase, 'failed');
  assert.equal(viewOf(f), 'failed');
  assert.equal(f.failureText, NO_RESPONSE_TEXT);
  assert.deepEqual(f.entries, []);
  assert.equal(f.filter, 'all');
  const f2 = loadStarted(f, 'unread');
  assert.equal(f2.filter, 'unread');
  assert.equal(f2.failureText, '');
});

test('US-010.7, US-011.5 CR-2: an add is not inserted before the first success, while loading or after failure', () => {
  const s0 = initialState();
  assert.equal(bookAdded(s0, book('9')), s0);
  const s1 = loadStarted(loaded('all', [book('1')]), 'all');
  assert.equal(bookAdded(s1, book('9')), s1);
  const f = loadFailed(s1, s1.latestRequest, { kind: 'timeout' });
  assert.equal(bookAdded(f, book('9')), f);
});

test('US-010.4: count is total plus matching adds minus deletes; non-matching add changes nothing', () => {
  const s = loaded('reading', [book('1', 'reading')], 7);
  const added = bookAdded(s, book('2', 'reading'));
  assert.equal(added.count, 8);
  assert.equal(added.entries[0].book.id, '2');
  assert.equal(added.entries[1].book.id, '1');
  assert.equal(bookAdded(s, book('3', 'unread')), s);
  assert.equal(bookRemoved(added, '1').state.count, 7);
  assert.equal(bookAdded(loaded('all', [], 0), book('4')).count, 1);
});

test('US-012.8: a replaced entry that no longer matches still counts', () => {
  const s = loaded('reading', [book('1', 'reading')], 1);
  const r = bookReplaced(s, book('1', 'completed'));
  assert.equal(r.state.entries[0].noLongerMatches, true);
  assert.equal(r.state.count, 1);
  assert.equal(r.state.entries[0].book.status, 'completed');
});

test('AP-7.1: replaced or removed on a cleared entry changes nothing, still announces, no reload while loading', () => {
  const s = loadStarted(loaded('all', [book('1')]), 'reading');
  const r = bookReplaced(s, book('1', 'reading'));
  assert.deepEqual(r.state, s);
  assert.equal(r.announce, true);
  assert.equal(r.applied, false);
  const d = bookRemoved(s, '1');
  assert.deepEqual(d.state, s);
  assert.equal(d.announce, true);
  assert.equal(d.applied, false);
  assert.equal(d.reload, false);
  const f = loadFailed(s, s.latestRequest, { kind: 'network' });
  assert.deepEqual(bookRemoved(f, '1').state, f);
  assert.equal(bookRemoved(f, '1').reload, true);
});

test('a list failure without a text uses the fixed generic text (SEC-F-197)', () => {
  const s = loadStarted(initialState(), 'all');
  for (const code of ['VALIDATION_ERROR', 'DUPLICATE_BOOK', 'INVALID_STATUS']) {
    const f = loadFailed(s, s.latestRequest, { kind: 'api', code, fields: [] });
    assert.equal(typeof f.failureText, 'string');
    assert.notEqual(f.failureText, '');
  }
  const dup = loadFailed(s, s.latestRequest, { kind: 'api', code: 'DUPLICATE_BOOK', fields: [] });
  assert.equal(dup.failureText, copy.SERVER_FAILED_TEXT);
});
