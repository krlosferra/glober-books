import { requestJson } from './http.js';

const BOOKS_PATH = '/api/v1/books';
const FILTERS = new Set(['unread', 'reading', 'completed']);
const STATUSES = FILTERS;

function isString(value) {
  return typeof value === 'string';
}

/**
 * Map a book from the wire (snake_case) to the page shape (camelCase).
 * @param {unknown} raw a book representation
 * @returns {object|null} the book, or null when it lacks one of the seven members or has a wrong type
 */
export function toBook(raw) {
  if (typeof raw !== 'object' || raw === null) {
    return null;
  }
  const ratingOk = raw.rating === null || Number.isInteger(raw.rating);
  const completedOk = raw.date_completed === null || isString(raw.date_completed);
  if (!isString(raw.id) || !isString(raw.title) || !isString(raw.author)
    || !STATUSES.has(raw.status) || !ratingOk || !isString(raw.date_added) || !completedOk) {
    return null;
  }
  return {
    id: raw.id,
    title: raw.title,
    author: raw.author,
    status: raw.status,
    rating: raw.rating,
    dateAdded: raw.date_added,
    dateCompleted: raw.date_completed,
  };
}

function parseList(body) {
  if (typeof body !== 'object' || body === null || !Array.isArray(body.books)
    || !Number.isInteger(body.total)) {
    return null;
  }
  const books = body.books.map(toBook);
  return books.includes(null) ? null : { books, total: body.total };
}

/**
 * List books, optionally filtered by status.
 * @param {string} filter all, unread, reading or completed (anything else sends no query)
 * @param {object} [deps] fetchImpl, signal, setTimer, clearTimer
 * @returns {Promise<object>} result with data {books, total}
 */
export function listBooks(filter, deps = {}) {
  const query = FILTERS.has(filter) ? `?status=${filter}` : '';
  return requestJson({
    ...deps, method: 'GET', path: BOOKS_PATH + query, parseSuccess: parseList, mapBook: toBook,
  });
}

/**
 * Add a book.
 * @param {{title: string, author: string, status: string}} input the book fields
 * @param {boolean} [allowDuplicate] sent as allow_duplicate only when true
 * @param {object} [deps] fetchImpl, signal, setTimer, clearTimer
 * @returns {Promise<object>} result with data the created book
 */
export function addBook(input, allowDuplicate = false, deps = {}) {
  const body = { title: input.title, author: input.author, status: input.status };
  if (allowDuplicate === true) {
    body.allow_duplicate = true;
  }
  return requestJson({
    ...deps, method: 'POST', path: BOOKS_PATH, body, parseSuccess: toBook, mapBook: toBook,
  });
}
