process.env.TZ = 'Pacific/Auckland';
import test from 'node:test';
import assert from 'node:assert/strict';
import { statusLabel, localDate, countText, ratingText, matchesFilter } from './format.js';

const UTC_VALUE = '2026-02-19T23:30:00.000Z';

test('statusLabel gives the labels of US-010.3', () => {
  assert.equal(statusLabel('unread'), 'Unread');
  assert.equal(statusLabel('reading'), 'Reading');
  assert.equal(statusLabel('completed'), 'Completed');
});

test('countText: 1 book, 0 books, 2 books, 1000 books', () => {
  assert.equal(countText(1), '1 book');
  assert.equal(countText(0), '0 books');
  assert.equal(countText(2), '2 books');
  assert.equal(countText(1000), '1000 books');
});

test('localDate Pacific/Auckland (UTC+13) gives 2026-02-20', () => {
  process.env.TZ = 'Pacific/Auckland';
  assert.equal(localDate(UTC_VALUE), '2026-02-20');
});

test('localDate America/Los_Angeles (UTC-8) gives 2026-02-19', () => {
  process.env.TZ = 'America/Los_Angeles';
  assert.equal(localDate(UTC_VALUE), '2026-02-19');
});

test('localDate UTC gives 2026-02-19', () => {
  process.env.TZ = 'UTC';
  assert.equal(localDate(UTC_VALUE), '2026-02-19');
});

test('ratingText: rated, not rated, nothing for unread and reading', () => {
  assert.equal(ratingText({ status: 'completed', rating: 4 }), 'Rating: 4 of 5');
  assert.equal(ratingText({ status: 'completed', rating: null }), 'Not rated');
  assert.equal(ratingText({ status: 'unread', rating: null }), '');
  assert.equal(ratingText({ status: 'reading', rating: null }), '');
});

test('matchesFilter: all matches every book, otherwise status equals', () => {
  assert.equal(matchesFilter({ status: 'reading' }, 'all'), true);
  assert.equal(matchesFilter({ status: 'reading' }, 'reading'), true);
  assert.equal(matchesFilter({ status: 'reading' }, 'unread'), false);
});
