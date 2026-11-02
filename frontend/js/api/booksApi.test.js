import test from 'node:test';
import assert from 'node:assert/strict';
import { listBooks, addBook, toBook } from './booksApi.js';

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
