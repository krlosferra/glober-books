import { requestJson } from './http.js';
import { bookPath } from '../logic/ids.js';

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

function bookRequest(id, method, suffix, body, deps) {
  const path = bookPath(id) + suffix;
  return requestJson({
    ...deps, method, path, body, parseSuccess: toBook, mapBook: toBook,
  });
}

/**
 * Get one book. An invalid id rejects with a TypeError before any request.
 * @param {string} id lower-case UUID
 * @param {object} [deps] fetchImpl, signal, setTimer, clearTimer
 * @returns {Promise<object>} result with data the book
 */
export async function getBook(id, deps = {}) {
  return bookRequest(id, 'GET', '', undefined, deps);
}

/**
 * Change the status. Invalid id or status rejects with a TypeError before any request.
 * @param {string} id lower-case UUID
 * @param {string} status unread, reading or completed
 * @param {object} [deps] fetchImpl, signal, setTimer, clearTimer
 * @returns {Promise<object>} result with data the book after the change
 */
export async function setStatus(id, status, deps = {}) {
  if (!STATUSES.has(status)) {
    throw new TypeError('invalid status');
  }
  return bookRequest(id, 'PUT', '/status', { status }, deps);
}

/**
 * Set or replace the rating. Invalid id or rating rejects with a TypeError before any request.
 * @param {string} id lower-case UUID
 * @param {number} rating integer 1 to 5
 * @param {object} [deps] fetchImpl, signal, setTimer, clearTimer
 * @returns {Promise<object>} result with data the book
 */
export async function setRating(id, rating, deps = {}) {
  if (!Number.isInteger(rating) || rating < 1 || rating > 5) {
    throw new TypeError('invalid rating');
  }
  return bookRequest(id, 'PUT', '/rating', { rating }, deps);
}

/**
 * Remove the rating (DELETE, no body). Invalid id rejects with a TypeError before any request.
 * @param {string} id lower-case UUID
 * @param {object} [deps] fetchImpl, signal, setTimer, clearTimer
 * @returns {Promise<object>} result with data the book with a null rating
 */
export async function clearRating(id, deps = {}) {
  return bookRequest(id, 'DELETE', '/rating', undefined, deps);
}

/**
 * Delete a book (DELETE, no body). Invalid id rejects with a TypeError before any request.
 * @param {string} id lower-case UUID
 * @param {object} [deps] fetchImpl, signal, setTimer, clearTimer
 * @returns {Promise<object>} result with data the deleted book
 */
export async function deleteBook(id, deps = {}) {
  return bookRequest(id, 'DELETE', '', undefined, deps);
}
