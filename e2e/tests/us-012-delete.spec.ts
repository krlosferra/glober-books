import type { Page } from '@playwright/test';
import { test, expect } from '../support/fixtures';
import { SCRIPT_TITLE, uniqueTitle } from '../support/builders';
import { failRequests, trackRequests } from '../support/requests';
import { expectNoBrowserStorage } from '../support/storage';
import { controls, countText, entries, listStatus, messages } from '../support/view';

test.beforeEach(async ({ library }) => {
  await library.reset();
});

// Expected texts are literals from the spec (US-012.5, .7, US-010.6, Section 5.9), never imported from the page's copy module.
const DELETE = /\/api\/v1\/books\/[^/]+$/;
const LIST = /\/api\/v1\/books$/;
const deleteDialog = (page: Page) => page.getByRole('dialog', { name: 'Delete book' });
const heading = (page: Page) => page.getByRole('heading', { level: 2, name: 'Your books' });
const longTitle = () => {
  const prefix = `${uniqueTitle('Long')} `;
  const title = prefix + 'L'.repeat(500 - [...prefix].length);
  if ([...title].length !== 500) throw new Error('long title must be exactly 500 code points');
  return title;
};

const TITLES = [
  { name: 'a plain title', make: () => uniqueTitle('Plain') },
  { name: 'a hostile title', make: () => `${SCRIPT_TITLE} ${uniqueTitle('Hostile')}` },
  { name: 'a title of 500 code points', make: longTitle },
] as const;

for (const { name, make } of TITLES) {
  test(`US-012.5 AC-1, smoke 6: Delete with confirmation removes the entry, updates the count, announces and focuses the heading (${name})`, async ({ page, library }) => {
    const title = make();
    const stay = uniqueTitle('Stays');
    const book = await library.create({ title, author: 'Del Author' });
    await library.create({ title: stay, author: 'Other' });
    const dialogs: string[] = [];
    page.on('dialog', async (d) => { dialogs.push(d.message()); await d.dismiss(); });
    const requests = trackRequests(page);
    await page.goto('/');
    await expect(entries(page)).toHaveCount(2);
    await expect(page.getByText(countText(2), { exact: true })).toBeVisible();
    await controls(page, title).remove.click();
    const dialog = deleteDialog(page);
    await expect(dialog).toBeVisible();
    await expect(dialog.locator('#delete-text')).toHaveText(`Delete ${title}? This cannot be undone.`);
    await expect(dialog.getByRole('button', { name: 'Cancel' })).toBeFocused();
    expect(requests('DELETE')).toHaveLength(0);
    await dialog.getByRole('button', { name: 'Delete', exact: true }).click();

    await expect(entries(page)).toHaveCount(1);
    await expect(controls(page, stay).entry).toBeVisible();
    await expect(page.getByText(countText(1), { exact: true })).toBeVisible();
    await expect(messages(page)).toHaveText(`Book deleted: ${title}`);
    await expect(heading(page)).toBeFocused();
    expect(requests('DELETE', DELETE)).toHaveLength(1);
    expect(requests('GET', LIST)).toHaveLength(1); // a successful delete does not reload the list (US-010.10)
    expect(await library.find(book.id)).toBeNull();
    expect((await library.list()).total).toBe(1);
    expect(dialogs).toEqual([]);
    await expect(page.locator('main script')).toHaveCount(0);
    await expectNoBrowserStorage(page);
  });
}

for (const how of ['Cancel', 'Escape'] as const) {
  test(`US-012.5 AC-2: ${how} sends no request and focuses the Delete button`, async ({ page, library }) => {
    const title = `${SCRIPT_TITLE} ${uniqueTitle('Keep')}`;
    const book = await library.create({ title, author: 'A' });
    const requests = trackRequests(page);
    await page.goto('/');
    const c = controls(page, title);
    await c.remove.click();
    const dialog = deleteDialog(page);
    await expect(dialog).toBeVisible();
    if (how === 'Cancel') await dialog.getByRole('button', { name: 'Cancel' }).click();
    else await page.keyboard.press('Escape');
    await expect(dialog).toBeHidden();
    await expect(c.remove).toBeFocused();
    await expect(c.entry).toBeVisible();
    await expect(messages(page)).toHaveText('');
    expect(requests('DELETE')).toHaveLength(0);
    expect(await library.find(book.id)).not.toBeNull();
  });
}

test('US-012.7, S-06: deleting a book already deleted behind the page shows the message, removes the entry and reloads the list', async ({ page, library }) => {
  const title = `${SCRIPT_TITLE} ${uniqueTitle('Gone')}`;
  const stay = uniqueTitle('Stays');
  const book = await library.create({ title, author: 'A' });
  await library.create({ title: stay, author: 'B' });
  const requests = trackRequests(page);
  await page.goto('/');
  const c = controls(page, title);
  await expect(entries(page)).toHaveCount(2);
  await library.remove(book.id); // behind the page
  await c.remove.click();
  await deleteDialog(page).getByRole('button', { name: 'Delete', exact: true }).click();
  await expect(messages(page)).toHaveText('This book no longer exists.');
  await expect(c.entry).toHaveCount(0);
  await expect(entries(page)).toHaveCount(1);
  await expect(page.getByText(countText(1), { exact: true })).toBeVisible();
  await expect(heading(page)).toBeFocused();
  expect(requests('DELETE', DELETE)).toHaveLength(1);
  expect(requests('GET', LIST)).toHaveLength(2); // the load and the reload after the 404
  await expectNoBrowserStorage(page);
});

test('US-010.6 AC-1: deleting the last book shows the empty message', async ({ page, library }) => {
  const title = uniqueTitle('Last');
  await library.create({ title, author: 'A' });
  await page.goto('/');
  await controls(page, title).remove.click();
  await deleteDialog(page).getByRole('button', { name: 'Delete', exact: true }).click();
  await expect(messages(page)).toHaveText(`Book deleted: ${title}`);
  await expect(entries(page)).toHaveCount(0);
  await expect(page.getByText(countText(0), { exact: true })).toBeVisible();
  await expect(listStatus(page)).toHaveText('Your reading list is empty. Add your first book above.');
  await expect(heading(page)).toBeFocused();
  expect((await library.list()).total).toBe(0);
});

const FAILURES = [
  { name: '500 INTERNAL_ERROR', failure: { kind: 'status', status: 500, code: 'INTERNAL_ERROR' }, text: 'Something went wrong on our side. Try again.' },
  { name: 'no response', failure: { kind: 'network' }, text: 'Cannot reach the server. Check that GloberBooks is running, then try again.' },
] as const;

for (const { name, failure, text } of FAILURES) {
  test(`US-012.7: a failed delete (${name}) keeps the entry, shows the message beside it and returns focus to Delete`, async ({ page, library }) => {
    const title = uniqueTitle('Fails');
    const book = await library.create({ title, author: 'A' });
    await failRequests(page, 'DELETE', DELETE, failure);
    await page.goto('/');
    const c = controls(page, title);
    await c.remove.click();
    await deleteDialog(page).getByRole('button', { name: 'Delete', exact: true }).click();
    await expect(c.entry.getByText(text, { exact: true })).toBeVisible();
    await expect(c.entry).toBeVisible();
    await expect(c.remove).toBeEnabled();
    await expect(c.remove).toBeFocused();
    await expect(page.getByText(countText(1), { exact: true })).toBeVisible();
    expect(await library.find(book.id)).not.toBeNull();
  });
}
