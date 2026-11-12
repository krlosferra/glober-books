import type { Page, Route } from '@playwright/test';

const LIST_URL = /\/api\/v1\/books(\?[^#]*)?$/;

/** A list answer the page cannot provoke: failure codes and malformed bodies per Section 5.9. */
export type ListFailure =
  | { kind: 'network' }
  | { kind: 'status'; status: number; code: string; details?: unknown }
  | { kind: 'text'; status: number; body: string };

/** One list request seen by a controller. */
export interface ListRequest {
  index: number;
  url: string;
  released: boolean;
}

interface Held {
  request: ListRequest;
  route: Route;
  /** The real answer, taken when the request arrived (the server state at request time). */
  snapshot: Awaited<ReturnType<Route['fetch']>>;
}

/** Controls the list requests of one page: counts them and holds the first ones. */
export interface ListController {
  /** Every GET of the list since the controller was installed, in order. */
  readonly requests: ListRequest[];
  /** Number of held requests that arrived and are not released yet. */
  pendingCount(): number;
  /** Releases held request number `index` (0-based): the page receives the answer of its arrival time. */
  release(index: number): Promise<void>;
}

/** Envelope of the constitution's error format. */
function envelope(code: string, details?: unknown): string {
  const body: Record<string, unknown> = { error: 'stubbed failure', code, timestamp: '2026-03-14T12:00:00.000Z' };
  if (details !== undefined) body.details = details;
  return JSON.stringify(body);
}

/**
 * Answers a route with a synthetic body but keeps the real response headers
 * (CSP and the others), so the guard fixture can still check them.
 */
async function fulfillLikeReal(route: Route, status: number, body: string): Promise<void> {
  const real = await route.fetch();
  await route.fulfill({ response: real, status, body, contentType: 'application/json' });
}

/**
 * Holds the first `holdFirst` list requests (GET of /api/v1/books, any query) until released and
 * lets the others through. A held request is answered with the real server state of its arrival.
 * @param page the page
 * @param holdFirst how many of the first list requests to hold
 */
export async function holdLists(page: Page, holdFirst: number): Promise<ListController> {
  const requests: ListRequest[] = [];
  const held: Held[] = [];
  await page.route(LIST_URL, async (route) => {
    if (route.request().method() !== 'GET') return route.continue();
    const request: ListRequest = { index: requests.length, url: route.request().url(), released: false };
    requests.push(request);
    if (request.index >= holdFirst) return route.continue();
    const snapshot = await route.fetch();
    held[request.index] = { request, route, snapshot };
    return undefined;
  });
  return {
    requests,
    pendingCount: () => held.filter((h) => h !== undefined && !h.request.released).length,
    async release(index) {
      const entry = held[index];
      if (entry === undefined) throw new Error(`list request ${index} is not held`);
      entry.request.released = true;
      await entry.route.fulfill({ response: entry.snapshot });
    },
  };
}

/**
 * Fails the first `times` list requests as described and lets the others through.
 * @param page the page
 * @param failure the failure to simulate
 * @param times how many list requests fail (default 1)
 * @returns the list of seen request URLs, in order
 */
export async function failLists(page: Page, failure: ListFailure, times = 1): Promise<string[]> {
  const urls: string[] = [];
  await page.route(LIST_URL, async (route) => {
    if (route.request().method() !== 'GET') return route.continue();
    urls.push(route.request().url());
    if (urls.length > times) return route.continue();
    if (failure.kind === 'network') return route.abort('failed');
    if (failure.kind === 'text') return fulfillLikeReal(route, failure.status, failure.body);
    return fulfillLikeReal(route, failure.status, envelope(failure.code, failure.details));
  });
  return urls;
}

/** A book as the list answers carry it (snake_case). */
export interface WireBook {
  id: string;
  title: string;
  author: string;
  status: 'unread' | 'reading' | 'completed';
  rating: number | null;
  date_added: string;
  date_completed: string | null;
}

/**
 * Answers every list request with the given books (a fixed timestamp cannot be provoked through the API).
 * @param page the page
 * @param books the books to list
 */
export async function stubListBooks(page: Page, books: WireBook[]): Promise<void> {
  await page.route(LIST_URL, async (route) => {
    if (route.request().method() !== 'GET') return route.continue();
    return fulfillLikeReal(route, 200, JSON.stringify({ books, total: books.length }));
  });
}
