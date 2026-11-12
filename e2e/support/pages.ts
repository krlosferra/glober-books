import type { Page, Route } from '@playwright/test';

const ADD_URL = /\/api\/v1\/books$/;

/** The add-form controls, by accessible label. */
export const form = (page: Page) => ({
  title: page.getByLabel('Title'),
  author: page.getByLabel('Author'),
  status: page.getByLabel('Status'),
  add: page.getByRole('button', { name: 'Add book' }),
});

/** The duplicate dialog and its parts. */
export const duplicate = (page: Page) => {
  const dialog = page.getByRole('dialog', { name: 'Possible duplicate' });
  return {
    dialog,
    cancel: dialog.getByRole('button', { name: 'Cancel' }),
    confirm: dialog.getByRole('button', { name: 'Add anyway' }),
  };
};

/** Types the values into the form without submitting. */
export async function fillBook(page: Page, title: string, author: string, status?: string): Promise<void> {
  const f = form(page);
  await f.title.fill(title);
  await f.author.fill(author);
  if (status !== undefined) await f.status.selectOption(status);
}

/** One POST of the add form as the page sent it. */
export interface SeenPost {
  body: Record<string, unknown>;
}

/** Records every POST /api/v1/books of the page (counted on the real page, not on the stub). */
export function trackPosts(page: Page): SeenPost[] {
  const posts: SeenPost[] = [];
  page.on('request', (request) => {
    if (request.method() === 'POST' && ADD_URL.test(new URL(request.url()).pathname)) {
      posts.push({ body: request.postDataJSON() as Record<string, unknown> });
    }
  });
  return posts;
}

/** A failure of the add request that the page cannot provoke through its own controls. */
export type AddFailure =
  | { kind: 'network' }
  | { kind: 'status'; status: number; code: string; details?: unknown };

function envelope(code: string, details?: unknown): string {
  const body: Record<string, unknown> = { error: 'stubbed failure', code, timestamp: '2026-03-14T12:00:00.000Z' };
  if (details !== undefined) body.details = details;
  return JSON.stringify(body);
}

/**
 * Answers with a synthetic body but keeps the real security headers: the headers are taken from
 * a real GET of the same URL, so no book is created by the stubbed POST.
 */
async function fulfillLikeReal(route: Route, status: number, body: string): Promise<void> {
  const real = await route.fetch({ method: 'GET', postData: undefined });
  await route.fulfill({ response: real, status, body, contentType: 'application/json' });
}

/**
 * Fails POSTs of the add form as described. The first `skip` POSTs reach the real server.
 * @param page the page
 * @param failure what the page receives
 * @param skip how many POSTs pass to the real server first (default 0)
 */
export async function failAdds(page: Page, failure: AddFailure, skip = 0): Promise<void> {
  let seen = 0;
  await page.route(ADD_URL, async (route) => {
    if (route.request().method() !== 'POST') return route.continue();
    seen += 1;
    if (seen <= skip) return route.continue();
    if (failure.kind === 'network') return route.abort('failed');
    return fulfillLikeReal(route, failure.status, envelope(failure.code, failure.details));
  });
}

/** Controls a held add request. */
export interface HeldAdd {
  /** POSTs that reached the route (the held one included). */
  readonly count: () => number;
  /** Lets the held POST through to the real server. */
  release(): Promise<void>;
}

/** Holds the first POST of the add form until released; later POSTs pass through. */
export async function holdAdd(page: Page): Promise<HeldAdd> {
  let count = 0;
  let open!: () => void;
  const gate = new Promise<void>((resolve) => { open = resolve; });
  await page.route(ADD_URL, async (route) => {
    if (route.request().method() !== 'POST') return route.continue();
    count += 1;
    if (count > 1) return route.continue();
    await gate;
    return route.continue();
  });
  return { count: () => count, release: async () => open() };
}

/**
 * Counts the replacements of the text of the "Messages" area (a mutation record of the area
 * itself or of its text). Install before `page.goto`.
 * @returns a reader of the count so far
 */
export async function countMessageUpdates(page: Page): Promise<() => Promise<number>> {
  await page.addInitScript(() => {
    const w = window as unknown as { __messageUpdates: number };
    w.__messageUpdates = 0;
    new MutationObserver((records) => {
      for (const record of records) {
        const element = record.target.nodeType === 1 ? (record.target as Element) : record.target.parentElement;
        if (element?.id === 'messages') w.__messageUpdates += 1;
      }
    }).observe(document, { childList: true, characterData: true, subtree: true });
  });
  return () => page.evaluate(() => (window as unknown as { __messageUpdates: number }).__messageUpdates);
}
