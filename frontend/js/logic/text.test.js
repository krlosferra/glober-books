import test from 'node:test';
import assert from 'node:assert/strict';
import { checkTitle, checkAuthor, trimWhiteSpace, countCodePoints } from './text.js';

test('US-011.3 T-07: 500 emoji count 500 and are accepted', () => {
  assert.deepEqual(checkTitle('\u{1F600}'.repeat(500)), { status: 'ok', length: 500 });
});

test('US-011.3 T-07: 500 decomposed e-acute count 500 after NFC', () => {
  assert.deepEqual(checkTitle('é'.repeat(500)), { status: 'ok', length: 500 });
});

test('US-011.3 T-22: 300 times U+1D15E count 600 after NFC', () => {
  assert.equal(checkTitle('\u{1D15E}'.repeat(300)).length, 600);
  assert.equal(checkTitle('\u{1D15E}'.repeat(300)).status, 'length');
});

test('US-011.3: empty and white space only are empty', () => {
  for (const v of ['', '   ', '\t\n', ' ', '\u0085', ' ', ' ', '　', null, undefined, 5, {}, []]) {
    assert.deepEqual(checkTitle(v), { status: 'empty', length: 0 });
    assert.equal(checkAuthor(v).status, 'empty');
  }
});

test('US-011.3: 501 title and 201 author code points are length, 500 and 200 are ok', () => {
  assert.equal(checkTitle('a'.repeat(501)).status, 'length');
  assert.equal(checkTitle('a'.repeat(500)).status, 'ok');
  assert.equal(checkAuthor('a'.repeat(201)).status, 'length');
  assert.equal(checkAuthor('a'.repeat(200)).status, 'ok');
  assert.equal(checkAuthor('\u{1F600}'.repeat(201)).status, 'length');
});

test('BR-006 item 2: trims exactly the White_Space characters', () => {
  for (const c of ['\u0009', '\u000A', '\u000B', '\u000C', '\u000D', ' ', '\u0085', ' ', ' ',
    ' ', ' ', ' ', ' ', ' ', ' ', '　']) {
    assert.equal(trimWhiteSpace(`${c}x${c}`), 'x', `U+${c.codePointAt(0).toString(16)}`);
  }
  for (const c of ['﻿', '​', '᠎', '⁠']) {
    assert.equal(trimWhiteSpace(`${c}x${c}`), `${c}x${c}`, `U+${c.codePointAt(0).toString(16)} is not White_Space`);
  }
  assert.equal(trimWhiteSpace(' a  b '), 'a  b');
});

test('BR-006 items 2 and 4: trimming comes before counting', () => {
  assert.deepEqual(checkTitle(`  ${'a'.repeat(500)}　`), { status: 'ok', length: 500 });
});

test('countCodePoints counts code points, not units or graphemes', () => {
  assert.equal(countCodePoints('\u{1F600}'), 1);
  assert.equal(countCodePoints('é'), 2);
  assert.equal(countCodePoints(''), 0);
});

test('hostile input does not throw and returns a defined result', () => {
  const inputs = ['\uD800', 'a\uDC00b', '\uD800'.repeat(100), 'x'.repeat(16384), ' '.repeat(16384) + 'a' + ' '.repeat(16384),
    ' a '.repeat(8000), Symbol.iterator.toString(), NaN, true, () => 1, Object.create(null), [1, 2]];
  for (const v of inputs) {
    const r = checkTitle(v);
    assert.ok(['ok', 'empty', 'length'].includes(r.status));
    assert.equal(typeof r.length, 'number');
  }
  assert.equal(checkTitle('x'.repeat(16384)).status, 'length');
});
