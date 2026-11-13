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

test('copy holds the failure texts of 5.9 and US-011 to US-013 verbatim', async () => {
  const c = await import('./copy.js');
  assert.equal(c.STATUS_CHOICE_TEXT, 'Choose Unread, Reading or Completed.');
  assert.equal(c.RATING_CHOICE_TEXT, 'Choose a rating from 1 to 5.');
  assert.equal(c.RATING_NOT_ALLOWED_TEXT, 'Only completed books can be rated.');
  assert.equal(c.LIBRARY_FULL_TEXT, 'Your library is full (5,000 books). Delete a book to add another.');
  assert.equal(c.BOOK_GONE_TEXT, 'This book no longer exists.');
  assert.equal(c.REQUEST_FAILED_TEXT, 'The request could not be processed. Reload the page and try again.');
  assert.equal(c.HOST_TEXT, 'This page cannot talk to the server from this address. Open GloberBooks from http://localhost or http://127.0.0.1.');
  assert.equal(c.SERVER_FAILED_TEXT, 'Something went wrong on our side. Try again.');
  assert.equal(c.NO_RESPONSE_TEXT, 'Cannot reach the server. Check that GloberBooks is running, then try again.');
});

test('copy holds the add form and duplicate dialog texts', async () => {
  const c = await import('./copy.js');
  assert.equal(c.DUPLICATE_HEADING, 'Possible duplicate');
  assert.equal(c.DUPLICATE_TEXT, 'This book may already exist. Add anyway?');
  assert.equal(c.CANCEL_LABEL, 'Cancel');
  assert.equal(c.ADD_ANYWAY_LABEL, 'Add anyway');
  assert.equal(c.LIBRARY_FULL_TEXT, 'Your library is full (5,000 books). Delete a book to add another.');
  for (const key of ['TITLE_EMPTY_TEXT', 'TITLE_LENGTH_TEXT', 'AUTHOR_EMPTY_TEXT', 'AUTHOR_LENGTH_TEXT']) {
    assert.equal(typeof c[key], 'string');
    assert.ok(c[key].length > 0);
  }
});

test('US-012.4 US-012.5: rating, clear and delete texts are verbatim', async () => {
  const c = await import('./copy.js');
  assert.equal(`${c.RATING_OF_PREFIX}Dune`, 'Rating of Dune');
  assert.equal(`${c.CLEAR_RATING_PREFIX}Dune`, 'Clear rating of Dune');
  assert.equal(`${c.DELETE_PREFIX}Dune`, 'Delete Dune');
  assert.equal(c.DELETE_HEADING, 'Delete book');
  assert.equal(c.DELETE_LABEL, 'Delete');
  assert.equal(c.deleteConfirmText('Dune'), 'Delete Dune? This cannot be undone.');
  assert.equal(`${c.BOOK_DELETED_PREFIX}Dune`, 'Book deleted: Dune');
});
