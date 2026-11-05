import { randomUUID } from 'node:crypto';

/** Input of POST /api/v1/books (snake_case as in the contract). */
export interface BookInput {
  title: string;
  author: string;
  status?: 'unread' | 'reading' | 'completed';
}

/** Hostile title of US-014.6 that must be rendered as inert text. */
export const SCRIPT_TITLE = '<script>alert(1)</script>';

/**
 * Returns a title that no other test uses, so tests can share one database.
 * @param label short readable prefix
 */
export function uniqueTitle(label: string): string {
  return `${label} ${randomUUID().slice(0, 8)}`;
}

/**
 * Builds a valid book input; any field can be overridden.
 * @param overrides fields to replace
 */
export function bookInput(overrides: Partial<BookInput> = {}): BookInput {
  return { title: uniqueTitle('Book'), author: 'Test Author', ...overrides };
}
