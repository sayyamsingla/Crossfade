// ---------------------------------------------------------------------------
// Small shared helpers: HTML escaping, relative time, and the image-with-
// fallback markup used everywhere an <img> comes from the backend.
// ---------------------------------------------------------------------------

function escapeHtml(str) {
  if (str == null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function initialsOf(name) {
  if (!name) return '?';
  return name.trim().split(/\s+/).map(w => w[0]).join('').slice(0, 2).toUpperCase();
}

const SWATCHES = ['#2fb67c', '#5b6ee1', '#e8536f', '#c98f2b', '#3fa7c9'];
function swatchColor(seed) {
  const n = typeof seed === 'number' ? seed : String(seed).split('').reduce((a, c) => a + c.charCodeAt(0), 0);
  return SWATCHES[n % SWATCHES.length];
}

/**
 * Returns HTML for an image with a graceful colored-initials fallback,
 * used for every artist/playlist/avatar image the backend provides.
 * If `url` is falsy, skips the <img> entirely and renders only the fallback
 * (avoids a flash of a broken-image icon while the backend is still stubbed out).
 */
function mediaTag({ url, name, seed, className = '', rounded = true }) {
  const resolved = window.resolveImageUrl(url);
  const initials = initialsOf(name);
  const bg = swatchColor(seed ?? name ?? Math.random());
  const shape = rounded ? 'style="border-radius:inherit"' : '';
  const fallback = `<div class="media-fallback" style="background:${bg};display:${resolved ? 'none' : 'flex'}">${escapeHtml(initials)}</div>`;
  const img = resolved
    ? `<img src="${escapeHtml(resolved)}" alt="${escapeHtml(name || '')}" class="media-img" ${shape}
         onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';" />`
    : '';
  return `<div class="media ${className}">${img}${fallback}</div>`;
}

function formatRelativeTime(isoString) {
  if (!isoString) return '';
  const then = new Date(isoString).getTime();
  if (Number.isNaN(then)) return '';
  const diffMs = Date.now() - then;
  const diffSec = Math.max(0, Math.floor(diffMs / 1000));
  const units = [
    ['y', 31536000], ['mo', 2592000], ['d', 86400], ['h', 3600], ['m', 60],
  ];
  for (const [label, secs] of units) {
    const val = Math.floor(diffSec / secs);
    if (val >= 1) return `${val}${label}`;
  }
  return 'just now';
}

window.escapeHtml = escapeHtml;
window.initialsOf = initialsOf;
window.swatchColor = swatchColor;
window.mediaTag = mediaTag;
window.formatRelativeTime = formatRelativeTime;
