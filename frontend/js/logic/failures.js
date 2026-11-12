/** Maps every failure to the action and exact text of Section 5.9 (US-013). */
import {
  STATUS_CHOICE_TEXT, RATING_CHOICE_TEXT, RATING_NOT_ALLOWED_TEXT, LIBRARY_FULL_TEXT,
  BOOK_GONE_TEXT, REQUEST_FAILED_TEXT, HOST_TEXT, SERVER_FAILED_TEXT, NO_RESPONSE_TEXT,
} from './copy.js';

const FIELD_ORDER = ['title', 'author', 'status'];
const REQUEST_CODES = new Set([
  'MALFORMED_REQUEST', 'UNSUPPORTED_MEDIA_TYPE', 'NOT_ACCEPTABLE',
  'METHOD_NOT_ALLOWED', 'NOT_FOUND', 'PAYLOAD_TOO_LARGE',
]);
const HOST_CODES = new Set(['INVALID_HOST', 'FORBIDDEN']);

const page = (text) => ({ action: 'page-message', text });

/** Result for the "no usable details" case and for the request-defect codes. */
function requestFailed() {
  return page(REQUEST_FAILED_TEXT);
}

function isUsable(entry) {
  if (typeof entry !== 'object' || entry === null || !FIELD_ORDER.includes(entry.field)) {
    return false;
  }
  return entry.field === 'status' || (typeof entry.message === 'string' && entry.message !== '');
}

/**
 * Pick the usable entries, one per field, in the order title, author, status.
 * Status gets the fixed text; title and author carry the server's field message as data
 * (US-011.6), to be shown with textContent only.
 */
function fieldEntries(fields) {
  if (!Array.isArray(fields)) {
    return [];
  }
  const usable = fields.filter(isUsable);
  return FIELD_ORDER.flatMap((name) => {
    const entry = usable.find((e) => e.field === name);
    if (entry === undefined) {
      return [];
    }
    return [{ field: name, text: name === 'status' ? STATUS_CHOICE_TEXT : entry.message }];
  });
}

function validation(failure) {
  const fields = fieldEntries(failure.fields);
  return fields.length === 0 ? requestFailed() : { action: 'field-errors', fields, focus: fields[0].field };
}

function byCode(code, failure) {
  switch (code) {
    case 'VALIDATION_ERROR':
      return validation(failure);
    case 'INVALID_STATUS':
      return { action: 'status-error', text: STATUS_CHOICE_TEXT, restore: true };
    case 'INVALID_RATING':
      return { action: 'rating-error', text: RATING_CHOICE_TEXT };
    case 'RATING_NOT_ALLOWED':
      return { action: 'rating-not-allowed', text: RATING_NOT_ALLOWED_TEXT, reloadEntry: true };
    case 'DUPLICATE_BOOK':
      return { action: 'duplicate-dialog', text: null };
    case 'LIBRARY_FULL':
      return page(LIBRARY_FULL_TEXT);
    case 'BOOK_NOT_FOUND':
      return { action: 'entry-gone', text: BOOK_GONE_TEXT, removeEntry: true, reloadList: true };
    default:
      break;
  }
  if (REQUEST_CODES.has(code)) {
    return requestFailed();
  }
  return page(HOST_CODES.has(code) ? HOST_TEXT : SERVER_FAILED_TEXT);
}

/**
 * Decide what the page does for a failed request. Returns only fixed texts, except the
 * `title` and `author` field messages of a usable VALIDATION_ERROR (data for textContent).
 * The server's `error` text is never read.
 * @param {unknown} failure a failure from api/http.js: {kind: 'network'|'timeout'|'unreadable'|'api', code?, fields?}
 * @returns {{action: string, text?: (string|null), fields?: object[], focus?: string}} the decision
 */
export function describeFailure(failure) {
  if (typeof failure !== 'object' || failure === null) {
    return page(SERVER_FAILED_TEXT);
  }
  if (failure.kind === 'network' || failure.kind === 'timeout') {
    return page(NO_RESPONSE_TEXT);
  }
  if (failure.kind !== 'api' || typeof failure.code !== 'string') {
    return page(SERVER_FAILED_TEXT);
  }
  return byCode(failure.code, failure);
}
