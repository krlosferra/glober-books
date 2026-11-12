import test from 'node:test';
import assert from 'node:assert/strict';
import { requestJson } from './http.js';
import { toBook } from './booksApi.js';

const BOOK = {
  id: '3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b', title: 'Clean Code', author: 'Robert Martin',
  status: 'unread', rating: null, date_added: '2026-02-19T10:00:00.000Z', date_completed: null,
};

function reply(status, text) {
  return async () => ({ ok: status >= 200 && status < 300, status, text: async () => text });
}

function call(fetchImpl, extra = {}) {
  return requestJson({
    method: 'GET', path: '/api/v1/books', parseSuccess: toBook, mapBook: toBook, fetchImpl, ...extra,
  });
}

function envelope(code, details) {
  const body = { error: 'text', code, timestamp: '2026-02-19T10:00:00.000Z' };
  if (details !== undefined) {
    body.details = details;
  }
  return JSON.stringify(body);
}

test('success body is parsed and mapped', async () => {
  const r = await call(reply(200, JSON.stringify(BOOK)));
  assert.equal(r.ok, true);
  assert.equal(r.data.dateAdded, BOOK.date_added);
});

test('400 VALIDATION_ERROR carries fields from details', async () => {
  const details = [{ field: 'title', message: 'must be between 1 and 500 characters' }];
  const r = await call(reply(400, envelope('VALIDATION_ERROR', details)));
  assert.deepEqual(r.failure, {
    kind: 'api', status: 400, code: 'VALIDATION_ERROR', fields: details,
  });
});

test('details of the wrong shape give no fields and no throw', async () => {
  for (const details of ['x', { field: 'title' }, [1, null, { field: 'title' }], null]) {
    const r = await call(reply(400, envelope('VALIDATION_ERROR', details)));
    assert.equal(r.failure.kind, 'api');
    assert.deepEqual(r.failure.fields, []);
  }
});

test('a status entry without a string message stays usable (CR-7)', async () => {
  const r = await call(reply(400, envelope('VALIDATION_ERROR', [{ field: 'status' }, { field: 'title', message: 5 }])));
  assert.deepEqual(r.failure.fields, [{ field: 'status', message: '' }]);
});

test('400 INVALID_STATUS', async () => {
  const details = [{ field: 'status', message: 'must be one of: unread, reading, completed' }];
  const r = await call(reply(400, envelope('INVALID_STATUS', details)));
  assert.equal(r.failure.code, 'INVALID_STATUS');
  assert.equal(r.failure.fields[0].field, 'status');
});

test('409 DUPLICATE_BOOK carries the mapped existing book', async () => {
  const r = await call(reply(409, envelope('DUPLICATE_BOOK', { existing_book: BOOK })));
  assert.equal(r.failure.code, 'DUPLICATE_BOOK');
  assert.equal(r.failure.existingBook.dateAdded, BOOK.date_added);
  assert.equal(r.failure.existingBook.dateCompleted, null);
});

test('409 DUPLICATE_BOOK without a usable existing book is unreadable', async () => {
  const r = await call(reply(409, envelope('DUPLICATE_BOOK', {})));
  assert.equal(r.failure.kind, 'unreadable');
});

test('409 LIBRARY_FULL, 404 BOOK_NOT_FOUND, 500 INTERNAL_ERROR', async () => {
  for (const [status, code] of [[409, 'LIBRARY_FULL'], [404, 'BOOK_NOT_FOUND'], [500, 'INTERNAL_ERROR']]) {
    const r = await call(reply(status, envelope(code)));
    assert.deepEqual(r.failure, { kind: 'api', status, code, fields: [] });
  }
});

test('the envelope error text is not exposed', async () => {
  const r = await call(reply(500, envelope('INTERNAL_ERROR')));
  assert.equal(JSON.stringify(r).includes('text'), false);
});

test('502 with an HTML body is unreadable', async () => {
  const r = await call(reply(502, '<html>Bad Gateway</html>'));
  assert.deepEqual(r.failure, { kind: 'unreadable' });
});

test('200 with {} is unreadable', async () => {
  const r = await call(reply(200, '{}'));
  assert.deepEqual(r.failure, { kind: 'unreadable' });
});

test('unknown, missing and non-string code are unreadable', async () => {
  for (const text of [envelope('SOMETHING_ELSE'), '{"error":"x"}', '{"code":5}', '[]']) {
    const r = await call(reply(400, text));
    assert.equal(r.failure.kind, 'unreadable');
  }
});

test('fetch rejecting is network', async () => {
  const r = await call(async () => { throw new TypeError('failed'); });
  assert.deepEqual(r.failure, { kind: 'network' });
});

test('request options: headers, redirect, cache, credentials, body', async () => {
  let seen;
  const fetchImpl = async (path, init) => { seen = { path, init }; return reply(200, JSON.stringify(BOOK))(); };
  await call(fetchImpl);
  assert.equal(seen.path, '/api/v1/books');
  assert.deepEqual(seen.init.headers, { Accept: 'application/json' });
  assert.equal(seen.init.redirect, 'error');
  assert.equal(seen.init.cache, 'no-store');
  assert.equal(seen.init.credentials, 'omit');
  assert.equal('body' in seen.init, false);
  await call(fetchImpl, { method: 'POST', body: { a: 1 } });
  assert.deepEqual(seen.init.headers, { Accept: 'application/json', 'Content-Type': 'application/json' });
  assert.equal(seen.init.body, '{"a":1}');
});

function fakeTimers() {
  const t = { delay: null, fn: null, cleared: false };
  t.setTimer = (fn, delay) => { t.fn = fn; t.delay = delay; return 7; };
  t.clearTimer = (id) => { t.cleared = id === 7; };
  return t;
}

test('timeout: no complete response after 15000 ms is network and aborts the signal', async () => {
  const timers = fakeTimers();
  let signal;
  const fetchImpl = async (path, init) => {
    signal = init.signal;
    return { ok: true, status: 200, text: () => new Promise(() => {}) };
  };
  const pending = call(fetchImpl, timers);
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(timers.delay, 15000);
  assert.equal(typeof timers.fn, 'function');
  timers.fn();
  const r = await pending;
  assert.deepEqual(r.failure, { kind: 'network' });
  assert.equal(signal.aborted, true);
  assert.equal(timers.cleared, true);
});

test('a response before the timer clears it and nothing is aborted', async () => {
  const timers = fakeTimers();
  let signal;
  const r = await call(async (p, init) => {
    signal = init.signal;
    return reply(200, JSON.stringify(BOOK))();
  }, timers);
  assert.equal(r.ok, true);
  assert.equal(timers.cleared, true);
  assert.equal(signal.aborted, false);
});

test('a caller-supplied abort is network', async () => {
  const caller = new AbortController();
  const pending = call(async () => new Promise(() => {}), { signal: caller.signal, ...fakeTimers() });
  caller.abort();
  assert.deepEqual((await pending).failure, { kind: 'network' });
  const already = await call(reply(200, '{}'), { signal: AbortSignal.abort() });
  assert.deepEqual(already.failure, { kind: 'network' });
});
