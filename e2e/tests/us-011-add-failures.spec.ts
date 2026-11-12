import { test, expect } from '../support/fixtures';
import { uniqueTitle } from '../support/builders';
import { entries, messages } from '../support/view';
import { form, fillBook, failAdds, countMessageUpdates, type AddFailure } from '../support/pages';

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
