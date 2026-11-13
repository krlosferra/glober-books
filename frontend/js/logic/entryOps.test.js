import test from 'node:test';
import assert from 'node:assert/strict';
import {
  ratingGroupName, clearRatingName, deleteName, deletedText, parseRating, isRatingChange, hasClearButton,
} from './entryOps.js';

test('US-012.4 US-012.5: names and announcement are exact', () => {
  assert.equal(ratingGroupName('Dune'), 'Rating of Dune');
  assert.equal(clearRatingName('Dune'), 'Clear rating of Dune');
  assert.equal(deleteName('Dune'), 'Delete Dune');
  assert.equal(deletedText('Dune'), 'Book deleted: Dune');
});

test('US-012.4: parseRating accepts only 1 to 5', () => {
  assert.deepEqual(['1', '3', '5'].map(parseRating), [1, 3, 5]);
  for (const bad of ['0', '6', '', '1.0', '01', 'x', null, undefined, 3]) {
    assert.equal(parseRating(bad), null, String(bad));
  }
});

test('US-012.4: a rating request is due only for a completed entry and a different value', () => {
  const done = { status: 'completed', rating: 3 };
  assert.equal(isRatingChange(done, 4), true);
  assert.equal(isRatingChange(done, 3), false);
  assert.equal(isRatingChange(done, null), false);
  assert.equal(isRatingChange({ status: 'completed', rating: null }, 3), true);
  assert.equal(isRatingChange({ status: 'reading', rating: null }, 3), false);
});

test('US-012.4: the clear button exists only while a rating exists', () => {
  assert.equal(hasClearButton({ status: 'completed', rating: 2 }), true);
  assert.equal(hasClearButton({ status: 'completed', rating: null }), false);
  assert.equal(hasClearButton({ status: 'reading', rating: null }), false);
});
