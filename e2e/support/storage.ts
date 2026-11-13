import { expect, type Page } from '@playwright/test';

/**
 * Asserts that the page stored nothing in the browser (SEC-F-138): localStorage, sessionStorage
 * and cookies are all empty. Call at the end of a flow.
 * @param page the page of the flow
 */
export async function expectNoBrowserStorage(page: Page): Promise<void> {
  const stored = await page.evaluate(() => ({
    local: localStorage.length,
    session: sessionStorage.length,
    cookie: document.cookie,
  }));
  expect(stored, 'localStorage, sessionStorage and document.cookie').toEqual({ local: 0, session: 0, cookie: '' });
  expect(await page.context().cookies(), 'cookies of the context').toEqual([]);
}
