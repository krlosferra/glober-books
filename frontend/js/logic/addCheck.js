/** Client-side check of the add form (UX only; the server stays the authority, US-011.3). */
import { checkTitle, checkAuthor } from './text.js';
import {
  TITLE_EMPTY_TEXT, TITLE_LENGTH_TEXT, AUTHOR_EMPTY_TEXT, AUTHOR_LENGTH_TEXT,
} from './copy.js';

function problem(field, check, emptyText, lengthText) {
  if (check.status === 'ok') {
    return [];
  }
  return [{ field, text: check.status === 'empty' ? emptyText : lengthText }];
}

/**
 * Check the typed title and author.
 * @param {{title: unknown, author: unknown}} values typed values
 * @returns {{field: 'title'|'author', text: string}[]} one entry per field in error, title first; empty when both are fine
 */
export function checkAddForm(values) {
  return [
    ...problem('title', checkTitle(values.title), TITLE_EMPTY_TEXT, TITLE_LENGTH_TEXT),
    ...problem('author', checkAuthor(values.author), AUTHOR_EMPTY_TEXT, AUTHOR_LENGTH_TEXT),
  ];
}
