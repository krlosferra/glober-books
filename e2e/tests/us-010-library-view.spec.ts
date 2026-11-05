import { test, expect } from '../support/fixtures';
import { SCRIPT_TITLE, uniqueTitle } from '../support/builders';

// Expected texts come from the spec: US-010.4 count wording, US-011.2 keyboard flow, US-014.6 inert text.
function countWording(n: number): string {
  return n === 1 ? '1 book' : `${n} books`;
}

test('US-010.1 AC-1: the page loads with the title GloberBooks and exactly one h1 @smoke', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveTitle('GloberBooks');
  await expect(page.getByRole('heading', { level: 1 })).toHaveCount(1);
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('GloberBooks');
  await expect(page.getByRole('heading', { level: 2, name: 'Your books' })).toBeVisible();
});

test('US-011.2 and M-1: a keyboard-only add shows the entry and the count @smoke', async ({ page, library }, testInfo) => {
  const before = (await library.list()).total;
  const title = uniqueTitle('Keyboard');
  await page.goto('/');
  await expect(page.getByText(countWording(before), { exact: true })).toBeVisible();
  await expect(page.getByLabel('Title')).toBeFocused();

  const started = Date.now();
  await page.keyboard.type(title);
  await page.keyboard.press('Tab');
  await page.keyboard.type('Keyboard Author');
  await page.keyboard.press('Enter');
  const entry = page.getByRole('list', { name: 'Books' }).getByRole('listitem').filter({ hasText: title });
  await expect(entry).toBeVisible();
  const elapsed = Date.now() - started;

  await expect(entry).toContainText('Keyboard Author');
  await expect(entry).toContainText('Unread');
  await expect(page.getByText(countWording(before + 1), { exact: true })).toBeVisible();
  testInfo.annotations.push({ type: 'M-1 keyboard add (ms)', description: String(elapsed) });
  console.log(`M-1 keyboard add took ${elapsed} ms`);
  expect(elapsed, 'M-1 bound, generous (spec target is 10 s for the user)').toBeLessThan(10_000);
});

test('US-014.6 T-12: <script>alert(1)</script> as a title is rendered as inert text @smoke', async ({ page, library }) => {
  const title = `${SCRIPT_TITLE} ${uniqueTitle('xss')}`;
  const dialogs: string[] = [];
  page.on('dialog', async (dialog) => {
    dialogs.push(dialog.message());
    await dialog.dismiss();
  });
  await library.create({ title, author: 'Hostile Author' });
  await page.goto('/');
  const entry = page.getByRole('list', { name: 'Books' }).getByRole('listitem').filter({ hasText: title });
  await expect(entry).toBeVisible();
  await expect(entry.getByText(title, { exact: true })).toBeVisible();
  await expect(page.locator('main script')).toHaveCount(0);
  await expect(entry.locator('script')).toHaveCount(0);
  expect(dialogs).toEqual([]);
  // The guard fixture asserts no CSP violation, console error or page error at the end.
});
