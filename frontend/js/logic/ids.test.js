import test from 'node:test';
import assert from 'node:assert/strict';
import { isBookId, bookPath } from './ids.js';

const ID = '3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b';

test('isBookId accepts a lowercase hyphenated UUID', () => {
  assert.equal(isBookId(ID), true);
});

test('isBookId rejects upper case, wrong length, percent, empty and non-strings', () => {
  assert.equal(isBookId(ID.toUpperCase()), false);
  assert.equal(isBookId(ID.slice(1)), false);
  assert.equal(isBookId(`${ID}0`), false);
  assert.equal(isBookId('%2e%2e'), false);
  assert.equal(isBookId('3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a%b'), false);
  assert.equal(isBookId(''), false);
  assert.equal(isBookId(null), false);
  assert.equal(isBookId(42), false);
  assert.equal(isBookId(undefined), false);
});

test('bookPath builds the encoded path for a valid id', () => {
  assert.equal(bookPath(ID), `/api/v1/books/${ID}`);
});

test('bookPath throws for invalid ids', () => {
  for (const bad of ['%2e%2e', '../x', '', ID.toUpperCase(), null]) {
    assert.throws(() => bookPath(bad), TypeError);
  }
});
