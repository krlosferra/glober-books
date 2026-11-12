import { byId, setText, setVisible } from './dom.js';
import {
  ADDED_PREFIX, COMPLETED_PREFIX, LOADING_TEXT, EMPTY_LIBRARY_TEXT, EMPTY_FILTER_TEXT,
} from '../logic/copy.js';
import { statusLabel, localDate, countText, ratingText } from '../logic/format.js';
import { viewOf } from '../logic/listState.js';
import { isBookId } from '../logic/ids.js';

function fillOptional(element, text) {
  setText(element, text);
  setVisible(element, text !== '');
}

function renderEntry(template, { book }) {
  const item = template.content.firstElementChild.cloneNode(true);
  if (isBookId(book.id)) {
    item.dataset.bookId = book.id;
  }
  setText(item.querySelector('.entry-title'), book.title);
  setText(item.querySelector('.entry-author'), book.author);
  setText(item.querySelector('.entry-status'), statusLabel(book.status));
  setText(item.querySelector('.entry-added'), `${ADDED_PREFIX}${localDate(book.dateAdded)}`);
  const completed = book.dateCompleted === null ? ''
    : `${COMPLETED_PREFIX}${localDate(book.dateCompleted)}`;
  fillOptional(item.querySelector('.entry-completed'), completed);
  fillOptional(item.querySelector('.entry-rating'), ratingText(book));
  return item;
}

/** Text of the list status region for a view, and which button it offers. */
function statusFor(view, state) {
  switch (view) {
    case 'loading':
      return { text: LOADING_TEXT, button: null };
    case 'failed':
      return { text: state.failureText, button: 'try-again' };
    case 'empty':
      return { text: EMPTY_LIBRARY_TEXT, button: null };
    case 'filterEmpty':
      return { text: EMPTY_FILTER_TEXT, button: 'show-all' };
    default:
      return { text: '', button: null };
  }
}

/**
 * Create the list view: it only renders a state of logic/listState.js.
 * @returns {{render: function(object): void}} the view
 */
export function createListView() {
  const list = byId('book-list');
  const count = byId('count');
  const status = byId('list-status');
  const template = byId('entry-template');
  const buttons = { 'try-again': byId('try-again'), 'show-all': byId('show-all') };

  return {
    /**
     * Render a page state.
     * @param {object} state the state of the list model
     */
    render(state) {
      const view = viewOf(state);
      const { text, button } = statusFor(view, state);
      list.replaceChildren(...state.entries.map((entry) => renderEntry(template, entry)));
      setVisible(list, view === 'list');
      setText(count, view === 'list' || view === 'empty' || view === 'filterEmpty'
        ? countText(state.count) : '');
      setVisible(count, view !== 'loading' && view !== 'failed');
      setText(status, text);
      Object.entries(buttons).forEach(([name, element]) => setVisible(element, name === button));
    },
  };
}
