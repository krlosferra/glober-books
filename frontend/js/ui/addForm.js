import { byId, setText, setVisible } from './dom.js';
import { addBook } from '../api/booksApi.js';
import { createMessages } from './messages.js';
import { confirmDuplicate } from './dialogs.js';
import { checkAddForm } from '../logic/addCheck.js';
import { describeFailure } from '../logic/failures.js';

const FIELD_NAMES = ['title', 'author', 'status'];

/**
 * Wire the add form.
 * @param {{onAdded: function(object): void}} handlers called with the created book on 201
 */
export function initAddForm({ onAdded }) {
  const form = byId('add-form');
  const button = byId('add-button');
  const messages = createMessages();
  const inputs = Object.fromEntries(FIELD_NAMES.map((name) => [name, byId(name)]));
  let inFlight = false;

  function setBusy(busy) {
    inFlight = busy;
    button.disabled = busy;
  }

  function clearFieldErrors() {
    for (const name of FIELD_NAMES) {
      setText(byId(`${name}-error`), '');
      setVisible(byId(`${name}-error`), false);
      inputs[name].removeAttribute('aria-describedby');
      inputs[name].removeAttribute('aria-invalid');
    }
  }

  function showFieldError(name, text) {
    const slot = byId(`${name}-error`);
    setText(slot, text);
    setVisible(slot, true);
    inputs[name].setAttribute('aria-describedby', slot.id);
    inputs[name].setAttribute('aria-invalid', 'true');
  }

  function showFields(entries) {
    for (const entry of entries) {
      showFieldError(entry.field, entry.text);
    }
    inputs[entries[0].field].focus();
  }

  /** Show the outcome of a failed request; keeps every typed value. */
  function handleFailure(failure) {
    const decision = describeFailure(failure);
    if (decision.action === 'field-errors') {
      showFields(decision.fields);
    } else if (decision.action === 'status-error') {
      showFields([{ field: 'status', text: decision.text }]);
    } else {
      if (typeof decision.text === 'string') {
        messages.show(decision.text);
      }
      inputs.title.focus();
    }
  }

  /** Send the add request; `allowDuplicate` is true only after "Add anyway" (CR-1). */
  async function send(allowDuplicate) {
    setBusy(true);
    const result = await addBook({
      title: inputs.title.value, author: inputs.author.value, status: inputs.status.value,
    }, allowDuplicate);
    if (result.ok) {
      setBusy(false);
      form.reset();
      onAdded(result.data);
      inputs.title.focus();
    } else if (describeFailure(result.failure).action === 'duplicate-dialog') {
      await askDuplicate(result.failure.existingBook);
    } else {
      setBusy(false);
      handleFailure(result.failure);
    }
  }

  /** The button stays disabled while the modal dialog is open (US-011.8). */
  async function askDuplicate(existing) {
    const confirmed = await confirmDuplicate(existing);
    if (confirmed) {
      await send(true);
    } else {
      setBusy(false);
      inputs.title.focus();
    }
  }

  async function submit(event) {
    event.preventDefault();
    if (inFlight) {
      return;
    }
    messages.clear();
    clearFieldErrors();
    const problems = checkAddForm({ title: inputs.title.value, author: inputs.author.value });
    if (problems.length > 0) {
      showFields(problems);
      return;
    }
    await send(false);
  }

  form.addEventListener('submit', submit);
}
