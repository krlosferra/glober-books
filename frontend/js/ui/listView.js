import { byId, setText, setVisible } from './dom.js';
import { LOADING_TEXT, EMPTY_LIBRARY_TEXT, EMPTY_FILTER_TEXT } from '../logic/copy.js';
import { countText } from '../logic/format.js';
import { viewOf } from '../logic/listState.js';
import { renderEntry, IDLE } from './entryView.js';

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
     * @param {function(string): {busy: boolean, message: string}} [uiOf] per-entry view state by book id
     */
    render(state, uiOf = () => IDLE) {
      const view = viewOf(state);
      const { text, button } = statusFor(view, state);
      list.replaceChildren(...state.entries.map((entry) => renderEntry(template, entry, uiOf(entry.book.id))));
      setVisible(list, view === 'list');
      setText(count, view === 'list' || view === 'empty' || view === 'filterEmpty'
        ? countText(state.count) : '');
      setVisible(count, view !== 'loading' && view !== 'failed');
      setText(status, text);
      Object.entries(buttons).forEach(([name, element]) => setVisible(element, name === button));
    },
  };
}
