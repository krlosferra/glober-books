/**
 * Find an element by id; the page shell guarantees that it exists.
 * @param {string} id element id
 * @returns {HTMLElement} the element
 */
export function byId(id) {
  const element = document.getElementById(id);
  if (element === null) {
    throw new Error(`Missing page element: ${id}`);
  }
  return element;
}

/**
 * Replace the text of an element with one textContent write.
 * @param {HTMLElement} element the target
 * @param {string} text the text, rendered literally
 */
export function setText(element, text) {
  element.textContent = text;
}

/**
 * Show or hide an element with the hidden attribute.
 * @param {HTMLElement} element the target
 * @param {boolean} visible true to show
 */
export function setVisible(element, visible) {
  element.hidden = !visible;
}

/**
 * Announce a message in a live region (one replacement of its text).
 * @param {HTMLElement} region an element with role status
 * @param {string} text the message
 */
export function announce(region, text) {
  region.textContent = text;
}
