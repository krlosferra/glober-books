import AxeBuilder from '@axe-core/playwright';
import type { Page } from '@playwright/test';
import { test, expect } from '../support/fixtures';
import { uniqueTitle } from '../support/builders';
import { duplicate, form } from '../support/pages';
import { chooseStatus, controls, entries } from '../support/view';

// No bypassCSP anywhere: the scan runs under the application's own policy (Task 43, criterion 3),
// and the guard fixture stays active, so any CSP violation also fails these tests.
const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'];

/** Scans the page and returns the serious and critical violations as readable strings. */
async function seriousViolations(page: Page): Promise<string[]> {
  const result = await new AxeBuilder({ page }).withTags(TAGS).analyze();
  expect(result.passes.length, 'axe ran and evaluated rules').toBeGreaterThan(0);
  return result.violations
    .filter((v) => v.impact === 'serious' || v.impact === 'critical')
    .map((v) => `${v.id} (${v.impact}): ${v.nodes.map((n) => n.target.join(' ')).join(' | ')}`);
}

/** Every form control, button and the lists' controls must have a non-empty accessible name. */
async function unlabelledControls(page: Page): Promise<string[]> {
  return page.evaluate(() => {
    const missing: string[] = [];
    const selector = 'input, select, textarea, button, dialog';
    for (const el of Array.from(document.querySelectorAll<HTMLElement>(selector))) {
      if ((el as HTMLInputElement).type === 'hidden') continue;
      const labels = (el as HTMLInputElement).labels ? Array.from((el as HTMLInputElement).labels!) : [];
      const named = [el.getAttribute('aria-label'), el.getAttribute('aria-labelledby'),
        ...labels.map((l) => l.textContent), el.tagName === 'BUTTON' ? el.textContent : null]
        .some((v) => v !== null && v.trim() !== '');
      if (!named) missing.push(el.id || el.tagName);
    }
    return missing;
  });
}

let titles: string[];

test.beforeEach(async ({ library }) => {
  await library.reset();
  titles = [uniqueTitle('Axe A'), uniqueTitle('Axe B'), uniqueTitle('Axe C')];
  await library.create({ title: titles[0], author: 'Author A' });
  await library.create({ title: titles[1], author: 'Author B', status: 'reading' });
  const done = await library.create({ title: titles[2], author: 'Author C', status: 'completed' });
  await library.rate(done.id, 4);
});

test('US-014.1 AC-1, US-014.2: data state with three books, one completed and rated, has no serious or critical violation', async ({ page }) => {
  await page.goto('/');
  await expect(entries(page)).toHaveCount(3);
  await expect(controls(page, titles[2]).star(4)).toBeChecked();
  expect(await seriousViolations(page)).toEqual([]);
  expect(await unlabelledControls(page)).toEqual([]);
});

test('US-014.1 AC-2: add form with two field errors has no serious or critical violation', async ({ page }) => {
  await page.goto('/');
  await expect(entries(page)).toHaveCount(3);
  await form(page).add.click();
  await expect(page.locator('#title-error')).toBeVisible();
  await expect(page.locator('#author-error')).toBeVisible();
  expect(await seriousViolations(page)).toEqual([]);
  expect(await unlabelledControls(page)).toEqual([]);
});

test('US-014.1 AC-3: duplicate dialog open has no serious or critical violation', async ({ page }) => {
  await page.goto('/');
  await expect(entries(page)).toHaveCount(3);
  await form(page).title.fill(titles[0]);
  await form(page).author.fill('Author A');
  await form(page).add.click();
  await expect(duplicate(page).dialog).toBeVisible();
  expect(await seriousViolations(page)).toEqual([]);
  expect(await unlabelledControls(page)).toEqual([]);
});

test('US-014.1 AC-3: status confirmation dialog open has no serious or critical violation', async ({ page }) => {
  await page.goto('/');
  await expect(entries(page)).toHaveCount(3);
  await chooseStatus(page, titles[2], 'unread');
  const dialog = page.getByRole('dialog', { name: 'Confirm status change' });
  await expect(dialog).toBeVisible();
  expect(await seriousViolations(page)).toEqual([]);
  expect(await unlabelledControls(page)).toEqual([]);
});

test('US-014.1 AC-3: delete dialog open has no serious or critical violation', async ({ page }) => {
  await page.goto('/');
  await expect(entries(page)).toHaveCount(3);
  await controls(page, titles[0]).remove.click();
  const dialog = page.getByRole('dialog', { name: 'Delete book' });
  await expect(dialog).toBeVisible();
  expect(await seriousViolations(page)).toEqual([]);
  expect(await unlabelledControls(page)).toEqual([]);
});
