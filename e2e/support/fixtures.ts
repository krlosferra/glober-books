import { test as base, expect, type Page } from '@playwright/test';
import { Library } from './library';

const VIOLATION_MARK = '__securitypolicyviolation__';
const BASE_ORIGIN = 'http://127.0.0.1:18080';

// SEC-F-130: Chromium logs this warning because the Permissions-Policy header names 'bluetooth',
// which it does not recognize. The Sponsor's decision on the constitution header list is pending.
// Remove this filter when the header value is corrected. Nothing else is filtered.
const SEC_F_130_WARNING = "Error with Permissions-Policy header: Unrecognized feature: 'bluetooth'.";

const GUARDED_CONSOLE = /Content Security Policy|Trusted Type|Permissions-Policy/i;

const CSP = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; font-src 'self'; "
  + "connect-src 'self'; object-src 'none'; frame-src 'none'; worker-src 'none'; manifest-src 'self'; "
  + "frame-ancestors 'none'; base-uri 'none'; form-action 'self'; require-trusted-types-for 'script'";

const EXACT_HEADERS: Record<string, string> = {
  'content-security-policy': CSP,
  'x-content-type-options': 'nosniff',
  'x-frame-options': 'DENY',
  'referrer-policy': 'no-referrer',
  'cross-origin-opener-policy': 'same-origin',
  'cross-origin-resource-policy': 'same-origin',
  'cache-control': 'no-store',
};

const PERMISSION_FEATURES = ['camera', 'microphone', 'geolocation', 'payment', 'usb', 'serial',
  'bluetooth', 'clipboard-read', 'display-capture', 'fullscreen'];

const CONTENT_TYPE_BY_RESOURCE: Record<string, RegExp> = {
  document: /^text\/html(;|$)/i,
  script: /^(text|application)\/javascript(;|$)/i,
  stylesheet: /^text\/css(;|$)/i,
  fetch: /^application\/json(;|$)/i,
  xhr: /^application\/json(;|$)/i,
};

interface Seen { url: string; status: number; resource: string; headers: Record<string, string> }

function headerProblems(seen: Seen): string[] {
  const problems: string[] = [];
  const where = `${seen.url} (${seen.status})`;
  for (const [name, expected] of Object.entries(EXACT_HEADERS)) {
    if (seen.headers[name] !== expected) problems.push(`${where}: header ${name} is "${seen.headers[name]}"`);
  }
  const policy = seen.headers['permissions-policy'] ?? '';
  for (const feature of PERMISSION_FEATURES) {
    if (!new RegExp(`(^|,\\s*)${feature}=\\(\\)`).test(policy)) problems.push(`${where}: Permissions-Policy lacks ${feature}=()`);
  }
  const contentType = seen.headers['content-type'];
  const expected = CONTENT_TYPE_BY_RESOURCE[seen.resource];
  if (contentType === undefined) problems.push(`${where}: no Content-Type`);
  else if (expected && !expected.test(contentType)) problems.push(`${where}: Content-Type "${contentType}" for ${seen.resource}`);
  return problems;
}

function watch(page: Page) {
  const violations: string[] = [];
  const consoleProblems: string[] = [];
  const pageErrors: string[] = [];
  const foreign: string[] = [];
  const seen: Seen[] = [];
  page.on('console', (message) => {
    const text = message.text();
    if (text.startsWith(VIOLATION_MARK)) violations.push(text.slice(VIOLATION_MARK.length));
    else if ((message.type() === 'error' || message.type() === 'warning')
      && GUARDED_CONSOLE.test(text) && text.trim() !== SEC_F_130_WARNING) consoleProblems.push(text);
  });
  page.on('pageerror', (error) => pageErrors.push(error.message));
  page.on('request', (request) => {
    const url = new URL(request.url());
    if ((url.protocol === 'http:' || url.protocol === 'https:') && url.origin !== BASE_ORIGIN) foreign.push(request.url());
  });
  page.on('response', (response) => {
    const url = new URL(response.url());
    if (url.origin !== BASE_ORIGIN || url.pathname === '/favicon.ico') return;
    seen.push({
      url: url.pathname,
      status: response.status(),
      resource: response.request().resourceType(),
      headers: response.headers(),
    });
  });
  return { violations, consoleProblems, pageErrors, foreign, seen };
}

/** Extended test: the auto `guard` fixture watches every test; `library` seeds through the API. */
export const test = base.extend<{ guard: void; library: Library }>({
  library: async ({ request }, use) => {
    await use(new Library(request));
  },
  guard: [async ({ page }, use) => {
    await page.addInitScript((mark) => {
      document.addEventListener('securitypolicyviolation', (event) => {
        console.log(`${mark}${event.violatedDirective} ${event.blockedURI}`);
      });
    }, VIOLATION_MARK);
    const found = watch(page);
    await use();
    expect.soft(found.violations, 'securitypolicyviolation events').toEqual([]);
    expect.soft(found.consoleProblems, 'CSP, Trusted Types or Permissions-Policy console messages').toEqual([]);
    expect.soft(found.pageErrors, 'page errors').toEqual([]);
    expect.soft(found.foreign, 'requests to another origin').toEqual([]);
    expect.soft(found.seen.flatMap(headerProblems), 'Content-Type and security headers of every asset').toEqual([]);
  }, { auto: true }],
});

export { expect };
