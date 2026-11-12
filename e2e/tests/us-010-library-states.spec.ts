import { test, expect } from '../support/fixtures';
import { uniqueTitle } from '../support/builders';
import { failLists, holdLists, type ListFailure } from '../support/stubs';
import { entries, listStatus, filterGroup, choose, addThroughForm } from '../support/view';

test.beforeEach(async ({ library }) => {
  await library.reset();
});

const GENERIC = 'Something went wrong on our side. Try again.';
const FAILURES: Array<{ name: string; failure: ListFailure; text: string }> = [
  { name: '500 INTERNAL_ERROR', failure: { kind: 'status', status: 500, code: 'INTERNAL_ERROR' }, text: GENERIC },
  { name: 'unreadable body', failure: { kind: 'text', status: 200, body: 'not json' }, text: GENERIC },
  { name: 'no response', failure: { kind: 'network' },
    text: 'Cannot reach the server. Check that GloberBooks is running, then try again.' },
  { name: 'FORBIDDEN', failure: { kind: 'status', status: 403, code: 'FORBIDDEN' },
    text: 'This page cannot talk to the server from this address. Open GloberBooks from http://localhost or http://127.0.0.1.' },
  { name: 'MALFORMED_REQUEST', failure: { kind: 'status', status: 400, code: 'MALFORMED_REQUEST' },
    text: 'The request could not be processed. Reload the page and try again.' },
  { name: 'VALIDATION_ERROR without details (SEC-F-197)', failure: { kind: 'status', status: 400, code: 'VALIDATION_ERROR' },
    text: 'The request could not be processed. Reload the page and try again.' },
  { name: 'DUPLICATE_BOOK on a list (SEC-F-197)', text: GENERIC, failure: { kind: 'status', status: 409, code: 'DUPLICATE_BOOK',
    details: { existing_book: { id: '3f2b8c1e-5d4a-4e6f-9a7b-1c2d3e4f5a6b', title: 'x', author: 'y', status: 'unread',
      rating: null, date_added: '2026-03-14T23:30:00.000Z', date_completed: null } } } },
];

for (const { name, failure, text } of FAILURES) {
  test(`US-010.7 AC-1: a failed list load (${name}) shows the 5.9 text and "Try again", then recovers`, async ({ page, library }) => {
    const book = await library.create({ title: uniqueTitle('After failure'), author: 'F' });
    const urls = await failLists(page, failure);
    await page.goto('/');
    await expect(listStatus(page)).toHaveText(text);
    await expect(page.getByRole('button', { name: 'Try again' })).toBeVisible();
    await expect(entries(page)).toHaveCount(0);
    await expect(page.locator('#count')).toBeHidden();
    await expect(page.getByRole('button', { name: 'Show all books' })).toBeHidden();
    await page.getByRole('button', { name: 'Try again' }).click();
    await expect(entries(page)).toHaveCount(1);
    await expect(entries(page).first()).toContainText(book.title);
    await expect(page.getByText('1 book', { exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Try again' })).toBeHidden();
    expect(urls).toHaveLength(2);
  });
}

test('US-010.7 AC-2: "Try again" repeats the request with the active filter', async ({ page, library }) => {
  await library.create({ title: uniqueTitle('Filtered retry'), author: 'F', status: 'reading' });
  await page.goto('/');
  await expect(page.getByText('1 book', { exact: true })).toBeVisible();
  const urls = await failLists(page, { kind: 'status', status: 500, code: 'INTERNAL_ERROR' }, 1);
  await choose(page, 'Reading');
  await expect(page.getByRole('button', { name: 'Try again' })).toBeVisible();
  await expect(entries(page)).toHaveCount(0);
  await page.getByRole('button', { name: 'Try again' }).click();
  await expect(entries(page)).toHaveCount(1);
  expect(urls.map((u) => new URL(u).search)).toEqual(['?status=reading', '?status=reading']);
});

test('US-010.7 AC-3: an add during a held first load is announced, not inserted, and listed by the next load', async ({ page, library }) => {
  const existing = await library.create({ title: uniqueTitle('Existing'), author: 'E', status: 'reading' });
  const held = await holdLists(page, 1);
  await page.goto('/');
  await expect.poll(() => held.pendingCount()).toBe(1);
  expect(held.requests).toHaveLength(1);
  const added = uniqueTitle('Added during load');
  await addThroughForm(page, added);
  await expect(entries(page)).toHaveCount(0);
  await expect(listStatus(page)).toHaveText('Loading your books\u2026');
  await expect(page.locator('#count')).toBeHidden();
  expect(held.requests).toHaveLength(1);
  await held.release(0);
  await expect(entries(page)).toHaveCount(1);
  await expect(entries(page).first()).toContainText(existing.title);
  await expect(page.getByText('1 book', { exact: true })).toBeVisible();
  await choose(page, 'Unread');
  await expect(entries(page)).toHaveCount(1);
  await expect(entries(page).first()).toContainText(added);
  await choose(page, 'All');
  await expect(entries(page)).toHaveCount(2);
  await expect(page.getByText('2 books', { exact: true })).toBeVisible();
});

test('US-010.7 AC-3: an add after a failed load is not inserted; "Try again" lists it', async ({ page }) => {
  await failLists(page, { kind: 'status', status: 500, code: 'INTERNAL_ERROR' }, 1);
  await page.goto('/');
  await expect(page.getByRole('button', { name: 'Try again' })).toBeVisible();
  const added = uniqueTitle('Added after failure');
  await addThroughForm(page, added);
  await expect(entries(page)).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Try again' })).toBeVisible();
  await page.getByRole('button', { name: 'Try again' }).click();
  await expect(entries(page)).toHaveCount(1);
  await expect(entries(page).first()).toContainText(added);
});

test('US-010.10 AC-1: only a load, a filter change or "Try again" requests the list; an add does not', async ({ page, library }) => {
  const held = await holdLists(page, 0);
  await page.goto('/');
  await expect(page.getByText('0 books', { exact: true })).toBeVisible();
  expect(held.requests).toHaveLength(1);
  await addThroughForm(page, uniqueTitle('No reload'));
  await expect(entries(page)).toHaveCount(1);
  await expect(page.getByText('1 book', { exact: true })).toBeVisible();
  expect(held.requests).toHaveLength(1);
  await library.create({ title: uniqueTitle('Unseen'), author: 'U' });
  await choose(page, 'Unread');
  await expect(entries(page)).toHaveCount(2);
  expect(held.requests).toHaveLength(2);
});

for (const order of [[1, 0], [0, 1]]) {
  test(`US-010.10 AC-2: overlapping loads show the last request's result (release order ${order.join(', ')})`, async ({ page, library }) => {
    const unread = await library.create({ title: uniqueTitle('Overlap unread'), author: 'O' });
    await library.create({ title: uniqueTitle('Overlap reading'), author: 'O', status: 'reading' });
    const held = await holdLists(page, 2);
    await page.goto('/');
    await expect.poll(() => held.pendingCount()).toBe(1);
    await choose(page, 'Unread');
    await expect.poll(() => held.pendingCount()).toBe(2);
    expect(held.requests.map((r) => new URL(r.url).search)).toEqual(['', '?status=unread']);
    await held.release(order[0]);
    if (order[0] === 0) {
      await expect(listStatus(page)).toHaveText('Loading your books\u2026');
      await expect(entries(page)).toHaveCount(0);
      await expect(page.locator('#count')).toBeHidden();
    }
    await held.release(order[1]);
    await expect(entries(page)).toHaveCount(1);
    await expect(entries(page).first()).toContainText(unread.title);
    await expect(page.getByText('1 book', { exact: true })).toBeVisible();
    await expect(filterGroup(page).getByRole('radio', { name: 'Unread' })).toBeChecked();
    await expect(listStatus(page)).toHaveText('');
  });
}

test('US-010.9 SEC-F-198: 1,000 and 5,000 maximum-size entries, times recorded', async ({ page, library }, testInfo) => {
  test.setTimeout(600_000);
  const list = page.locator('#book-list > li');
  await library.createMaxSize(1000);
  await page.goto('/');
  const loaded = Date.now();
  await expect(list).toHaveCount(1000, { timeout: 120_000 });
  const thousand = Date.now() - loaded;
  await expect(page.getByText('1000 books', { exact: true })).toBeVisible();
  await library.createMaxSize(4000, 1000);
  const started = Date.now();
  await page.reload();
  await expect(list).toHaveCount(5000, { timeout: 120_000 });
  const firstLoad = Date.now() - started;
  await choose(page, 'Unread');
  const refilter = Date.now();
  await expect(list).toHaveCount(5000, { timeout: 120_000 });
  const rerender = Date.now() - refilter;
  testInfo.annotations.push({ type: 'SEC-F-198 ms (1000 after load, 5000 first load, 5000 re-render)',
    description: `${thousand}, ${firstLoad}, ${rerender}` });
  console.log(`SEC-F-198: 1000 entries ${thousand} ms; 5000 entries first load ${firstLoad} ms, re-render ${rerender} ms`);
  await expect(page.getByText('5000 books', { exact: true })).toBeVisible();
  await library.reset();
});
