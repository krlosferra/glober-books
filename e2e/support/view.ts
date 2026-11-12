import { expect, type Page } from '@playwright/test';

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
