import { test, expect } from '../support/fixtures';
import { SCRIPT_TITLE, uniqueTitle } from '../support/builders';
import { entries, messages, choose } from '../support/view';
import { form, fillBook, trackPosts, failAdds, holdAdd, countMessageUpdates, type AddFailure } from '../support/pages';

// The error slot has a visual "Error: " prefix from the stylesheet; the description ends with the message.
const endsWith = (text: string) => new RegExp(`${text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`);

test.beforeEach(async ({ library }) => {
  await library.reset();
});

// Expected texts are literals from the spec (US-011, Section 5.9), never imported from the page's copy module.
const FULL = 'Your library is full (5,000 books). Delete a book to add another.';
const GENERIC = 'Something went wrong on our side. Try again.';
const NO_RESPONSE = 'Cannot reach the server. Check that GloberBooks is running, then try again.';
const REQUEST_FAILED = 'The request could not be processed. Reload the page and try again.';
const STATUS_TEXT = 'Choose Unread, Reading or Completed.';

test('US-011.1 AC-1: labelled fields, Status options with Unread default, button, focus on Title at load', async ({ page }) => {
  await page.goto('/');
  const f = form(page);
  await expect(f.title).toBeVisible();
  await expect(f.author).toBeVisible();
  await expect(f.add).toBeVisible();
  await expect(f.title).toBeFocused();
  await expect(f.status.getByRole('option')).toHaveText(['Unread', 'Reading', 'Completed']);
  await expect(f.status).toHaveValue('unread');
  await expect(f.status.locator('option:checked')).toHaveText('Unread');
});

const CHECKS = [
  { name: 'empty title', title: '', author: 'An Author', field: 'title' },
  { name: 'whitespace-only title', title: '   ', author: 'An Author', field: 'title' },
  { name: 'title of 501 code points', title: 'T'.repeat(501), author: 'An Author', field: 'title' },
  { name: 'empty author', title: 'A Title', author: '', field: 'author' },
  { name: 'author of 201 code points', title: 'A Title', author: 'A'.repeat(201), field: 'author' },
] as const;

for (const { name, title, author, field } of CHECKS) {
  test(`US-011.3 AC-1: ${name} shows a message linked to its field and sends no request`, async ({ page, library }) => {
    const posts = trackPosts(page);
    await page.goto('/');
    await fillBook(page, title, author);
    await form(page).add.click();
    const input = form(page)[field];
    await expect(input).toHaveAttribute('aria-invalid', 'true');
    await expect(input).not.toHaveAccessibleDescription('');
    await expect(input).toBeFocused();
    await expect(page.locator(`#${field}-error`)).toBeVisible();
    await expect(page.locator(`#${field}-error`)).not.toHaveText('');
    await expect(form(page).add).toBeEnabled();
    expect(posts).toHaveLength(0);
    expect((await library.list()).total).toBe(0);
  });
}

test('US-011.3 AC-1: both fields empty show two linked messages, focus on Title, and no request', async ({ page }) => {
  const posts = trackPosts(page);
  const updates = await countMessageUpdates(page);
  await page.goto('/');
  await form(page).add.click();
  await expect(form(page).title).toHaveAttribute('aria-invalid', 'true');
  await expect(form(page).author).toHaveAttribute('aria-invalid', 'true');
  await expect(form(page).title).not.toHaveAccessibleDescription('');
  await expect(form(page).author).not.toHaveAccessibleDescription('');
  await expect(form(page).title).toBeFocused();
  expect(posts).toHaveLength(0);
  expect(await updates(), 'a field-level problem does not touch the Messages area').toBe(0);
});

test('US-011.4 AC-1: a double click sends one request while the response is held', async ({ page, library }) => {
  const held = await holdAdd(page);
  const posts = trackPosts(page);
  await page.goto('/');
  await fillBook(page, uniqueTitle('Double click'), 'Click Author');
  await form(page).add.dblclick();
  await expect(form(page).add).toBeDisabled();
  await expect.poll(held.count).toBe(1);
  await held.release();
  await expect(messages(page)).toContainText('Book added: Double click');
  await expect(form(page).add).toBeEnabled();
  expect(posts).toHaveLength(1);
  expect(held.count()).toBe(1);
  expect((await library.list()).total).toBe(1);
});

test('US-011.4 AC-1: a double Enter sends one request while the response is held', async ({ page, library }) => {
  const held = await holdAdd(page);
  const posts = trackPosts(page);
  await page.goto('/');
  await fillBook(page, uniqueTitle('Double enter'), 'Enter Author');
  await form(page).author.press('Enter');
  await expect(form(page).add).toBeDisabled();
  await form(page).author.press('Enter');
  await form(page).title.press('Enter');
  await expect.poll(held.count).toBe(1);
  await held.release();
  await expect(messages(page)).toContainText('Book added: Double enter');
  expect(posts).toHaveLength(1);
  expect((await library.list()).total).toBe(1);
});

test('US-011.5 AC-1: a 201 inserts the book on top, clears the form, resets Status, confirms and focuses Title', async ({ page, library }) => {
  const older = await library.create({ title: uniqueTitle('Older'), author: 'O' });
  const title = uniqueTitle('Fresh');
  await page.goto('/');
  await expect(entries(page)).toHaveCount(1);
  await fillBook(page, title, 'Fresh Author', 'reading');
  await form(page).add.click();
  await expect(messages(page)).toHaveText(`Book added: ${title}`);
  await expect(entries(page)).toHaveCount(2);
  await expect(entries(page).first()).toContainText(title);
  await expect(entries(page).first()).toContainText('Reading');
  await expect(entries(page).nth(1)).toContainText(older.title);
  await expect(page.getByText('2 books', { exact: true })).toBeVisible();
  await expect(form(page).title).toHaveValue('');
  await expect(form(page).author).toHaveValue('');
  await expect(form(page).status).toHaveValue('unread');
  await expect(form(page).title).toBeFocused();
  await expect(form(page).add).toBeEnabled();
});

test('US-011.5 AC-2: a book outside the active filter is confirmed but not listed', async ({ page, library }) => {
  await library.create({ title: uniqueTitle('Reading one'), author: 'R', status: 'reading' });
  const title = uniqueTitle('Hidden unread');
  await page.goto('/');
  await choose(page, 'Reading');
  await expect(entries(page)).toHaveCount(1);
  await fillBook(page, title, 'U Author');
  await form(page).add.click();
  await expect(messages(page)).toHaveText(`Book added: ${title}`);
  await expect(entries(page)).toHaveCount(1);
  await expect(entries(page).filter({ hasText: title })).toHaveCount(0);
  await expect(form(page).title).toBeFocused();
});

test('US-011.6 AC-1: a forbidden character shows the server message beside the field, keeps values, focuses Title', async ({ page, library }) => {
  const updates = await countMessageUpdates(page);
  await page.goto('/');
  const title = `Zero​width ${uniqueTitle('T-03')}`;
  await fillBook(page, title, 'Plain Author', 'reading');
  const answer = page.waitForResponse((r) => r.request().method() === 'POST' && r.url().endsWith('/api/v1/books'));
  await form(page).add.click();
  const response = await answer;
  expect(response.status()).toBe(400);
  const body = await response.json();
  expect(body.code).toBe('VALIDATION_ERROR');
  const message = body.details.find((d: { field: string }) => d.field === 'title').message as string;
  expect(message.length).toBeGreaterThan(0);
  await expect(page.locator('#title-error')).toHaveText(message);
  await expect(form(page).title).toHaveAccessibleDescription(endsWith(message));
  await expect(form(page).title).toBeFocused();
  await expect(form(page).title).toHaveValue(title);
  await expect(form(page).author).toHaveValue('Plain Author');
  await expect(form(page).status).toHaveValue('reading');
  await expect(form(page).add).toBeEnabled();
  expect(await updates(), 'US-013.4 and CR-5: a field-level failure is zero updates').toBe(0);
  expect((await library.list()).total).toBe(0);
});

test('US-011.6 AC-1: forbidden characters in both fields show both messages and focus the first', async ({ page }) => {
  await page.goto('/');
  await fillBook(page, `Bad‮ title ${uniqueTitle('both')}`, 'Bad​ author');
  await form(page).add.click();
  await expect(page.locator('#title-error')).not.toHaveText('');
  await expect(page.locator('#author-error')).not.toHaveText('');
  await expect(form(page).title).toBeFocused();
  await expect(form(page).author).toHaveAccessibleDescription(/\S/);
});

test('US-011.6 AC-2: a stubbed VALIDATION_ERROR with a status entry shows the Status text beside Status and focuses it', async ({ page }) => {
  await failAdds(page, { kind: 'status', status: 400, code: 'VALIDATION_ERROR',
    details: [{ field: 'status', message: 'ignored by the page' }] });
  const updates = await countMessageUpdates(page);
  await page.goto('/');
  await fillBook(page, uniqueTitle('Status entry'), 'S Author', 'completed');
  await form(page).add.click();
  await expect(page.locator('#status-error')).toHaveText(STATUS_TEXT);
  await expect(form(page).status).toHaveAccessibleDescription(endsWith(STATUS_TEXT));
  await expect(form(page).status).toBeFocused();
  await expect(form(page).status).toHaveValue('completed');
  await expect(form(page).add).toBeEnabled();
  expect(await updates()).toBe(0);
});

test('US-011.6 AC-2: a stubbed VALIDATION_ERROR with title, author and status entries shows each beside its field', async ({ page }) => {
  await failAdds(page, { kind: 'status', status: 400, code: 'VALIDATION_ERROR', details: [
    { field: 'status', message: 'x' }, { field: 'author', message: 'Author is not acceptable.' },
    { field: 'title', message: 'Title is not acceptable.' }] });
  await page.goto('/');
  await fillBook(page, uniqueTitle('Three entries'), 'Three Author');
  await form(page).add.click();
  await expect(page.locator('#title-error')).toHaveText('Title is not acceptable.');
  await expect(page.locator('#author-error')).toHaveText('Author is not acceptable.');
  await expect(page.locator('#status-error')).toHaveText(STATUS_TEXT);
  await expect(form(page).title).toBeFocused();
});

test('US-011.6 AC-2 and US-014.6: hostile field messages from the server are inert text', async ({ page }) => {
  const hostile = '<img src=x onerror=alert(1)>';
  await failAdds(page, { kind: 'status', status: 400, code: 'VALIDATION_ERROR',
    details: [{ field: 'title', message: hostile }] });
  const dialogs: string[] = [];
  page.on('dialog', async (d) => { dialogs.push(d.message()); await d.dismiss(); });
  await page.goto('/');
  await fillBook(page, uniqueTitle('Hostile'), 'H Author');
  await form(page).add.click();
  await expect(page.locator('#title-error')).toHaveText(hostile);
  await expect(page.locator('main img')).toHaveCount(0);
  expect(dialogs).toEqual([]);
});

test('US-011.6 AC-3: a stubbed INVALID_STATUS shows the Status text and keeps the values', async ({ page }) => {
  await failAdds(page, { kind: 'status', status: 400, code: 'INVALID_STATUS' });
  const updates = await countMessageUpdates(page);
  await page.goto('/');
  const title = uniqueTitle('Invalid status');
  await fillBook(page, title, 'I Author', 'reading');
  await form(page).add.click();
  await expect(page.locator('#status-error')).toHaveText(STATUS_TEXT);
  await expect(form(page).status).toBeFocused();
  await expect(form(page).title).toHaveValue(title);
  await expect(form(page).author).toHaveValue('I Author');
  await expect(form(page).status).toHaveValue('reading');
  expect(await updates()).toBe(0);
});

const PAGE_FAILURES: Array<{ id: string; name: string; failure: AddFailure; text: string }> = [
  { id: 'US-011.9', name: 'LIBRARY_FULL', text: FULL, failure: { kind: 'status', status: 409, code: 'LIBRARY_FULL' } },
  { id: 'US-011.10', name: '500 INTERNAL_ERROR', text: GENERIC, failure: { kind: 'status', status: 500, code: 'INTERNAL_ERROR' } },
  { id: 'US-011.10', name: 'no response', text: NO_RESPONSE, failure: { kind: 'network' } },
  { id: 'US-011.10', name: 'MALFORMED_REQUEST', text: REQUEST_FAILED, failure: { kind: 'status', status: 400, code: 'MALFORMED_REQUEST' } },
];

for (const { id, name, failure, text } of PAGE_FAILURES) {
  test(`${id} AC-1: ${name} shows its 5.9 text once, keeps the values and focuses Title`, async ({ page, library }) => {
    await failAdds(page, failure);
    const updates = await countMessageUpdates(page);
    await page.goto('/');
    const title = uniqueTitle('Kept');
    await fillBook(page, title, 'Kept Author', 'completed');
    await form(page).add.click();
    await expect(messages(page)).toHaveText(text);
    await expect(form(page).title).toBeFocused();
    await expect(form(page).title).toHaveValue(title);
    await expect(form(page).author).toHaveValue('Kept Author');
    await expect(form(page).status).toHaveValue('completed');
    await expect(form(page).add).toBeEnabled();
    await expect(entries(page)).toHaveCount(0);
    expect(await updates(), 'US-013.4 and CR-5: one update per page-level failure').toBe(1);
    expect((await library.list()).total).toBe(0);
  });
}

test('US-013.4 AC-1: a failure after an earlier message is one replacement after the separate clearing', async ({ page }) => {
  await failAdds(page, { kind: 'status', status: 500, code: 'INTERNAL_ERROR' }, 1);
  const updates = await countMessageUpdates(page);
  await page.goto('/');
  await fillBook(page, uniqueTitle('First'), 'F');
  await form(page).add.click();
  await expect(messages(page)).toContainText('Book added: First');
  const before = await updates();
  await fillBook(page, uniqueTitle('Second'), 'S');
  await form(page).add.click();
  await expect(messages(page)).toHaveText(GENERIC);
  expect(await updates() - before, 'clearing at the start of the action plus one replacement').toBe(2);
});

test('US-011.6 AC-1: no stale field message after a later success', async ({ page }) => {
  await page.goto('/');
  await form(page).add.click();
  await expect(page.locator('#title-error')).toBeVisible();
  await fillBook(page, uniqueTitle('Recovered'), 'R');
  await form(page).add.click();
  await expect(messages(page)).toContainText('Book added: Recovered');
  await expect(page.locator('#title-error')).toBeHidden();
  await expect(form(page).title).not.toHaveAttribute('aria-invalid', 'true');
});

async function addAndRead(page: import('@playwright/test').Page, title: string, author: string) {
  await page.goto('/');
  await fillBook(page, title, author);
  await form(page).add.click();
  await expect(messages(page)).toContainText('Book added:');
}

test('T-07 via the page: a title of 500 code points is accepted and shown whole', async ({ page, library }) => {
  const tag = uniqueTitle('Long');
  const title = tag + 'T'.repeat(500 - tag.length);
  expect([...title]).toHaveLength(500);
  await addAndRead(page, title, 'Long Author');
  expect(await entries(page).first().locator('.entry-title').textContent()).toBe(title);
  expect((await library.list()).books[0].title).toBe(title);
});

test('T-07 via the page: 500 decomposed "e acute" (1,000 code points typed) are accepted as 500 code points', async ({ page, library }) => {
  const typed = 'é'.repeat(500);
  expect([...typed]).toHaveLength(1000);
  await addAndRead(page, typed, 'Decomposed Author');
  const stored = (await library.list()).books[0].title;
  expect([...stored]).toHaveLength(500);
  expect(stored).toBe('é'.repeat(500));
  expect(await entries(page).first().locator('.entry-title').textContent()).toBe(stored);
});

test('T-08 via the page: a leading U+00A0 and outer spaces are shown trimmed', async ({ page, library }) => {
  const core = uniqueTitle('Trim');
  await addAndRead(page, ` ${core}`, '  Trim Author  ');
  expect(await entries(page).first().locator('.entry-title').textContent()).toBe(core);
  expect(await entries(page).first().locator('.entry-author').textContent()).toBe('Trim Author');
  const stored = (await library.list()).books[0];
  expect(stored.title).toBe(core);
  expect(stored.author).toBe('Trim Author');
});

test('US-014.6 T-12: a hostile title added through the form is rendered as inert text', async ({ page }) => {
  const title = `${SCRIPT_TITLE} ${uniqueTitle('xss add')}`;
  const dialogs: string[] = [];
  page.on('dialog', async (d) => { dialogs.push(d.message()); await d.dismiss(); });
  await addAndRead(page, title, '<b>bold</b>');
  await expect(messages(page)).toHaveText(`Book added: ${title}`);
  await expect(entries(page).first().locator('.entry-title')).toHaveText(title);
  await expect(page.locator('main script')).toHaveCount(0);
  await expect(page.locator('main b')).toHaveCount(0);
  expect(dialogs).toEqual([]);
});

test('T-40 via the page: "Straße" then "STRASSE" are two books, "Clean Code" then "CLEAN CODE" open the dialog', async ({ page, library }) => {
  const tag = uniqueTitle('T40').toLowerCase();
  await library.create({ title: `Clean Code ${tag}`, author: 'b' });
  await page.goto('/');
  await fillBook(page, `Straße ${tag}`, 'a');
  await form(page).add.click();
  await expect(messages(page)).toContainText('Book added:');
  await fillBook(page, `STRASSE ${tag}`, 'a');
  await form(page).add.click();
  await expect(page.getByRole('dialog', { name: 'Possible duplicate' })).toBeHidden();
  await expect(entries(page)).toHaveCount(3);
  await fillBook(page, `CLEAN CODE ${tag}`, 'b');
  await form(page).add.click();
  await expect(page.getByRole('dialog', { name: 'Possible duplicate' })).toBeVisible();
});
