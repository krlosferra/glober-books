import { byId, setText } from './dom.js';
import { ADDED_PREFIX } from '../logic/copy.js';
import { statusLabel, localDate } from '../logic/format.js';

const CONFIRMED = 'confirm';

function isText(value) {
  return typeof value === 'string';
}

/** Keep Tab and Shift+Tab inside the dialog (US-014.2, CR-6): wrap between the two buttons. */
function trapTab(event, first, last) {
  if (event.key !== 'Tab') {
    return;
  }
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}

/**
 * Open the modal "Possible duplicate" dialog (US-011.7) and wait for the user's choice.
 * The existing book is shown with textContent only. Escape and Cancel both resolve false;
 * the native modal dialog has no other way to close (no outside click, no hidden path).
 * @param {{title: string, author: string, status: string, dateAdded: string}} existing a book mapped by the API layer
 * @returns {Promise<boolean>} true for "Add anyway", false for Cancel or Escape
 */
export function confirmDuplicate(existing) {
  const dialog = byId('duplicate-dialog');
  const cancel = byId('duplicate-cancel');
  const confirm = byId('duplicate-confirm');
  const shown = isText(existing.title) && isText(existing.author) && isText(existing.dateAdded);
  setText(byId('duplicate-title'), shown ? existing.title : '');
  setText(byId('duplicate-author'), shown ? existing.author : '');
  setText(byId('duplicate-status'), shown ? statusLabel(existing.status) : '');
  setText(byId('duplicate-added'), shown ? `${ADDED_PREFIX}${localDate(existing.dateAdded)}` : '');
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
