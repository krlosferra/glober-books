import { byId, setText } from './dom.js';
import { ADDED_PREFIX, confirmStatusText, deleteConfirmText } from '../logic/copy.js';
import { nextTabTarget } from '../logic/focusTrap.js';
import { statusLabel, localDate } from '../logic/format.js';

const CONFIRMED = 'confirm';

function isText(value) {
  return typeof value === 'string';
}

/** Keep Tab and Shift+Tab on the two buttons (US-014.2, CR-6), also when focus is on neither. */
function trapTab(event, first, last) {
  if (event.key !== 'Tab') {
    return;
  }
  const active = document.activeElement;
  const current = active === first ? 'first' : active === last ? 'last' : 'other';
  const target = nextTabTarget(event.shiftKey, current);
  if (target !== null) {
    event.preventDefault();
    (target === 'first' ? first : last).focus();
  }
}

/**
 * Show a modal dialog with two buttons and wait for the choice. Escape and Cancel both
 * resolve false; the native modal dialog has no other way to close.
 */
function runDialog(dialog, cancel, confirm) {
  dialog.returnValue = '';
  return new Promise((resolve) => {
    const onConfirm = () => dialog.close(CONFIRMED);
    const onCancel = () => dialog.close('cancel');
    const onKey = (event) => trapTab(event, cancel, confirm);
    dialog.addEventListener('close', () => {
      confirm.removeEventListener('click', onConfirm);
      cancel.removeEventListener('click', onCancel);
      dialog.removeEventListener('keydown', onKey);
      resolve(dialog.returnValue === CONFIRMED);
    }, { once: true });
    confirm.addEventListener('click', onConfirm);
    cancel.addEventListener('click', onCancel);
    dialog.addEventListener('keydown', onKey);
    dialog.showModal();
    cancel.focus();
  });
}

/**
 * Open the modal "Possible duplicate" dialog (US-011.7) and wait for the user's choice.
 * The existing book is shown with textContent only.
 * @param {{title: string, author: string, status: string, dateAdded: string}} existing a book mapped by the API layer
 * @returns {Promise<boolean>} true for "Add anyway", false for Cancel or Escape
 */
export function confirmDuplicate(existing) {
  const shown = isText(existing.title) && isText(existing.author) && isText(existing.dateAdded);
  setText(byId('duplicate-title'), shown ? existing.title : '');
  setText(byId('duplicate-author'), shown ? existing.author : '');
  setText(byId('duplicate-status'), shown ? statusLabel(existing.status) : '');
  setText(byId('duplicate-added'), shown ? `${ADDED_PREFIX}${localDate(existing.dateAdded)}` : '');
  return runDialog(byId('duplicate-dialog'), byId('duplicate-cancel'), byId('duplicate-confirm'));
}

/**
 * Open the modal "Confirm status change" dialog (US-012.3) and wait for the choice.
 * The title is shown with textContent only; initial focus is on Cancel.
 * @param {string} title the book title
 * @param {string} targetLabel Unread or Reading
 * @returns {Promise<boolean>} true for "Continue", false for Cancel or Escape
 */
export function confirmStatusChange(title, targetLabel) {
  setText(byId('status-text'), confirmStatusText(title, targetLabel));
  return runDialog(byId('status-dialog'), byId('status-cancel'), byId('status-confirm'));
}

/**
 * Open the modal "Delete book" dialog (US-012.5) and wait for the choice.
 * The title is shown with textContent only; initial focus is on Cancel.
 * @param {string} title the book title
 * @returns {Promise<boolean>} true for "Delete", false for Cancel or Escape
 */
export function confirmDelete(title) {
  setText(byId('delete-text'), deleteConfirmText(title));
  return runDialog(byId('delete-dialog'), byId('delete-cancel'), byId('delete-confirm'));
}
