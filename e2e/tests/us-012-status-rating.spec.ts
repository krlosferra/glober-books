import type { Page } from '@playwright/test';
import { test, expect } from '../support/fixtures';
import { SCRIPT_TITLE, uniqueTitle } from '../support/builders';
import { trackRequests } from '../support/requests';
import { choose, chooseStatus, controls, countText, entries, listStatus, localDay, messages, shownStatus } from '../support/view';

test.beforeEach(async ({ library }) => {
  await library.reset();
});

// Expected texts are literals from the spec (US-010.3, US-012, Section 5.9), never imported from the page's copy module.
const STATUS_PUT = /\/status$/;
const RATING_PUT = /\/rating$/;
const confirmDialog = (page: Page) => page.getByRole('dialog', { name: 'Confirm status change' });

test('US-012.1, .2, .4, smoke 5: change to completed, rate, replace, clear; rating control only on completed', async ({ page, library }) => {
  const title = `${SCRIPT_TITLE} ${uniqueTitle('Flow')}`;
  const other = uniqueTitle('Other');
  const book = await library.create({ title, author: 'Flow Author' });
  await library.create({ title: other, author: 'Other Author', status: 'reading' });
  const puts = trackRequests(page);
  await page.goto('/');
  const c = controls(page, title);
  await expect(entries(page)).toHaveCount(2);
  await expect(c.status.getByRole('option')).toHaveText(['Unread', 'Reading', 'Completed']);
  await expect(c.group).toHaveCount(0);
  await expect(controls(page, other).group).toHaveCount(0);

  await chooseStatus(page, title, 'completed');
  await expect(shownStatus(c.entry, 'Completed')).toBeVisible();
  const completed = await library.find(book.id);
  expect(completed?.status).toBe('completed');
  expect(completed?.date_completed).not.toBeNull();
  await expect(c.entry).toContainText(`Completed: ${localDay(completed!.date_completed!)}`);
  await expect(c.entry).toContainText('Not rated');
  await expect(c.group).toBeVisible();
  await expect(c.clear).toHaveCount(0);
  await expect(controls(page, other).group).toHaveCount(0);
  expect(puts('PUT', STATUS_PUT)).toHaveLength(1);

  await c.star(4).check();
  await expect(c.entry).toContainText('Rating: 4 of 5');
  await expect(c.star(4)).toBeChecked();
  await expect(c.clear).toBeVisible();
  expect((await library.find(book.id))?.rating).toBe(4);

  await c.star(2).check();
  await expect(c.entry).toContainText('Rating: 2 of 5');
  await expect(c.star(2)).toBeChecked();
  await expect(c.star(4)).not.toBeChecked();
  expect((await library.find(book.id))?.rating).toBe(2);
  expect(puts('PUT', RATING_PUT)).toHaveLength(2);

  await c.clear.click();
  await expect(c.entry).toContainText('Not rated');
  await expect(c.entry).not.toContainText('Rating: ');
  for (const n of [1, 2, 3, 4, 5]) await expect(c.star(n)).not.toBeChecked();
  await expect(c.clear).toHaveCount(0);
  expect(puts('DELETE', RATING_PUT)).toHaveLength(1);
  const cleared = await library.find(book.id);
  expect(cleared?.rating).toBeNull();
  expect(cleared?.date_completed).toBe(completed?.date_completed);
});

test('US-012.2 AC-1: the rating control disappears after a change away from completed and exists only on completed entries', async ({ page, library }) => {
  const title = uniqueTitle('Away');
  await library.create({ title, author: 'A', status: 'completed' });
  await page.goto('/');
  const c = controls(page, title);
  await expect(c.group).toBeVisible();
  await chooseStatus(page, title, 'reading'); // no rating: no dialog (US-012.3)
  await expect(shownStatus(c.entry, 'Reading')).toBeVisible();
  await expect(confirmDialog(page)).toHaveCount(0);
  await expect(c.group).toHaveCount(0);
  await chooseStatus(page, title, 'unread');
  await expect(shownStatus(c.entry, 'Unread')).toBeVisible();
  await expect(c.group).toHaveCount(0);
});

for (const target of ['Unread', 'Reading'] as const) {
  test(`US-012.3 AC-1, S-02: a rated book moved back to ${target} asks first; Continue clears rating and date as returned`, async ({ page, library }) => {
    const title = `${SCRIPT_TITLE} ${uniqueTitle('Back')}`;
    const book = await library.create({ title, author: 'A', status: 'completed' });
    await library.rate(book.id, 4);
    const puts = trackRequests(page);
    await page.goto('/');
    const c = controls(page, title);
    await expect(c.entry).toContainText('Rating: 4 of 5');
    await chooseStatus(page, title, target.toLowerCase());
    const dialog = confirmDialog(page);
    await expect(dialog).toBeVisible();
    await expect(dialog.locator('#status-text')).toHaveText(`Moving ${title} back to ${target} removes its rating and completion date.`);
    await expect(dialog.getByRole('button', { name: 'Cancel' })).toBeFocused();
    expect(puts('PUT', STATUS_PUT)).toHaveLength(0);
    await dialog.getByRole('button', { name: 'Continue' }).click();
    await expect(shownStatus(c.entry, target)).toBeVisible();
    await expect(c.entry).not.toContainText('Completed: ');
    await expect(c.entry).not.toContainText('Rating: ');
    await expect(c.entry).not.toContainText('Not rated');
    await expect(c.group).toHaveCount(0);
    expect(puts('PUT', STATUS_PUT)).toHaveLength(1);
    const after = await library.find(book.id);
    expect(after).toMatchObject({ status: target.toLowerCase(), rating: null, date_completed: null });
    await expect(c.status).toBeFocused();
  });
}

for (const how of ['Cancel', 'Escape'] as const) {
  test(`US-012.3 AC-2, S-03: ${how} restores the previous choice without a request`, async ({ page, library }) => {
    const title = uniqueTitle('Keep');
    const book = await library.create({ title, author: 'A', status: 'completed' });
    const rated = await library.rate(book.id, 3);
    const puts = trackRequests(page);
    await page.goto('/');
    const c = controls(page, title);
    await chooseStatus(page, title, 'reading');
    const dialog = confirmDialog(page);
    await expect(dialog).toBeVisible();
    if (how === 'Cancel') await dialog.getByRole('button', { name: 'Cancel' }).click();
    else await page.keyboard.press('Escape');
    await expect(dialog).toBeHidden();
    await expect(c.status).toHaveValue('completed'); // wait for the restored value
    await expect(c.status).toBeFocused();
    await expect(c.entry).toContainText('Rating: 3 of 5');
    await expect(c.star(3)).toBeChecked();
    expect(puts('PUT', STATUS_PUT)).toHaveLength(0);
    expect(await library.find(book.id)).toMatchObject({ status: 'completed', rating: 3, date_completed: rated.date_completed });
  });
}

test('US-012.3, S-03: moving back and then to completed again gives a new completion date and no rating', async ({ page, library }) => {
  const title = uniqueTitle('Again');
  const book = await library.create({ title, author: 'A', status: 'completed' });
  await library.rate(book.id, 5);
  await page.goto('/');
  const c = controls(page, title);
  await chooseStatus(page, title, 'unread');
  await confirmDialog(page).getByRole('button', { name: 'Continue' }).click();
  await expect(c.group).toHaveCount(0);
  await chooseStatus(page, title, 'completed');
  await expect(c.group).toBeVisible();
  const after = await library.find(book.id);
  expect(after?.rating).toBeNull();
  expect(after?.date_completed).not.toBeNull();
  await expect(c.entry).toContainText(`Completed: ${localDay(after!.date_completed!)}`);
  await expect(c.entry).toContainText('Not rated');
});

test('US-012.3: a completed book without a rating changes without a dialog', async ({ page, library }) => {
  const title = uniqueTitle('Plain');
  const book = await library.create({ title, author: 'A', status: 'completed' });
  await page.goto('/');
  await chooseStatus(page, title, 'unread');
  await expect(shownStatus(controls(page, title).entry, 'Unread')).toBeVisible();
  await expect(confirmDialog(page)).toBeHidden();
  expect((await library.find(book.id))?.status).toBe('unread');
});

test('US-012.4: the rating group is a labelled group operable by keyboard', async ({ page, library }) => {
  const title = uniqueTitle('Keys');
  const book = await library.create({ title, author: 'A', status: 'completed' });
  await page.goto('/');
  const c = controls(page, title);
  await c.star(1).focus();
  await page.keyboard.press('ArrowRight'); // moves to 2 and checks it, as native radios do
  await expect(c.entry).toContainText('Rating: 2 of 5');
  await expect(c.star(2)).toBeChecked();
  expect((await library.find(book.id))?.rating).toBe(2);
});

test('US-012.8: with a filter, a changed entry stays and shows "No longer matches this filter" until the next load', async ({ page, library }) => {
  const title = uniqueTitle('Filtered');
  const keep = uniqueTitle('Stays');
  await library.create({ title: keep, author: 'A' });
  await library.create({ title, author: 'A' });
  await page.goto('/');
  await choose(page, 'Unread');
  await expect(entries(page)).toHaveCount(2);
  const lists = trackRequests(page);
  await chooseStatus(page, title, 'reading');
  const c = controls(page, title);
  await expect(c.entry.getByText('No longer matches this filter', { exact: true })).toBeVisible();
  await expect(shownStatus(c.entry, 'Reading')).toBeVisible();
  await expect(page.getByText(countText(2), { exact: true })).toBeVisible();
  await expect(controls(page, keep).entry.getByText('No longer matches this filter')).toHaveCount(0);
  expect(lists('GET', /\/api\/v1\/books$/)).toHaveLength(0);
  await choose(page, 'Reading');
  await expect(c.entry.getByText('No longer matches this filter')).toHaveCount(0);
  await choose(page, 'Unread');
  await expect(entries(page)).toHaveCount(1);
  await expect(controls(page, keep).entry).toBeVisible();
});

test('US-012.3 and S-04, S-11: a same-state choice sends no request and changes nothing', async ({ page, library }) => {
  const done = uniqueTitle('SameDone');
  const read = uniqueTitle('SameReading');
  const fresh = uniqueTitle('SameUnread');
  const doneBook = await library.create({ title: done, author: 'A', status: 'completed' });
  await library.rate(doneBook.id, 4);
  const readBook = await library.create({ title: read, author: 'A', status: 'reading' });
  const freshBook = await library.create({ title: fresh, author: 'A' });
  const before = await library.list();
  const puts = trackRequests(page);
  await page.goto('/');
  await expect(entries(page)).toHaveCount(3);
  await chooseStatus(page, done, 'completed');
  await chooseStatus(page, read, 'reading');
  await chooseStatus(page, fresh, 'unread');
  await expect(confirmDialog(page)).toBeHidden();
  await expect(controls(page, done).entry).toContainText('Rating: 4 of 5');
  await expect(controls(page, done).status).toHaveValue('completed');
  await expect(controls(page, read).status).toHaveValue('reading');
  await expect(controls(page, fresh).status).toHaveValue('unread');
  await expect(messages(page)).toHaveText('');
  expect(puts('PUT')).toHaveLength(0);
  expect((await library.list()).books).toEqual(before.books);
  expect(await library.find(doneBook.id)).toMatchObject({ rating: 4 });
  expect(await library.find(readBook.id)).toMatchObject({ status: 'reading' });
  expect(await library.find(freshBook.id)).toMatchObject({ status: 'unread' });
});

test('US-012.7, S-01: rating a book changed behind the page shows the message and reloads the entry', async ({ page, library }) => {
  const title = `${SCRIPT_TITLE} ${uniqueTitle('Stale')}`;
  const book = await library.create({ title, author: 'A', status: 'completed' });
  const requests = trackRequests(page);
  await page.goto('/');
  const c = controls(page, title);
  await expect(c.group).toBeVisible();
  await library.setStatus(book.id, 'reading'); // behind the page
  await c.star(3).check();
  await expect(c.entry.getByText('Only completed books can be rated.', { exact: true })).toBeVisible();
  await expect(shownStatus(c.entry, 'Reading')).toBeVisible();
  await expect(c.group).toHaveCount(0);
  await expect(c.status).toHaveValue('reading');
  expect(requests('PUT', RATING_PUT)).toHaveLength(1);
  expect(requests('GET', new RegExp(`/api/v1/books/${book.id}$`))).toHaveLength(1);
  expect(await library.find(book.id)).toMatchObject({ status: 'reading', rating: null });
  // F-37-1, P-F2: the rating radio is gone, so focus falls back to the status select.
  await expect(page.getByLabel(`Status of ${title}`, { exact: true })).toBeFocused();
});

for (const action of ['status', 'rating', 'clear'] as const) {
  test(`US-012.7, S-05, S-06: acting (${action}) on a book deleted behind the page removes the entry and reloads the list`, async ({ page, library }) => {
    const title = uniqueTitle('Gone');
    const stay = uniqueTitle('Stays');
    const book = await library.create({ title, author: 'A', status: 'completed' });
    await library.create({ title: stay, author: 'A' });
    if (action === 'clear') await library.rate(book.id, 2);
    const requests = trackRequests(page);
    await page.goto('/');
    const c = controls(page, title);
    await expect(entries(page)).toHaveCount(2);
    await library.remove(book.id); // behind the page
    if (action === 'status') await chooseStatus(page, title, 'unread');
    else if (action === 'rating') await c.star(5).check();
    else await c.clear.click();
    if (action === 'status') await expect(confirmDialog(page)).toBeHidden();
    await expect(messages(page)).toHaveText('This book no longer exists.');
    await expect(c.entry).toHaveCount(0);
    await expect(entries(page)).toHaveCount(1);
    await expect(page.getByText(countText(1), { exact: true })).toBeVisible();
    expect(requests('GET', /\/api\/v1\/books$/)).toHaveLength(2); // the load and the reload after 404
    expect(await library.find(book.id)).toBeNull();
    await expect(listStatus(page)).not.toHaveText(/empty/);
  });
}
