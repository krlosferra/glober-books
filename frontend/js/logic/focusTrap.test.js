import { test } from 'node:test';
import assert from 'node:assert/strict';
import { nextTabTarget } from './focusTrap.js';

test('Tab on the first button keeps the browser default (moves to the last)', () => {
  assert.equal(nextTabTarget(false, 'first'), null);
});

test('Tab on the last button wraps to the first', () => {
  assert.equal(nextTabTarget(false, 'last'), 'first');
});

test('Shift+Tab on the first button wraps to the last', () => {
  assert.equal(nextTabTarget(true, 'first'), 'last');
});

test('Shift+Tab on the last button keeps the browser default (moves to the first)', () => {
  assert.equal(nextTabTarget(true, 'last'), null);
});

test('focus on neither button: Tab goes to the first, Shift+Tab to the last', () => {
  assert.equal(nextTabTarget(false, 'other'), 'first');
  assert.equal(nextTabTarget(true, 'other'), 'last');
});
