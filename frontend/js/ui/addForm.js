import { byId } from './dom.js';
import { addBook } from '../api/booksApi.js';

/**
 * Wire the add form.
 * @param {{onAdded: function(object): void}} handlers called with the created book on 201
 */
export function initAddForm({ onAdded }) {
  const form = byId('add-form');
  const title = byId('title');
  const author = byId('author');
  const status = byId('status');
  const button = byId('add-button');
  let inFlight = false;

  function setBusy(busy) {
    inFlight = busy;
    button.disabled = busy;
  }

  async function submit(event) {
    event.preventDefault();
    if (inFlight) {
      return;
    }
    setBusy(true);
    const result = await addBook({
      title: title.value, author: author.value, status: status.value,
    });
    if (result.ok) {
      form.reset();
      onAdded(result.data);
    }
    setBusy(false);
    title.focus();
  }

  form.addEventListener('submit', submit);
}
