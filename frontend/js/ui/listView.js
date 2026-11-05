import { byId, setText, setVisible } from './dom.js';
import { ADDED_PREFIX, COMPLETED_PREFIX, LOADING_TEXT } from '../logic/copy.js';
import { statusLabel, localDate, countText, ratingText } from '../logic/format.js';

function fillOptional(element, text) {
  setText(element, text);
  setVisible(element, text !== '');
}

function renderEntry(template, book) {
  const item = template.content.firstElementChild.cloneNode(true);
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

/**
 * Create the list view over the page shell.
 * @returns {object} showLoading(), showBooks(books, total), prepend(book)
 */
export function createListView() {
  const list = byId('book-list');
  const count = byId('count');
  const loading = byId('loading');
  const template = byId('entry-template');
  let total = 0;

  function showCount(n) {
    total = n;
    setText(count, countText(n));
    setVisible(count, true);
  }

  return {
    /** Remove entries and count; show only the loading text. */
    showLoading() {
      list.replaceChildren();
      setVisible(list, false);
      setVisible(count, false);
      setText(loading, LOADING_TEXT);
    },
    /**
     * Show a loaded list in API order.
     * @param {object[]} books books as mapped by the API client
     * @param {number} listTotal the total of the response
     */
    showBooks(books, listTotal) {
      setText(loading, '');
      list.replaceChildren(...books.map((book) => renderEntry(template, book)));
      setVisible(list, books.length > 0);
      showCount(listTotal);
    },
    /**
     * Insert a new book at the top and update the count.
     * @param {object} book the created book
     */
    prepend(book) {
      list.prepend(renderEntry(template, book));
      setVisible(list, true);
      showCount(total + 1);
    },
  };
}
