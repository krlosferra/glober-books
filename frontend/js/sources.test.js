import test from 'node:test';
import assert from 'node:assert/strict';
import { readdirSync, readFileSync } from 'node:fs';
import { join, relative, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = dirname(fileURLToPath(import.meta.url));
const HTTP_FILE = join('api', 'http.js');

// Patterns are assembled from parts so this file does not contain the words it forbids.
const word = (...parts) => parts.join('');
const RULES = [
  ['eval', new RegExp(`\\b${word('ev', 'al')}\\s*\\(`)],
  ['new Function', new RegExp(word('new\\s+Fun', 'ction\\b'))],
  ['document write', new RegExp(`document\\s*\\.\\s*${word('wri', 'te')}(ln)?\\b`)],
  ['inner HTML', new RegExp(word('inner', 'HTML'))],
  ['outer HTML', new RegExp(word('outer', 'HTML'))],
  ['insert adjacent', new RegExp(word('insertAdjacent', 'HTML'))],
  ['unsafe HTML setter', new RegExp(word('setHTML', 'Unsafe'))],
  ['unsafe HTML parser', new RegExp(word('parseHTML', 'Unsafe'))],
  ['srcdoc', new RegExp(word('src', 'doc'))],
  ['timer with string argument', new RegExp(`\\b(setTimeout|setInterval)\\s*\\(\\s*["'\`]`)],
  ['dynamic import', new RegExp(word('\\bimp', 'ort\\s*\\('))],
  ['inline handler', new RegExp(`["'\`][^"'\`]*<[^>]*\\son[a-z]+\\s*=`)],
  ['script URL', new RegExp(word('java', 'script:'))],
  ['local storage', new RegExp(word('local', 'Storage'))],
  ['session storage', new RegExp(word('session', 'Storage'))],
  ['external URL', new RegExp(word('https?', ':\\/\\/'))],
  // Heuristic: bracket access built by string concatenation, e.g. el['inner' + 'HTML'].
  ['computed bracket access', /\[\s*(["'`])[^"'`\]]*\1\s*\+|\+\s*(["'`])[^"'`\]]*\2\s*\]/],
];
const FETCH_RULE = new RegExp(`\\b${word('fet', 'ch')}\\s*\\(`);

/** Return the names of the rules a source text breaks. */
function violations(text, file = 'other.js') {
  const found = RULES.filter(([, re]) => re.test(text)).map(([name]) => name);
  if (file !== HTTP_FILE && FETCH_RULE.test(text)) {
    found.push('network call outside api/http.js');
  }
  return found;
}

function productionFiles(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) {
      return productionFiles(path);
    }
    return entry.name.endsWith('.js') && !entry.name.endsWith('.test.js') ? [path] : [];
  });
}

test('checker accepts timers with a function argument', () => {
  assert.deepEqual(violations('setTimeout(function () { stop(); }, 15000)'), []);
  assert.deepEqual(violations('setTimeout(() => abort(), 15000)'), []);
});

test('checker flags timers with a string or template argument', () => {
  assert.notDeepEqual(violations('setTimeout("abort()", 15000)'), []);
  assert.notDeepEqual(violations("setTimeout('x', 1)"), []);
  assert.notDeepEqual(violations('setTimeout(`x`, 1)'), []);
  assert.notDeepEqual(violations("setInterval('x', 1)"), []);
});

test('checker flags the other forbidden sinks', () => {
  const samples = [
    `el.${word('inner', 'HTML')} = x`, `el.${word('outer', 'HTML')} = x`,
    `el.${word('insertAdjacent', 'HTML')}('a', x)`, `new ${word('Fun', 'ction')}('x')`,
    `${word('ev', 'al')}('x')`, `document.${word('wri', 'te')}('x')`, `el.${word('src', 'doc')} = x`,
    `await ${word('imp', 'ort')}('./x.js')`, `x = '${word('java', 'script:')}void(0)'`,
    `${word('local', 'Storage')}.getItem('a')`, `${word('session', 'Storage')}.clear()`,
    `x = '${word('https', '://')}example.test'`, `x = '<a onclick="f()">'`,
  ];
  for (const sample of samples) {
    assert.notDeepEqual(violations(sample), [], sample);
  }
});

test('checker flags bracket access built by concatenation (heuristic)', () => {
  assert.notDeepEqual(violations(`el['inner' + 'HTML'] = x`), []);
  assert.notDeepEqual(violations(`window['ev' + 'al']('x')`), []);
  assert.deepEqual(violations('const a = list[index + 1]'), []);
});

test('network call is allowed only in api/http.js', () => {
  const call = `${word('fet', 'ch')}(path)`;
  assert.deepEqual(violations(call, HTTP_FILE), []);
  assert.notDeepEqual(violations(call, join('api', 'booksApi.js')), []);
});

test('production sources contain no forbidden sink', () => {
  const files = productionFiles(ROOT);
  assert.ok(files.length >= 5, 'production files found');
  for (const path of files) {
    const rel = relative(ROOT, path);
    assert.deepEqual(violations(readFileSync(path, 'utf8'), rel), [], rel);
  }
});
