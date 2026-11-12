import test from 'node:test';
import assert from 'node:assert/strict';
import { describeFailure } from './failures.js';

// Expected strings are copied from spec.md Section 5.9, US-011, US-012 and US-013.
const STATUS = 'Choose Unread, Reading or Completed.';
const RATING = 'Choose a rating from 1 to 5.';
const NOT_ALLOWED = 'Only completed books can be rated.';
const FULL = 'Your library is full (5,000 books). Delete a book to add another.';
const GONE = 'This book no longer exists.';
const MALFORMED = 'The request could not be processed. Reload the page and try again.';
const HOST = 'This page cannot talk to the server from this address. Open GloberBooks from http://localhost or http://127.0.0.1.';
const INTERNAL = 'Something went wrong on our side. Try again.';
const NO_RESPONSE = 'Cannot reach the server. Check that GloberBooks is running, then try again.';

const api = (code, fields = []) => ({ kind: 'api', status: 400, code, fields });
const page = (text) => ({ action: 'page-message', text });

test('US-013.1 5.9: INVALID_STATUS shows the status text and restores the control', () => {
  assert.deepEqual(describeFailure(api('INVALID_STATUS')), { action: 'status-error', text: STATUS, restore: true });
});

test('US-013.1 5.9: INVALID_RATING and RATING_NOT_ALLOWED', () => {
  assert.deepEqual(describeFailure(api('INVALID_RATING')), { action: 'rating-error', text: RATING });
  assert.deepEqual(describeFailure(api('RATING_NOT_ALLOWED')),
    { action: 'rating-not-allowed', text: NOT_ALLOWED, reloadEntry: true });
});

test('US-013.1 5.9: DUPLICATE_BOOK opens the dialog, no text', () => {
  assert.deepEqual(describeFailure(api('DUPLICATE_BOOK')), { action: 'duplicate-dialog', text: null });
});

test('US-011.9 US-013.1: LIBRARY_FULL text', () => {
  assert.deepEqual(describeFailure(api('LIBRARY_FULL')), page(FULL));
});

test('US-012.7 5.9: BOOK_NOT_FOUND removes the entry, reloads the list, announces', () => {
  assert.deepEqual(describeFailure(api('BOOK_NOT_FOUND')),
    { action: 'entry-gone', text: GONE, removeEntry: true, reloadList: true });
});

test('US-013.1 5.9: request-defect codes share the MALFORMED_REQUEST text', () => {
  for (const code of ['MALFORMED_REQUEST', 'UNSUPPORTED_MEDIA_TYPE', 'NOT_ACCEPTABLE', 'METHOD_NOT_ALLOWED',
    'NOT_FOUND', 'PAYLOAD_TOO_LARGE']) {
    assert.deepEqual(describeFailure(api(code)), page(MALFORMED), code);
  }
});

test('US-013.1 5.9: INVALID_HOST and FORBIDDEN share the host text', () => {
  assert.deepEqual(describeFailure(api('INVALID_HOST')), page(HOST));
  assert.deepEqual(describeFailure(api('FORBIDDEN')), page(HOST));
});

test('US-013.1 .3 5.9: INTERNAL_ERROR and an unknown code give the internal text', () => {
  assert.deepEqual(describeFailure(api('INTERNAL_ERROR')), page(INTERNAL));
  assert.deepEqual(describeFailure(api('SOMETHING_NEW')), page(INTERNAL));
  assert.deepEqual(describeFailure(api('')), page(INTERNAL));
});

test('US-013.2 US-013.3 US-013.6: network, timeout and unreadable responses', () => {
  assert.deepEqual(describeFailure({ kind: 'network' }), page(NO_RESPONSE));
  assert.deepEqual(describeFailure({ kind: 'timeout' }), page(NO_RESPONSE));
  assert.deepEqual(describeFailure({ kind: 'unreadable' }), page(INTERNAL));
});

test('US-011.6 CR-7: VALIDATION_ERROR maps title and author messages and the status text, first field focused', () => {
  const fields = [
    { field: 'status', message: 'ignored server wording' },
    { field: 'author', message: 'must not be empty' },
    { field: 'title', message: 'is required' },
  ];
  assert.deepEqual(describeFailure(api('VALIDATION_ERROR', fields)), {
    action: 'field-errors',
    fields: [
      { field: 'title', text: 'is required' },
      { field: 'author', text: 'must not be empty' },
      { field: 'status', text: STATUS },
    ],
    focus: 'title',
  });
});

test('CR-7: a status entry is usable without a message; unusable entries are ignored', () => {
  const r = describeFailure(api('VALIDATION_ERROR', [{ field: 'status' }, { field: 'title', message: '' },
    { field: 'rating', message: 'x' }, null, 5, { field: 'author', message: 7 }]));
  assert.deepEqual(r, { action: 'field-errors', fields: [{ field: 'status', text: STATUS }], focus: 'status' });
});

test('US-011.6 CR-7: no usable details falls back to the MALFORMED_REQUEST text', () => {
  const unusable = [undefined, null, 'x', {}, 3, [], [{ field: 'title', message: '' }], [{ field: 'rating', message: 'x' }],
    [null], [{ field: 'title' }]];
  for (const fields of unusable) {
    assert.deepEqual(describeFailure(api('VALIDATION_ERROR', fields)), page(MALFORMED));
  }
  assert.deepEqual(describeFailure({ kind: 'api', code: 'VALIDATION_ERROR' }), page(MALFORMED));
});

test('US-013.5: the server error text, status and id are never returned', () => {
  const r = describeFailure({ kind: 'api', status: 500, code: 'INTERNAL_ERROR', error: '<script>alert(1)</script> 500 stack', id: 'abc' });
  assert.deepEqual(r, page(INTERNAL));
  const v = describeFailure({ kind: 'api', code: 'VALIDATION_ERROR', error: 'SECRET', fields: [{ field: 'title', message: 'm' }] });
  assert.ok(!JSON.stringify(v).includes('SECRET'));
});

test('hostile code values are never echoed', () => {
  const r = describeFailure(api('<img src=x onerror=alert(1)>'));
  assert.deepEqual(r, page(INTERNAL));
});

test('hostile input does not throw', () => {
  const inputs = [null, undefined, 1, 'x', [], {}, { kind: 'api' }, { kind: 'api', code: 5 }, { kind: 'api', code: null },
    { kind: 'api', code: 'VALIDATION_ERROR', fields: { length: 2 } }, { kind: 'api', code: '\uD800'.repeat(10) },
    { kind: 'api', code: 'x'.repeat(16384) }, { kind: 7 }, Object.create(null), () => 1];
  for (const f of inputs) {
    const r = describeFailure(f);
    assert.equal(typeof r.action, 'string');
  }
});

test('failures.js and the table use only exact copy strings (US-013.1)', () => {
  const texts = ['INVALID_STATUS', 'INVALID_RATING', 'RATING_NOT_ALLOWED', 'LIBRARY_FULL', 'BOOK_NOT_FOUND',
    'MALFORMED_REQUEST', 'INVALID_HOST', 'INTERNAL_ERROR'].map((c) => describeFailure(api(c)).text);
  assert.deepEqual(texts, [STATUS, RATING, NOT_ALLOWED, FULL, GONE, MALFORMED, HOST, INTERNAL]);
});
