import test from 'node:test';
import assert from 'node:assert/strict';
import {
  listBooks, addBook, toBook, getBook, setStatus, setRating, clearRating, deleteBook,
} from './booksApi.js';

const WIRE = {
  id: '3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b', title: 'Clean Code', author: 'Robert Martin',
  status: 'completed', rating: 4, date_added: '2026-02-19T10:00:00.000Z',
  date_completed: '2026-02-20T10:00:00.000Z',
};

function recorder(status, body) {
  const calls = [];
  const fetchImpl = async (path, init) => {
    calls.push({ path, init });
    return { ok: status < 300, status, text: async () => JSON.stringify(body) };
  };
  return { calls, fetchImpl };
}

test('toBook maps snake_case to camelCase and keeps the other members', () => {
  assert.deepEqual(toBook(WIRE), {
    id: WIRE.id, title: 'Clean Code', author: 'Robert Martin', status: 'completed', rating: 4,
    dateAdded: WIRE.date_added, dateCompleted: WIRE.date_completed,
  });
});

test('toBook returns null when a member is missing or mistyped', () => {
  for (const key of Object.keys(WIRE)) {
    const copy = { ...WIRE };
    delete copy[key];
    assert.equal(toBook(copy), null, key);
  }
  assert.equal(toBook({ ...WIRE, status: 'lost' }), null);
  assert.equal(toBook(null), null);
});

test('addBook default: 201, POST /api/v1/books, body without allow_duplicate', async () => {
  const { calls, fetchImpl } = recorder(201, { ...WIRE, status: 'unread', rating: null, date_completed: null });
  const r = await addBook({ title: 'Clean Code', author: 'Robert Martin', status: 'unread' }, false, { fetchImpl });
  assert.equal(r.ok, true);
  assert.equal(r.data.status, 'unread');
  assert.equal(calls[0].path, '/api/v1/books');
  assert.equal(calls[0].init.method, 'POST');
  assert.equal(calls[0].init.body, '{"title":"Clean Code","author":"Robert Martin","status":"unread"}');
  assert.equal(calls[0].init.headers['Content-Type'], 'application/json');
});

test('addBook with the override sends allow_duplicate true', async () => {
  const { calls, fetchImpl } = recorder(201, WIRE);
  await addBook({ title: 'Clean Code', author: 'Robert Martin', status: 'unread' }, true, { fetchImpl });
  assert.equal(
    calls[0].init.body,
    '{"title":"Clean Code","author":"Robert Martin","status":"unread","allow_duplicate":true}',
  );
});

test('addBook never sends allow_duplicate for non-true values', async () => {
  const { calls, fetchImpl } = recorder(201, WIRE);
  await addBook({ title: 't', author: 'a', status: 'unread' }, 'yes', { fetchImpl });
  assert.equal(calls[0].init.body.includes('allow_duplicate'), false);
});

test('listBooks: 200 gives {books, total} mapped; all filter sends no query', async () => {
  const { calls, fetchImpl } = recorder(200, { books: [WIRE], total: 1 });
  const r = await listBooks('all', { fetchImpl });
  assert.equal(calls[0].path, '/api/v1/books');
  assert.equal(calls[0].init.method, 'GET');
  assert.equal(r.data.total, 1);
  assert.equal(r.data.books[0].dateCompleted, WIRE.date_completed);
  assert.equal('Content-Type' in calls[0].init.headers, false);
});

test('listBooks: query only for the three status values', async () => {
  const { calls, fetchImpl } = recorder(200, { books: [], total: 0 });
  for (const f of ['unread', 'reading', 'completed']) {
    await listBooks(f, { fetchImpl });
  }
  assert.deepEqual(calls.map((c) => c.path), [
    '/api/v1/books?status=unread', '/api/v1/books?status=reading', '/api/v1/books?status=completed',
  ]);
  for (const bad of ['', 'UNREAD', 'x&y=1', undefined, 'unread&a=b']) {
    await listBooks(bad, { fetchImpl });
    assert.equal(calls.at(-1).path, '/api/v1/books');
  }
});

test('listBooks: wrong shapes are unreadable', async () => {
  for (const body of [{}, { books: [] }, { total: 0 }, { books: [{}], total: 1 }, { books: 'x', total: 0 }]) {
    const { fetchImpl } = recorder(200, body);
    assert.deepEqual((await listBooks('all', { fetchImpl })).failure, { kind: 'unreadable' });
  }
});

test('addBook: 409 DUPLICATE_BOOK and 409 LIBRARY_FULL', async () => {
  const dup = recorder(409, { error: 'x', code: 'DUPLICATE_BOOK', timestamp: 't', details: { existing_book: WIRE } });
  const r = await addBook({ title: 't', author: 'a', status: 'unread' }, false, { fetchImpl: dup.fetchImpl });
  assert.equal(r.failure.existingBook.id, WIRE.id);
  const full = recorder(409, { error: 'x', code: 'LIBRARY_FULL', timestamp: 't' });
  const f = await addBook({ title: 't', author: 'a', status: 'unread' }, false, { fetchImpl: full.fetchImpl });
  assert.equal(f.failure.code, 'LIBRARY_FULL');
});

const ID = WIRE.id;
const BASE = `/api/v1/books/${ID}`;

test('getBook: GET path, no body, no Content-Type, mapped book', async () => {
  const { calls, fetchImpl } = recorder(200, WIRE);
  const r = await getBook(ID, { fetchImpl });
  assert.equal(calls[0].path, BASE);
  assert.equal(calls[0].init.method, 'GET');
  assert.equal('body' in calls[0].init, false);
  assert.equal('Content-Type' in calls[0].init.headers, false);
  assert.equal(r.data.dateAdded, WIRE.date_added);
});

test('setStatus: PUT /status with the spec body and Content-Type', async () => {
  const { calls, fetchImpl } = recorder(200, { ...WIRE, rating: null });
  const r = await setStatus(ID, 'completed', { fetchImpl });
  assert.equal(calls[0].path, `${BASE}/status`);
  assert.equal(calls[0].init.method, 'PUT');
  assert.equal(calls[0].init.body, '{"status":"completed"}');
  assert.equal(calls[0].init.headers['Content-Type'], 'application/json');
  assert.equal(r.data.rating, null);
});

test('setRating: PUT /rating with the spec body and Content-Type', async () => {
  const { calls, fetchImpl } = recorder(200, { ...WIRE, rating: 5 });
  const r = await setRating(ID, 5, { fetchImpl });
  assert.equal(calls[0].path, `${BASE}/rating`);
  assert.equal(calls[0].init.method, 'PUT');
  assert.equal(calls[0].init.body, '{"rating":5}');
  assert.equal(calls[0].init.headers['Content-Type'], 'application/json');
  assert.equal(r.data.rating, 5);
});

test('SEC-F-097: clearRating and deleteBook send DELETE with Content-Type and no body', async () => {
  const { calls, fetchImpl } = recorder(200, { ...WIRE, rating: null });
  const cleared = await clearRating(ID, { fetchImpl });
  const deleted = await deleteBook(ID, { fetchImpl });
  assert.deepEqual(calls.map((c) => c.path), [`${BASE}/rating`, BASE]);
  for (const c of calls) {
    assert.equal(c.init.method, 'DELETE');
    assert.equal(c.init.headers['Content-Type'], 'application/json');
    assert.equal('body' in c.init, false);
  }
  assert.equal(cleared.data.rating, null);
  assert.equal(deleted.ok, true);
});

test('SEC-F-097: every non-GET wrapper carries Content-Type, GET does not', async () => {
  const { calls, fetchImpl } = recorder(200, WIRE);
  await getBook(ID, { fetchImpl });
  await setStatus(ID, 'reading', { fetchImpl });
  await setRating(ID, 3, { fetchImpl });
  await clearRating(ID, { fetchImpl });
  await deleteBook(ID, { fetchImpl });
  await addBook({ title: 't', author: 'a', status: 'unread' }, false, { fetchImpl });
  for (const c of calls) {
    assert.equal('Content-Type' in c.init.headers, c.init.method !== 'GET', c.init.method + c.path);
  }
});

test('BR-011: a hostile id is refused before any request by every function', async () => {
  const hostile = [
    '../x', '..', '%2e%2e', '%2E', ID.toUpperCase(), '', 'a'.repeat(5000), `${ID}/status`,
    `${ID}?x=1`, ` ${ID}`, null, undefined, 42, {}, [ID],
  ];
  const { calls, fetchImpl } = recorder(200, WIRE);
  const fns = [
    (id) => getBook(id, { fetchImpl }),
    (id) => setStatus(id, 'reading', { fetchImpl }),
    (id) => setRating(id, 3, { fetchImpl }),
    (id) => clearRating(id, { fetchImpl }),
    (id) => deleteBook(id, { fetchImpl }),
  ];
  for (const fn of fns) {
    for (const id of hostile) {
      await assert.rejects(() => fn(id), TypeError);
    }
  }
  assert.equal(calls.length, 0);
});

test('invalid status and rating values are refused before any request', async () => {
  const { calls, fetchImpl } = recorder(200, WIRE);
  for (const s of ['lost', 'Completed', '', null, undefined, 1]) {
    await assert.rejects(() => setStatus(ID, s, { fetchImpl }), TypeError);
  }
  for (const n of [0, 6, -1, 2.5, '3', NaN, null, undefined, Infinity]) {
    await assert.rejects(() => setRating(ID, n, { fetchImpl }), TypeError);
  }
  assert.equal(calls.length, 0);
});

test('failures: 404, 400 with fields, RATING_NOT_ALLOWED, 500, non-JSON, network', async () => {
  const nf = recorder(404, { error: 'SECRET', code: 'BOOK_NOT_FOUND', timestamp: 't' });
  const a = await getBook(ID, { fetchImpl: nf.fetchImpl });
  assert.deepEqual(a.failure, { kind: 'api', status: 404, code: 'BOOK_NOT_FOUND', fields: [] });
  assert.equal(JSON.stringify(a).includes('SECRET'), false);
  const bad = recorder(400, {
    error: 'x', code: 'INVALID_RATING', timestamp: 't',
    details: [{ field: 'rating', message: 'must be an integer from 1 to 5' }],
  });
  const b = await setRating(ID, 3, { fetchImpl: bad.fetchImpl });
  assert.equal(b.failure.code, 'INVALID_RATING');
  assert.deepEqual(b.failure.fields, [{ field: 'rating', message: 'must be an integer from 1 to 5' }]);
  const na = recorder(400, { error: 'x', code: 'RATING_NOT_ALLOWED', timestamp: 't' });
  assert.equal((await setRating(ID, 3, { fetchImpl: na.fetchImpl })).failure.code, 'RATING_NOT_ALLOWED');
  const ise = recorder(500, { error: 'x', code: 'INTERNAL_ERROR', timestamp: 't' });
  assert.equal((await deleteBook(ID, { fetchImpl: ise.fetchImpl })).failure.status, 500);
  const html = { ok: false, status: 502, text: async () => '<html>' };
  assert.deepEqual((await clearRating(ID, { fetchImpl: async () => html })).failure, { kind: 'unreadable' });
  const wrong = recorder(200, { id: ID });
  assert.deepEqual((await setStatus(ID, 'reading', { fetchImpl: wrong.fetchImpl })).failure, { kind: 'unreadable' });
  const down = async () => { throw new Error('down'); };
  assert.deepEqual((await deleteBook(ID, { fetchImpl: down })).failure, { kind: 'network' });
});
