import test from 'node:test';
import assert from 'node:assert/strict';
import { STATUS_LABELS, ADDED_PREFIX, COMPLETED_PREFIX, RATING_PREFIX, NOT_RATED } from './copy.js';

test('copy holds the exact strings fixed by US-010.3', () => {
  assert.deepEqual({ ...STATUS_LABELS }, { unread: 'Unread', reading: 'Reading', completed: 'Completed' });
  assert.equal(ADDED_PREFIX, 'Added: ');
  assert.equal(COMPLETED_PREFIX, 'Completed: ');
  assert.equal(RATING_PREFIX, 'Rating: ');
  assert.equal(NOT_RATED, 'Not rated');
});
