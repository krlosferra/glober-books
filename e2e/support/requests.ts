import type { Page, Route } from '@playwright/test';

const BOOK_URL = /\/api\/v1\/books(\/[^/?#]+)?(\/(status|rating))?(\?[^#]*)?$/;

/** One request of the page to the books API. */
export interface SeenRequest {
  method: string;
  path: string;
}

/**
 * Records every request of the page to /api/v1/books and below (counted on the real page).
 * @param page the page
 * @returns a filter by method and by an optional path ending, over the requests so far
 */
export function trackRequests(page: Page): (method: string, pathEnd?: RegExp) => SeenRequest[] {
  const seen: SeenRequest[] = [];
  page.on('request', (request) => {
    const url = new URL(request.url());
    if (BOOK_URL.test(url.pathname + url.search)) seen.push({ method: request.method(), path: url.pathname });
  });
  return (method, pathEnd) => seen.filter((r) => r.method === method && (pathEnd === undefined || pathEnd.test(r.path)));
}

/** A failure the page cannot provoke through its own controls. */
export type Failure =
  | { kind: 'network' }
  | { kind: 'status'; status: number; code: string };

/**
 * Answers with a synthetic body but keeps the real security headers (taken from a real GET of
 * the list), so nothing changes on the server and the guard fixture still checks the headers.
 */
async function fulfillLikeReal(route: Route, status: number, body: string): Promise<void> {
  const list = new URL('/api/v1/books', route.request().url()).toString();
  const real = await route.fetch({ method: 'GET', url: list, postData: undefined });
  await route.fulfill({ response: real, status, body, contentType: 'application/json' });
}

/**
 * Fails the requests of one method whose path matches, per the error envelope of the constitution.
 * @param page the page
 * @param method HTTP method to fail
 * @param pathEnd matches the path of the requests to fail
 * @param failure what the page receives
 */
export async function failRequests(page: Page, method: string, pathEnd: RegExp, failure: Failure): Promise<void> {
  await page.route(BOOK_URL, async (route) => {
    const request = route.request();
    if (request.method() !== method || !pathEnd.test(new URL(request.url()).pathname)) return route.continue();
    if (failure.kind === 'network') return route.abort('failed');
    const body = JSON.stringify({ error: 'stubbed failure', code: failure.code, timestamp: '2026-03-14T12:00:00.000Z' });
    return fulfillLikeReal(route, failure.status, body);
  });
}
