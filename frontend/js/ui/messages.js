import { byId, announce } from './dom.js';

/**
 * The page-level "Messages" status area (US-013.4). Showing a failure is one replacement of
 * the text; clearing at the start of a new action is a separate, explicit call (CR-5).
 * @returns {{show: function(string): void, clear: function(): void}} the area
 */
export function createMessages() {
  const region = byId('messages');
  return {
    /** Replace the text once. @param {string} text the message, rendered literally */
    show(text) {
      announce(region, text);
    },
    /** Empty the area; does nothing when it is already empty. */
    clear() {
      if (region.textContent !== '') {
        announce(region, '');
      }
    },
  };
}
