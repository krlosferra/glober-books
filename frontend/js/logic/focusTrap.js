/**
 * Decide where Tab or Shift+Tab sends focus inside a two-button dialog (US-014.2, CR-6).
 * Focus never leaves the two buttons: from the edge button it wraps, from anywhere else
 * (for example non-interactive text) Shift+Tab goes to the last button and Tab to the first.
 * @param {boolean} shift true for Shift+Tab
 * @param {'first'|'last'|'other'} current which element has focus
 * @returns {'first'|'last'|null} the button to focus (caller calls preventDefault), or null to keep the browser default
 */
export function nextTabTarget(shift, current) {
  if (shift) {
    return current === 'last' ? null : 'last';
  }
  return current === 'first' ? null : 'first';
}
