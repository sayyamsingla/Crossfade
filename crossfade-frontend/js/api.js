// ---------------------------------------------------------------------------
// Crossfade API client — every call the frontend makes to the backend.
// Full endpoint contract is documented in README.md — build the Spring Boot
// controllers to match these paths, methods, and response shapes exactly
// and the frontend will work with no changes.
// ---------------------------------------------------------------------------

const { API_PREFIX, API_BASE } = window.CROSSFADE_CONFIG;

class ApiError extends Error {
  constructor(message, cause) {
    super(message);
    this.name = 'ApiError';
    this.cause = cause;
  }
}

async function apiFetch(path, options = {}) {
  const url = `${API_PREFIX}${path}`;
  let res;
  try {
    res = await fetch(url, {
      headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
      ...options,
    });
  } catch (err) {
    // Network-level failure — backend down, wrong port, or CORS rejected the request.
    throw new ApiError(
      `Could not reach ${url}. Is the backend running on ${API_BASE}? (CORS must also allow http://localhost:5173 — see README.)`,
      err
    );
  }
  if (!res.ok) {
    let detail = '';
    try { detail = (await res.json()).message || ''; } catch (_) { /* no body */ }
    throw new ApiError(`${options.method || 'GET'} ${path} failed: ${res.status} ${res.statusText}${detail ? ' — ' + detail : ''}`);
  }
  if (res.status === 204) return null;
  return res.json();
}

/** Resolve an image URL returned by the backend. Accepts absolute URLs
 *  ("http://...") as-is, and treats anything else as a path relative to
 *  API_BASE (e.g. "/images/artists/12.jpg"). Returns null if no url given. */
function resolveImageUrl(url) {
  if (!url) return null;
  if (/^https?:\/\//i.test(url)) return url;
  return `${API_BASE}${url.startsWith('/') ? '' : '/'}${url}`;
}

// TEMP: while USE_MOCK_DATA is on (see js/config.js), every Api method below
// delegates to js/mock-data.js instead of hitting the network. Delete both
// files (and this check) once the real backend is ready.
const RealApi = {
  // ---- profile -------------------------------------------------------
  getProfile(userId) {
    return apiFetch(`/users/${userId}`);
  },
  getNowPlaying(userId) {
    return apiFetch(`/users/${userId}/now-playing`);
  },
  getTopTracks(userId, { range = '4weeks', limit = 5 } = {}) {
    return apiFetch(`/users/${userId}/top-tracks?range=${range}&limit=${limit}`);
  },
  getTopArtists(userId, { range = '4weeks', limit = 6 } = {}) {
    return apiFetch(`/users/${userId}/top-artists?range=${range}&limit=${limit}`);
  },
  getTopGenres(userId, { limit = 5 } = {}) {
    return apiFetch(`/users/${userId}/top-genres?limit=${limit}`);
  },
  getPlaylists(userId) {
    return apiFetch(`/users/${userId}/playlists`);
  },
  getPlaylistTracks(playlistId) {
    return apiFetch(`/playlists/${playlistId}/tracks`);
  },
  likePlaylist(playlistId) {
    return apiFetch(`/playlists/${playlistId}/like`, { method: 'POST' });
  },
  unlikePlaylist(playlistId) {
    return apiFetch(`/playlists/${playlistId}/like`, { method: 'DELETE' });
  },

  // ---- comments (profile wall) ---------------------------------------
  getComments(userId) {
    return apiFetch(`/users/${userId}/comments`);
  },
  postComment(userId, authorId, text) {
    return apiFetch(`/users/${userId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ authorId, text }),
    });
  },
  likeComment(commentId) {
    return apiFetch(`/comments/${commentId}/like`, { method: 'POST' });
  },
  unlikeComment(commentId) {
    return apiFetch(`/comments/${commentId}/like`, { method: 'DELETE' });
  },

  // ---- feed ------------------------------------------------------------
  getFeed(userId) {
    return apiFetch(`/users/${userId}/feed`);
  },

  // ---- people / friends / search -----------------------------------------
  /** People this user follows — powers the Friends tab and the Compatibility picker. */
  getFollowing(userId) {
    return apiFetch(`/users/${userId}/following`);
  },
  /** Find any user by (partial) display name — powers the Friends-tab search box. */
  searchUsers(query) {
    return apiFetch(`/users/search?q=${encodeURIComponent(query)}`);
  },
  followUser(userId) {
    return apiFetch(`/users/${userId}/follow`, { method: 'POST' });
  },
  unfollowUser(userId) {
    return apiFetch(`/users/${userId}/follow`, { method: 'DELETE' });
  },
  getCompatibility(userId, withUserId) {
    return apiFetch(`/compatibility?userId=${userId}&withUserId=${withUserId}`);
  },
};

const Api = window.CROSSFADE_CONFIG.USE_MOCK_DATA ? window.MockApi : RealApi;

window.Api = Api;
window.ApiError = ApiError;
window.resolveImageUrl = resolveImageUrl;
