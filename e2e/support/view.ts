import { expect, type Locator, type Page } from '@playwright/test';

/** The list items of the list named "Books". */
export const entries = (page: Page) => page.getByRole('list', { name: 'Books' }).getByRole('listitem');
/** The status region of the list (loading, empty, filter-empty and failure texts). */
export const listStatus = (page: Page) => page.locator('#list-status');
/** The page-level message area named "Messages". */
export const messages = (page: Page) => page.getByRole('status', { name: 'Messages' });
/** The radio group named "Filter by status". */
export const filterGroup = (page: Page) => page.getByRole('group', { name: 'Filter by status' });
/** Checks one filter option by its name. */
export const choose = (page: Page, name: string) => filterGroup(page).getByRole('radio', { name }).check();

/** Adds a book through the form and waits for its confirmation text. */
export async function addThroughForm(page: Page, title: string): Promise<void> {
  await page.getByLabel('Title').fill(title);
  await page.getByLabel('Author').fill('Form Author');
  await page.getByRole('button', { name: 'Add book' }).click();
  await expect(messages(page)).toHaveText(`Book added: ${title}`);
}

/** The entry of a book, found by its (unique) title inside the list named "Books". */
export const entryOf = (page: Page, title: string) => entries(page).filter({ hasText: title });

/** The controls of one entry, by accessible name (US-012.1, .4, .5). */
export const controls = (page: Page, title: string) => {
  const entry = entryOf(page, title);
  const group = entry.getByRole('group', { name: `Rating of ${title}`, exact: true });
  return {
    entry,
    status: entry.getByRole('combobox', { name: `Status of ${title}`, exact: true }),
    group,
    star: (n: number) => group.getByRole('radio', { name: String(n), exact: true }),
    clear: entry.getByRole('button', { name: `Clear rating of ${title}`, exact: true }),
    remove: entry.getByRole('button', { name: `Delete ${title}`, exact: true }),
  };
};

/** The count text of US-010.4. */
export const countText = (n: number) => (n === 1 ? '1 book' : `${n} books`);

/** The local calendar date (YYYY-MM-DD) of an ISO instant, as US-010.3 shows it. */
export function localDay(iso: string): string {
  const d = new Date(iso);
  const two = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${two(d.getMonth() + 1)}-${two(d.getDate())}`;
}

/** Changes the status of an entry: the select is focused first (selectOption does not focus it). */
export async function chooseStatus(page: Page, title: string, status: string): Promise<void> {
  const { status: select } = controls(page, title);
  await select.focus();
  await select.selectOption(status);
}

/** The status label shown as text of an entry (not the option of the status control). */
export const shownStatus = (entry: Locator, label: string) =>
  entry.getByRole('paragraph').filter({ hasText: new RegExp(`^${label}$`) });
