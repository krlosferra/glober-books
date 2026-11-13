import test from 'node:test';
import assert from 'node:assert/strict';
import { needsConfirmation, isChange, statusControlName } from './statusChange.js';
import {
  confirmStatusText, NO_LONGER_MATCHES_TEXT, CONFIRM_STATUS_HEADING, CONTINUE_LABEL, STATUS_OF_PREFIX,
} from './copy.js';

test('US-012.3: a shown rating plus unread or reading needs the confirmation', () => {
  assert.equal(needsConfirmation({ rating: 4 }, 'unread'), true);
  assert.equal(needsConfirmation({ rating: 1 }, 'reading'), true);
  assert.equal(needsConfirmation({ rating: 5 }, 'completed'), false);
});

test('US-012.3: a completed entry without a rating changes without a dialog', () => {
  assert.equal(needsConfirmation({ rating: null }, 'unread'), false);
  assert.equal(needsConfirmation({ rating: null }, 'reading'), false);
});

test('US-012.1: only a different valid status is a change', () => {
  assert.equal(isChange({ status: 'unread' }, 'reading'), true);
  assert.equal(isChange({ status: 'unread' }, 'unread'), false);
  assert.equal(isChange({ status: 'unread' }, 'other'), false);
});

test('US-012.1, .3, .8: exact strings', () => {
  assert.equal(STATUS_OF_PREFIX, 'Status of ');
  assert.equal(statusControlName('Dune'), 'Status of Dune');
  assert.equal(NO_LONGER_MATCHES_TEXT, 'No longer matches this filter');
  assert.equal(CONFIRM_STATUS_HEADING, 'Confirm status change');
  assert.equal(CONTINUE_LABEL, 'Continue');
  assert.equal(confirmStatusText('Dune', 'Unread'), 'Moving Dune back to Unread removes its rating and completion date.');
  assert.equal(confirmStatusText('<b>x</b>', 'Reading'), 'Moving <b>x</b> back to Reading removes its rating and completion date.');
});
