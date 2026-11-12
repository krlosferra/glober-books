import { byId, announce } from './dom.js';
import { createListView } from './listView.js';
import { initAddForm } from './addForm.js';
import { initEntryActions } from './entryActions.js';
import { listBooks } from '../api/booksApi.js';
import { BOOK_ADDED_PREFIX } from '../logic/copy.js';
import {
  initialState, loadStarted, loadSucceeded, loadFailed, bookAdded,
} from '../logic/listState.js';

function checkedFilter() {
  const checked = document.querySelector('input[name="filter"]:checked');
  return checked === null ? 'all' : checked.value;
}

/** Start the page: load the list and wire the filter, the retry and the add form. */
export async function startApp() {
  const view = createListView();
  const messages = byId('messages');
  let state = initialState();

  let uiOf;

  function update(next) {
    state = next;
    view.render(state, uiOf);
  }

  async function load(filter) {
    update(loadStarted(state, filter));
    const seq = state.latestRequest;
    const result = await listBooks(state.filter);
    update(result.ok
      ? loadSucceeded(state, seq, result.data)
      : loadFailed(state, seq, result.failure));
  }

  ({ uiOf } = initEntryActions({
    getState: () => state,
    setState(next) {
      state = next;
    },
    update,
    announce: (text) => announce(messages, text),
    reload: () => load(state.filter),
  }));

  initAddForm({
    onAdded(book) {
      announce(messages, `${BOOK_ADDED_PREFIX}${book.title}`);
      update(bookAdded(state, book));
    },
  });

  byId('filter').addEventListener('change', () => load(checkedFilter()));
  byId('try-again').addEventListener('click', () => {
    byId('books-heading').focus();
    load(state.filter);
  });
  byId('show-all').addEventListener('click', () => {
    document.querySelector('input[name="filter"][value="all"]').checked = true;
    byId('books-heading').focus();
    load('all');
  });

  await load('all');
}
