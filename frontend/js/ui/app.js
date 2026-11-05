import { byId, announce, setText } from './dom.js';
import { createListView } from './listView.js';
import { initAddForm } from './addForm.js';
import { listBooks } from '../api/booksApi.js';
import { BOOK_ADDED_PREFIX } from '../logic/copy.js';

/** Start the page: load the list and wire the add form. */
export async function startApp() {
  const view = createListView();
  const messages = byId('messages');
  let loaded = false;

  initAddForm({
    onAdded(book) {
      announce(messages, `${BOOK_ADDED_PREFIX}${book.title}`);
      if (loaded) {
        view.prepend(book);
      }
    },
  });

  view.showLoading();
  const result = await listBooks('all');
  if (result.ok) {
    loaded = true;
    view.showBooks(result.data.books, result.data.total);
  } else {
    setText(byId('loading'), '');
  }
}
