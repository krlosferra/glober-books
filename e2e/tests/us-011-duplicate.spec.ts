import { test, expect } from '../support/fixtures';
import { SCRIPT_TITLE, uniqueTitle } from '../support/builders';
import { entries, messages } from '../support/view';
import { form, duplicate, fillBook, trackPosts, failAdds, countMessageUpdates, type AddFailure } from '../support/pages';
import type { Page } from '@playwright/test';

test.beforeEach(async ({ library }) => {
  await library.reset();
});

// Expected texts are literals from the spec (US-011.7, Section 5.9), never imported from the page's copy module.
const DIALOG_TEXT = 'This book may already exist. Add anyway?';

/** Opens the page and submits the same title and author until the dialog opens. */
async function openDialog(page: Page, title: string, author: string) {
  await page.goto('/');
  await fillBook(page, title, author);
  await form(page).add.click();
  await expect(duplicate(page).dialog).toBeVisible();
}

test('US-011.7 AC-1: the dialog shows the text and the existing book; initial focus on Cancel', async ({ page, library }) => {
  const title = uniqueTitle('Dup');
  await library.create({ title, author: 'Dup Author', status: 'reading' });
  const updates = await countMessageUpdates(page);
  await page.goto('/');
  await fillBook(page, `  ${title.toUpperCase()}  `, 'DUP AUTHOR', 'completed');
  await form(page).add.click();
  const d = duplicate(page);
  await expect(d.dialog).toBeVisible();
  await expect(d.dialog.getByRole('heading', { name: 'Possible duplicate' })).toBeVisible();
  await expect(d.dialog.getByText(DIALOG_TEXT, { exact: true })).toBeVisible();
  await expect(d.dialog.getByText(title, { exact: true })).toBeVisible();
  await expect(d.dialog.getByText('Dup Author', { exact: true })).toBeVisible();
  await expect(d.dialog.getByText('Reading', { exact: true })).toBeVisible();
  await expect(d.dialog.getByText(/^Added: \d{4}-\d{2}-\d{2}$/)).toBeVisible();
  await expect(d.dialog).toHaveAccessibleDescription(DIALOG_TEXT);
  await expect(d.cancel).toBeFocused();
  await expect(d.confirm).toBeVisible();
  await expect(form(page).add).toBeDisabled();
  expect(await updates(), 'the dialog is not a Messages update').toBe(0);
});

test('US-011.7 AC-1: Escape cancels, keeps the values, focuses Title and adds nothing', async ({ page, library }) => {
  const title = uniqueTitle('Escape');
  await library.create({ title, author: 'Esc Author' });
  const posts = trackPosts(page);
  await page.goto('/');
  await fillBook(page, title, 'Esc Author', 'completed');
  await form(page).add.click();
  await expect(duplicate(page).dialog).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(duplicate(page).dialog).toBeHidden();
  await expect(form(page).title).toBeFocused();
  await expect(form(page).title).toHaveValue(title);
  await expect(form(page).author).toHaveValue('Esc Author');
  await expect(form(page).status).toHaveValue('completed');
  await expect(form(page).add).toBeEnabled();
  expect(posts).toHaveLength(1);
  expect((await library.list()).total).toBe(1);
});

test('US-011.8 AC-1: Cancel closes, keeps the values, focuses Title and adds nothing', async ({ page, library }) => {
  const title = uniqueTitle('Cancel');
  await library.create({ title, author: 'Can Author' });
  const posts = trackPosts(page);
  const updates = await countMessageUpdates(page);
  await page.goto('/');
  await fillBook(page, title, 'Can Author', 'reading');
  await form(page).add.click();
  await duplicate(page).cancel.click();
  await expect(duplicate(page).dialog).toBeHidden();
  await expect(form(page).title).toBeFocused();
  await expect(form(page).title).toHaveValue(title);
  await expect(form(page).author).toHaveValue('Can Author');
  await expect(form(page).status).toHaveValue('reading');
  await expect(entries(page)).toHaveCount(1);
  expect(posts).toHaveLength(1);
  expect((await library.list()).total).toBe(1);
  expect(await updates()).toBe(0);
});

test('US-011.8 AC-2 and smoke 4: Add anyway resends with allow_duplicate and adds a second book', async ({ page, library }) => {
  const title = uniqueTitle('Anyway');
  await library.create({ title, author: 'Any Author' });
  const posts = trackPosts(page);
  await page.goto('/');
  await expect(entries(page)).toHaveCount(1);
  await fillBook(page, title, 'Any Author', 'reading');
  await form(page).add.click();
  await duplicate(page).confirm.click();
  await expect(duplicate(page).dialog).toBeHidden();
  await expect(entries(page)).toHaveCount(2);
  await expect(entries(page).first()).toContainText('Reading');
  await expect(messages(page)).toHaveText(`Book added: ${title}`);
  await expect(page.getByText('2 books', { exact: true })).toBeVisible();
  await expect(form(page).title).toHaveValue('');
  await expect(form(page).status).toHaveValue('unread');
  await expect(form(page).title).toBeFocused();
  await expect(form(page).add).toBeEnabled();
  expect(posts.map((p) => p.body.allow_duplicate)).toEqual([undefined, true]);
  expect(posts[1].body).toMatchObject({ title, author: 'Any Author', status: 'reading' });
  const { books, total } = await library.list();
  expect(total).toBe(2);
  expect(books.map((b) => b.title)).toEqual([title, title]);
});

const AFTER_ANYWAY: Array<{ name: string; failure: AddFailure; text: string }> = [
  { name: 'LIBRARY_FULL', failure: { kind: 'status', status: 409, code: 'LIBRARY_FULL' },
    text: 'Your library is full (5,000 books). Delete a book to add another.' },
  { name: '500 INTERNAL_ERROR', failure: { kind: 'status', status: 500, code: 'INTERNAL_ERROR' },
    text: 'Something went wrong on our side. Try again.' },
  { name: 'no response', failure: { kind: 'network' },
    text: 'Cannot reach the server. Check that GloberBooks is running, then try again.' },
];

for (const { name, failure, text } of AFTER_ANYWAY) {
  test(`US-011.8 AC-3 and US-011.9: Add anyway ending in ${name} shows its text once, keeps the values, focuses Title`, async ({ page, library }) => {
    const title = uniqueTitle('Anyway fail');
    await library.create({ title, author: 'AF Author' });
    await failAdds(page, failure, 1);
    const updates = await countMessageUpdates(page);
    await openDialog(page, title, 'AF Author');
    await duplicate(page).confirm.click();
    await expect(messages(page)).toHaveText(text);
    await expect(duplicate(page).dialog).toBeHidden();
    await expect(form(page).title).toBeFocused();
    await expect(form(page).title).toHaveValue(title);
    await expect(form(page).author).toHaveValue('AF Author');
    await expect(form(page).add).toBeEnabled();
    await expect(entries(page)).toHaveCount(1);
    expect(await updates()).toBe(1);
    expect((await library.list()).total).toBe(1);
  });
}

test('US-011.8 AC-3 and US-011.6: Add anyway ending in a VALIDATION_ERROR shows the field message and focuses the field', async ({ page, library }) => {
  const title = uniqueTitle('Anyway invalid');
  await library.create({ title, author: 'AV Author' });
  await failAdds(page, { kind: 'status', status: 400, code: 'VALIDATION_ERROR',
    details: [{ field: 'author', message: 'Author is not acceptable.' }] }, 1);
  const updates = await countMessageUpdates(page);
  await openDialog(page, title, 'AV Author');
  await duplicate(page).confirm.click();
  await expect(page.locator('#author-error')).toHaveText('Author is not acceptable.');
  await expect(form(page).author).toBeFocused();
  await expect(form(page).title).toHaveValue(title);
  await expect(form(page).add).toBeEnabled();
  expect(await updates()).toBe(0);
});

const IN_DIALOG = /^duplicate-(cancel|confirm)$/;
const focusedId = (page: Page) => () => page.evaluate(() => document.activeElement?.id ?? '');

test('US-014.2 CR-6: after a click on the dialog text, repeated Tab stays inside the two buttons', async ({ page, library }) => {
  const title = uniqueTitle('Trap');
  await library.create({ title, author: 'Trap Author' });
  await openDialog(page, title, 'Trap Author');
  await duplicate(page).dialog.getByText(DIALOG_TEXT, { exact: true }).click();
  for (let i = 0; i < 5; i += 1) {
    await page.keyboard.press('Tab');
    await expect.poll(focusedId(page), { message: `focus after Tab ${i + 1}` }).toMatch(IN_DIALOG);
  }
  await expect(duplicate(page).dialog).toBeVisible();
});

// FINDING (Task 29): focus leaves the two buttons (the active element is not a dialog button) on
// Shift+Tab after a click on non-interactive dialog text; the Tab-only trap wraps only from a button.
// test.fail() keeps the strict assertion and turns red when the production defect is fixed.
test('US-014.2 CR-6: after a click on the dialog text, Shift+Tab stays inside the two buttons', async ({ page, library }) => {
  test.fail();
  const title = uniqueTitle('Trap back');
  await library.create({ title, author: 'Trap Author' });
  await openDialog(page, title, 'Trap Author');
  await duplicate(page).dialog.getByText(title, { exact: true }).click();
  for (let i = 0; i < 5; i += 1) {
    await page.keyboard.press('Shift+Tab');
    await expect.poll(focusedId(page), { message: `focus after Shift+Tab ${i + 1}` }).toMatch(IN_DIALOG);
  }
  await expect(duplicate(page).dialog).toBeVisible();
});

test('US-014.2: a very short viewport keeps both dialog buttons reachable', async ({ page, library }) => {
  const title = uniqueTitle('Short');
  await library.create({ title, author: 'Short Author' });
  await page.setViewportSize({ width: 320, height: 90 });
  await openDialog(page, title, 'Short Author');
  await expect(duplicate(page).cancel).toBeInViewport({ ratio: 1 });
  await expect(duplicate(page).confirm).toBeInViewport({ ratio: 1 });
  await duplicate(page).confirm.click();
  await expect(entries(page)).toHaveCount(2);
});

test('US-014.6 T-12: a hostile existing title in the dialog is inert text', async ({ page, library }) => {
  const title = `${SCRIPT_TITLE} ${uniqueTitle('xss dup')}`;
  await library.create({ title, author: '<b>bold</b>' });
  const dialogs: string[] = [];
  page.on('dialog', async (d) => { dialogs.push(d.message()); await d.dismiss(); });
  await openDialog(page, title, '<b>bold</b>');
  await expect(duplicate(page).dialog.getByText(title, { exact: true })).toBeVisible();
  await expect(duplicate(page).dialog.getByText('<b>bold</b>', { exact: true })).toBeVisible();
  await expect(page.locator('dialog script, dialog b')).toHaveCount(0);
  expect(dialogs).toEqual([]);
});

test('US-014.6: a 500-code-point existing title in the dialog is shown whole and contained', async ({ page, library }) => {
  const tag = uniqueTitle('Max');
  const title = tag + 'W'.repeat(500 - tag.length);
  await library.create({ title, author: 'M'.repeat(200) });
  await page.setViewportSize({ width: 320, height: 90 });
  await openDialog(page, title, 'M'.repeat(200));
  expect(await page.locator('#duplicate-title').textContent()).toBe(title);
  expect(await page.locator('#duplicate-author').textContent()).toBe('M'.repeat(200));
  const noOverflow = await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth);
  expect(noOverflow, 'no horizontal scroll at 320 px').toBe(true);
  await expect(duplicate(page).confirm).toBeInViewport({ ratio: 1 });
});
