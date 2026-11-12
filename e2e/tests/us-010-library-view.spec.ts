import { test, expect } from '../support/fixtures';
import { SCRIPT_TITLE, uniqueTitle } from '../support/builders';
import { holdLists, stubListBooks, type WireBook } from '../support/stubs';
import { entries, listStatus, filterGroup, choose } from '../support/view';

test.beforeEach(async ({ library }) => {
  await library.reset();
});

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

const FAR_ZONES = [
  { zone: 'Pacific/Kiritimati', added: '2026-03-15', completed: '2026-03-16' },
  { zone: 'Pacific/Pago_Pago', added: '2026-03-14', completed: '2026-03-14' },
];

for (const { zone, added, completed } of FAR_ZONES) {
  test.describe(`time zone ${zone}`, () => {
    test.use({ timezoneId: zone });
    test(`US-010.3 AC-1: fixed UTC values show the local dates ${added} and ${completed} in ${zone}`, async ({ page }) => {
      const book: WireBook = {
        id: '3f2b8c1e-5d4a-4e6f-9a7b-1c2d3e4f5a6b', title: 'Zone Book', author: 'Zone Author', status: 'completed',
        rating: 5, date_added: '2026-03-14T23:30:00.000Z', date_completed: '2026-03-15T10:30:00.000Z',
      };
      await stubListBooks(page, [book]);
      await page.goto('/');
      await expect(entries(page)).toHaveCount(1);
      await expect(entries(page).first().getByText(`Added: ${added}`, { exact: true })).toBeVisible();
      await expect(entries(page).first().getByText(`Completed: ${completed}`, { exact: true })).toBeVisible();
    });
  });
}

test('US-010.2 and .3 AC-1: loading text in a status region, then entries with their content', async ({ page, library }) => {
  const reading = await library.create({ title: uniqueTitle('Entry'), author: 'Entry Author', status: 'reading' });
  const held = await holdLists(page, 1);
  await page.goto('/');
  await expect.poll(() => held.pendingCount()).toBe(1);
  await expect(listStatus(page)).toHaveText('Loading your books\u2026');
  await expect(listStatus(page)).toHaveAttribute('role', 'status');
  await expect(entries(page)).toHaveCount(0);
  await expect(page.locator('#count')).toBeHidden();
  await held.release(0);
  await expect(entries(page)).toHaveCount(1);
  const entry = entries(page).first();
  await expect(entry).toContainText(reading.title);
  await expect(entry).toContainText('Entry Author');
  await expect(entry.getByText('Reading', { exact: true })).toBeVisible();
  await expect(entry.getByText(/^Added: \d{4}-\d{2}-\d{2}$/)).toBeVisible();
  await expect(entry.getByText(/Completed:|Rating:|Not rated/)).toHaveCount(0);
  await expect(listStatus(page)).toHaveText('');
});

test('US-010.3 AC-3: rating texts for rated, unrated completed and other statuses', async ({ page, library }) => {
  const rated = await library.create({ title: uniqueTitle('Rated'), author: 'R', status: 'completed' });
  await library.rate(rated.id, 4);
  const plain = await library.create({ title: uniqueTitle('Plain'), author: 'P', status: 'completed' });
  const unread = await library.create({ title: uniqueTitle('Unread'), author: 'U' });
  await page.goto('/');
  const byTitle = (title: string) => entries(page).filter({ hasText: title });
  await expect(byTitle(rated.title).getByText('Rating: 4 of 5', { exact: true })).toBeVisible();
  await expect(byTitle(rated.title).getByText(/^Completed: \d{4}-\d{2}-\d{2}$/)).toBeVisible();
  await expect(byTitle(plain.title).getByText('Not rated', { exact: true })).toBeVisible();
  await expect(byTitle(unread.title).getByText(/Rating:|Not rated|Completed:/)).toHaveCount(0);
  await expect(byTitle(unread.title).getByText('Unread', { exact: true })).toBeVisible();
});

test('US-010.8 T-12: hostile strings and 500 code points are shown as inert text', async ({ page, library }) => {
  const dialogs: string[] = [];
  page.on('dialog', async (dialog) => {
    dialogs.push(dialog.message());
    await dialog.dismiss();
  });
  const long = uniqueTitle('Long').padEnd(500, 'W');
  const hostile = [
    { title: `<img src=x onerror=alert(1)> ${uniqueTitle('img')}`, author: '"><script>alert(2)</script>' },
    { title: `"><script>alert(3)</script> ${uniqueTitle('quote')}`, author: '<img src=x onerror=alert(4)>' },
    { title: long, author: 'A'.repeat(200) },
  ];
  for (const input of hostile) await library.create(input);
  await page.goto('/');
  await expect(entries(page)).toHaveCount(3);
  for (const input of hostile) {
    const entry = entries(page).filter({ hasText: input.title.slice(0, 20) });
    await expect(entry.getByText(input.title, { exact: true })).toBeVisible();
    await expect(entry.getByText(input.author, { exact: true })).toBeVisible();
    await expect(entry.locator('img, script')).toHaveCount(0);
  }
  await expect(page.locator('main script, main img')).toHaveCount(0);
  expect(dialogs).toEqual([]);
  // The guard fixture asserts zero CSP violations, console problems and page errors in this flow.
});

test('US-010.4 AC-1: API order, count texts 1 book and n books, no thousands separator', async ({ page, library }) => {
  for (const n of [1, 2, 3]) await library.create({ title: uniqueTitle(`Order${n}`), author: 'O' });
  const { books, total } = await library.list();
  expect(total).toBe(3);
  await page.goto('/');
  await expect(entries(page)).toHaveCount(3);
  for (const [i, book] of books.entries()) await expect(entries(page).nth(i)).toContainText(book.title);
  await expect(page.getByText('3 books', { exact: true })).toBeVisible();
  await library.remove(books[0].id);
  await library.remove(books[1].id);
  await page.reload();
  await expect(page.getByText('1 book', { exact: true })).toBeVisible();
});

test('US-010.5 AC-1: the filter is a group of four radios, All checked, and sends the status parameter', async ({ page, library }) => {
  await library.create({ title: uniqueTitle('Filter U'), author: 'F' });
  const done = await library.create({ title: uniqueTitle('Filter C'), author: 'F', status: 'completed' });
  const held = await holdLists(page, 0);
  await page.goto('/');
  await expect(entries(page)).toHaveCount(2);
  const radios = filterGroup(page).getByRole('radio');
  await expect(radios).toHaveCount(4);
  await expect(radios.nth(0)).toHaveAccessibleName('All');
  await expect(radios.nth(1)).toHaveAccessibleName('Unread');
  await expect(radios.nth(2)).toHaveAccessibleName('Reading');
  await expect(radios.nth(3)).toHaveAccessibleName('Completed');
  await expect(radios.nth(0)).toBeChecked();
  await choose(page, 'Completed');
  await expect(entries(page)).toHaveCount(1);
  await expect(entries(page).first()).toContainText(done.title);
  await expect(page.getByText('1 book', { exact: true })).toBeVisible();
  await choose(page, 'All');
  await expect(entries(page)).toHaveCount(2);
  const urls = held.requests.map((r) => new URL(r.url));
  expect(urls.map((u) => u.search)).toEqual(['', '?status=completed', '']);
});

test('US-010.6 AC-1: the empty library message with "0 books" and no button', async ({ page }) => {
  await page.goto('/');
  await expect(listStatus(page)).toHaveText('Your reading list is empty. Add your first book above.');
  await expect(page.getByText('0 books', { exact: true })).toBeVisible();
  await expect(entries(page)).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Show all books' })).toBeHidden();
});

test('US-010.6 AC-2: the filter-empty message and "Show all books" checks All and loads', async ({ page, library }) => {
  const unread = await library.create({ title: uniqueTitle('Only unread'), author: 'F' });
  await page.goto('/');
  await expect(entries(page)).toHaveCount(1);
  await choose(page, 'Completed');
  await expect(listStatus(page)).toHaveText('No books with this status.');
  await expect(page.getByText('0 books', { exact: true })).toBeVisible();
  await expect(entries(page)).toHaveCount(0);
  await page.getByRole('button', { name: 'Show all books' }).click();
  await expect(filterGroup(page).getByRole('radio', { name: 'All' })).toBeChecked();
  await expect(entries(page)).toHaveCount(1);
  await expect(entries(page).first()).toContainText(unread.title);
  await expect(page.getByRole('button', { name: 'Show all books' })).toBeHidden();
  await expect(listStatus(page)).toHaveText('');
});
