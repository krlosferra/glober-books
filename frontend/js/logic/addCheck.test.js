import test from 'node:test';
import assert from 'node:assert/strict';
import { checkAddForm } from './addCheck.js';

test('valid values give no problem', () => {
  assert.deepEqual(checkAddForm({ title: 'Dune', author: 'Frank Herbert' }), []);
  assert.deepEqual(checkAddForm({ title: '😀'.repeat(500), author: '😀'.repeat(200) }), []);
});

test('empty and whitespace-only values are reported per field, title first', () => {
  const r = checkAddForm({ title: '   ', author: '' });
  assert.deepEqual(r.map((p) => p.field), ['title', 'author']);
  assert.ok(r.every((p) => p.text.length > 0));
});

test('too long values are reported with a different text from empty', () => {
  const long = checkAddForm({ title: 'a'.repeat(501), author: 'b'.repeat(201) });
  const empty = checkAddForm({ title: '', author: '' });
  assert.deepEqual(long.map((p) => p.field), ['title', 'author']);
  assert.notEqual(long[0].text, empty[0].text);
  assert.notEqual(long[1].text, empty[1].text);
});

test('only the field in error is reported', () => {
  assert.deepEqual(checkAddForm({ title: 'x', author: '' }).map((p) => p.field), ['author']);
});
