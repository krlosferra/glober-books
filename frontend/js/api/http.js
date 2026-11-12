/** The only module that calls fetch. Results never throw for HTTP outcomes. */

export const TIMEOUT_MS = 15000;

const CODES = new Set([
  'VALIDATION_ERROR', 'INVALID_STATUS', 'INVALID_RATING', 'RATING_NOT_ALLOWED',
  'BOOK_NOT_FOUND', 'DUPLICATE_BOOK', 'LIBRARY_FULL', 'MALFORMED_REQUEST',
  'INVALID_HOST', 'FORBIDDEN', 'NOT_FOUND', 'METHOD_NOT_ALLOWED',
  'NOT_ACCEPTABLE', 'PAYLOAD_TOO_LARGE', 'UNSUPPORTED_MEDIA_TYPE', 'INTERNAL_ERROR',
]);

const NETWORK = Object.freeze({ ok: false, failure: Object.freeze({ kind: 'network' }) });
const UNREADABLE = Object.freeze({ ok: false, failure: Object.freeze({ kind: 'unreadable' }) });

function isObject(value) {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function parseFields(details) {
  if (!Array.isArray(details)) {
    return [];
  }
  return details
    .filter((d) => isObject(d) && typeof d.field === 'string'
      && (typeof d.message === 'string' || d.field === 'status'))
    .map((d) => ({ field: d.field, message: typeof d.message === 'string' ? d.message : '' }));
}

function apiFailure(status, body, mapBook) {
  const failure = { kind: 'api', status, code: body.code, fields: parseFields(body.details) };
  if (body.code === 'DUPLICATE_BOOK') {
    const existing = isObject(body.details) ? mapBook(body.details.existing_book) : null;
    if (existing === null) {
      return UNREADABLE;
    }
    failure.existingBook = existing;
  }
  return { ok: false, failure };
}

function interpret(response, text, parseSuccess, mapBook) {
  let body;
  try {
    body = JSON.parse(text);
  } catch {
    return UNREADABLE;
  }
  if (response.ok) {
    const data = parseSuccess(body);
    return data === null ? UNREADABLE : { ok: true, data };
  }
  if (!isObject(body) || typeof body.code !== 'string' || !CODES.has(body.code)) {
    return UNREADABLE;
  }
  return apiFailure(response.status, body, mapBook);
}

/**
 * Send one request and classify the outcome.
 * @param {object} options request options
 * @param {string} options.method HTTP method
 * @param {string} options.path root-relative path, with query if any
 * @param {object} [options.body] JSON body (non-GET only)
 * @param {function(unknown): (object|null)} options.parseSuccess maps a success body, null if the wrong shape
 * @param {function(unknown): (object|null)} options.mapBook maps a book, null if the wrong shape
 * @param {function} [options.fetchImpl] fetch implementation
 * @param {AbortSignal} [options.signal] caller signal (superseded request)
 * @param {function} [options.setTimer] setTimeout
 * @param {function} [options.clearTimer] clearTimeout
 * @returns {Promise<{ok: true, data: object}|{ok: false, failure: object}>} the result
 */
export async function requestJson(options) {
  const {
    method, path, body, parseSuccess, mapBook, signal,
    fetchImpl = globalThis.fetch.bind(globalThis),
    setTimer = setTimeout,
    clearTimer = clearTimeout,
  } = options;
  const controller = new AbortController();
  const onCallerAbort = () => controller.abort();
  if (signal) {
    if (signal.aborted) {
      return NETWORK;
    }
    signal.addEventListener('abort', onCallerAbort, { once: true });
  }
  const aborted = new Promise((resolve) => {
    controller.signal.addEventListener('abort', () => resolve(NETWORK), { once: true });
  });
  const timer = setTimer(function () { controller.abort(); }, TIMEOUT_MS);
  const headers = { Accept: 'application/json' };
  const init = { method, headers, redirect: 'error', cache: 'no-store', credentials: 'omit', signal: controller.signal };
  if (method !== 'GET') {
    headers['Content-Type'] = 'application/json';
  }
  if (body !== undefined) {
    init.body = JSON.stringify(body);
  }
  const work = (async () => {
    try {
      const response = await fetchImpl(path, init);
      const text = await response.text();
      return interpret(response, text, parseSuccess, mapBook);
    } catch {
      return NETWORK;
    }
  })();
  try {
    return await Promise.race([work, aborted]);
  } finally {
    clearTimer(timer);
    if (signal) {
      signal.removeEventListener('abort', onCallerAbort);
    }
  }
}
