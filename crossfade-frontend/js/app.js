// ---------------------------------------------------------------------------
// Crossfade frontend — rendering + wiring. All data comes from the backend
// via js/api.js. See README.md for the full endpoint contract.
// ---------------------------------------------------------------------------

let CURRENT_USER_ID = window.CROSSFADE_CONFIG.CURRENT_USER_ID;

let currentProfile = null;      // your own profile (CURRENT_USER_ID) — cached once, used for compat "you" avatar
let viewedUserId = CURRENT_USER_ID; // whose profile the Profile tab is currently showing
let peopleCache = {};           // userId -> { userId, displayName, avatarUrl, topGenre } — filled in from every source (profile loads, friends list, search)
let friendsListCache = null;    // array — people you follow, used by both the Friends tab and the Compatibility picker
let feedLoaded = false;

// ---------------------------------------------------------------------- init
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('loginBtn').href = `${window.CROSSFADE_CONFIG.API_BASE}/oauth2/authorization/spotify`;

  if (new URLSearchParams(window.location.search).get('loginError')) {
    document.getElementById('loginErrorNote').style.display = 'block';
    window.history.replaceState({}, '', window.location.pathname);
  }
  document.getElementById('logoutBtn').addEventListener('click', async () => {
    try { await Api.logout(); } catch (_) { /* best-effort */ }
    window.location.reload();
  });

  initTabs();
  document.getElementById('retryBtn').addEventListener('click', () => loadProfilePage());
  document.getElementById('commentForm').addEventListener('submit', handleCommentSubmit);
  document.getElementById('searchInput').addEventListener('input', handleSearchInput);
  const commentInput = document.getElementById('commentInput');
  commentInput.addEventListener('input', () => {
    commentInput.style.height = 'auto';
    commentInput.style.height = commentInput.scrollHeight + 'px';
  });
  document.getElementById('backToMeBtn').addEventListener('click', () => {
    viewedUserId = CURRENT_USER_ID;
    loadProfilePage();
  });
  document.getElementById('compareBtn').addEventListener('click', () => {
    const person = peopleCache[viewedUserId];
    goToCompatWith(viewedUserId, person ? person.displayName : 'them');
  });
  document.getElementById('playlistBackBtn').addEventListener('click', closePlaylistPage);
  document.getElementById('syncBtn').addEventListener('click', handleSyncClick);
  document.getElementById('chooseHandleForm').addEventListener('submit', handleChooseHandleSubmit);

  initSession();
});

/** Resolves who's logged in via the session cookie before loading anything
 *  else. Shows the "log in with Spotify" page on failure (no session, or
 *  session expired) instead of trying to render a profile for nobody. New
 *  accounts (and any pre-existing account that hasn't picked a real handle
 *  yet) are routed to the handle picker instead of straight into the app. */
async function initSession() {
  try {
    const me = await Api.getMe();
    CURRENT_USER_ID = me.id;
    window.CROSSFADE_CONFIG.CURRENT_USER_ID = me.id;
    viewedUserId = me.id;
    document.getElementById('logoutBtn').style.display = 'inline-block';
    if (!me.hasChosenHandle) {
      activateTab('choose-handle');
      return;
    }
    activateTab('profile');
    loadProfilePage();
  } catch (err) {
    activateTab('loggedout');
  }
}

async function handleChooseHandleSubmit(e) {
  e.preventDefault();
  const input = document.getElementById('handleInput');
  const errorEl = document.getElementById('handleError');
  const btn = document.getElementById('handleSubmitBtn');
  errorEl.style.display = 'none';
  btn.disabled = true;
  try {
    await Api.setHandle(input.value.trim());
    activateTab('profile');
    loadProfilePage();
  } catch (err) {
    errorEl.textContent = err.message || 'Could not save that handle.';
    errorEl.style.display = 'block';
    btn.disabled = false;
  }
}

// ------------------------------------------------------------------ sync
/** Formats a future ISO instant as a short "in Nd"/"in Nh" style string. */
function formatTimeUntil(isoString) {
  const target = new Date(isoString).getTime();
  if (Number.isNaN(target)) return '';
  const diffSec = Math.max(0, Math.floor((target - Date.now()) / 1000));
  const units = [
    ['d', 86400], ['h', 3600], ['m', 60],
  ];
  for (const [label, secs] of units) {
    const val = Math.floor(diffSec / secs);
    if (val >= 1) return `in ${val}${label}`;
  }
  return 'soon';
}

async function refreshSyncStatus() {
  const btn = document.getElementById('syncBtn');
  try {
    const status = await Api.getSyncStatus();
    const remaining = status.syncsAllowed - status.syncsUsed;
    if (remaining > 0) {
      btn.textContent = 'Sync now';
      btn.disabled = false;
      btn.title = `Pulls your latest top tracks, top artists, and playlists from Spotify. You can sync ${status.syncsAllowed} times every 7 days — ${remaining} left this week.`;
    } else {
      btn.textContent = 'Sync now';
      btn.disabled = true;
      btn.title = `You have used all ${status.syncsAllowed} syncs for this 7 day period. The oldest one drops off ${formatTimeUntil(status.nextAvailableAt)}, freeing up another sync.`;
    }
  } catch (err) {
    console.error(err);
  }
}

async function handleSyncClick() {
  const btn = document.getElementById('syncBtn');
  btn.disabled = true;
  const previousText = btn.textContent;
  btn.textContent = 'Syncing…';
  try {
    await Api.triggerSync();
    if (viewedUserId === CURRENT_USER_ID) loadProfilePage();
    await refreshSyncStatus();
  } catch (err) {
    if (err.message && err.message.includes('429')) {
      await refreshSyncStatus();
    } else {
      btn.textContent = previousText;
      btn.disabled = false;
      showBanner(err.message || 'Sync failed.');
    }
  }
}

function initTabs() {
  document.querySelectorAll('.nav-link').forEach(btn => {
    btn.addEventListener('click', () => {
      const tab = btn.dataset.tab;
      activateTab(tab);
      // Real nav clicks decide what to (re)load; "Profile" always means "my profile" here —
      // viewProfile() below switches to the same tab without going through this reset.
      if (tab === 'profile') { viewedUserId = CURRENT_USER_ID; loadProfilePage(); }
      if (tab === 'feed') loadFeedPage();
      if (tab === 'friends') loadFriendsPage();
      if (tab === 'compat') loadCompatPage();
    });
  });
}

/** Pure UI switch — which nav item is highlighted and which page is visible.
 *  Deliberately has no side effects on data, so callers control what loads. */
function activateTab(tab) {
  document.querySelectorAll('.nav-link').forEach(b => b.classList.toggle('active', b.dataset.tab === tab));
  document.querySelectorAll('.page').forEach(p => p.classList.toggle('active', p.id === 'page-' + tab));
  document.querySelector('.app-shell').classList.toggle('signed-out', tab === 'loggedout' || tab === 'choose-handle');
}

/** Jump to someone's profile from anywhere (friend card, search result,
 *  comment author, feed actor) and switch to the Profile tab to show it. */
function viewProfile(userId) {
  viewedUserId = Number(userId);
  activateTab('profile');
  loadProfilePage();
}
window.viewProfile = viewProfile;

// -------------------------------------------------------------- error banner
function showBanner(message) {
  const el = document.getElementById('errorBanner');
  el.querySelector('.banner-text').textContent = message;
  el.style.display = 'flex';
}
function hideBanner() {
  document.getElementById('errorBanner').style.display = 'none';
}

/** Runs `fn`, and on failure renders an inline error inside `containerId`
 *  instead of throwing — keeps one broken endpoint from taking down the page. */
async function loadSection(fn, containerId) {
  try {
    await fn();
  } catch (err) {
    const el = document.getElementById(containerId);
    if (el) el.innerHTML = `<p class="error-inline">${escapeHtml(err.message)}</p>`;
    console.error(err);
  }
}

// -------------------------------------------------------------------- profile
async function loadProfilePage() {
  const userId = viewedUserId;
  try {
    const profile = await Api.getProfile(userId);
    if (userId === CURRENT_USER_ID) currentProfile = profile;
    peopleCache[userId] = { userId, displayName: profile.displayName, avatarUrl: profile.avatarUrl, topGenre: profile.topGenre };
    renderHero(profile);
    hideBanner();
  } catch (err) {
    showBanner(err.message);
    return; // backend unreachable — no point calling the rest
  }

  loadSection(() => Api.getNowPlaying(userId).then(renderNowPlaying), 'nowPlayingStrip');
  loadSection(() => Api.getTopTracks(userId).then(renderTracks), 'tracksList');
  loadSection(() => Api.getTopArtists(userId).then(renderArtists), 'artistGrid');
  // top genres hidden from profile, backend endpoint still in use elsewhere
  // loadSection(() => Api.getTopGenres(userId).then(renderGenres), 'genreList');
  loadSection(() => Api.getPlaylists(userId).then(renderPlaylists), 'playlistGrid');
  loadSection(() => Api.getComments(userId).then(renderComments), 'commentList');

  if (currentProfile) {
    document.getElementById('commentFormAvatar').innerHTML = mediaTag({ url: currentProfile.avatarUrl, name: currentProfile.displayName, seed: currentProfile.id });
  }
}

function renderHero(p) {
  const isSelf = viewedUserId === CURRENT_USER_ID;
  document.getElementById('heroAvatarWrap').innerHTML = mediaTag({ url: p.avatarUrl, name: p.displayName, seed: p.id });
  document.getElementById('idChip').textContent = initialsOf(currentProfile ? currentProfile.displayName : p.displayName);
  document.getElementById('heroName').textContent = p.displayName;
  document.getElementById('heroHandle').textContent = p.handle || '';
  document.getElementById('heroFollowers').textContent = (p.followersCount ?? 0).toLocaleString();
  document.getElementById('heroFollowing').textContent = (p.followingCount ?? 0).toLocaleString();
  // top genre stat hidden from profile, backend field still in use elsewhere
  // document.getElementById('heroTopGenre').textContent = p.topGenre || '—';
  const bioEl = document.getElementById('heroBio');
  if (p.bio) { bioEl.textContent = p.bio; bioEl.style.display = 'block'; } else { bioEl.style.display = 'none'; }

  const banner = document.getElementById('viewingBanner');
  const compareBtn = document.getElementById('compareBtn');
  const followBtn = document.getElementById('heroFollowBtn');
  const syncBtn = document.getElementById('syncBtn');
  banner.style.display = isSelf ? 'none' : 'flex';
  if (!isSelf) document.getElementById('viewingText').textContent = `Viewing ${p.displayName}'s profile`;
  compareBtn.style.display = isSelf ? 'none' : 'inline-block';
  syncBtn.style.display = isSelf ? 'inline-block' : 'none';
  if (isSelf) refreshSyncStatus();

  followBtn.style.display = isSelf ? 'none' : 'inline-block';
  if (!isSelf) {
    followBtn.disabled = false;
    followBtn.onclick = async () => {
      await toggleFollow(p.id, followBtn);
      Api.getProfile(p.id).then(fresh => {
        document.getElementById('heroFollowers').textContent = (fresh.followersCount ?? 0).toLocaleString();
      });
    };
    getFriendsList().then(friends => {
      const isFollowing = friends.some(u => u.userId === p.id);
      followBtn.classList.toggle('following', isFollowing);
      followBtn.textContent = isFollowing ? 'Following' : 'Follow';
    });
  }
}

function renderNowPlaying(data) {
  const el = document.getElementById('nowPlayingStrip');
  if (!data || !data.playing || !data.track) { el.style.display = 'none'; return; }
  el.style.display = 'flex';
  el.innerHTML = `
    <svg class="np-note" viewBox="0 0 24 24"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>
    <div class="np-body">
      <div class="np-label">Now playing</div>
      <div class="np-title">${escapeHtml(data.track.title)}</div>
      <div class="np-artist">${escapeHtml(data.track.artist)}</div>
    </div>
  `;
}

const TRACKS_COLLAPSED_COUNT = 5;

function trackRowHtml(t) {
  return `
    <div class="track-row">
      <div class="track-rank mono">${t.rank}</div>
      <div class="track-cover">${mediaTag({ url: t.coverUrl, name: t.title, seed: t.trackId })}</div>
      <div style="flex:1;min-width:0;">
        <div class="track-name">${escapeHtml(t.title)}</div>
        <div class="track-artist">${escapeHtml(t.artist)}</div>
      </div>
    </div>
  `;
}

function renderTracks(tracks) {
  const el = document.getElementById('tracksList');
  if (!tracks || !tracks.length) { el.innerHTML = '<p class="empty-note">No top tracks yet.</p>'; return; }

  const visible = tracks.slice(0, TRACKS_COLLAPSED_COUNT).map(trackRowHtml).join('');
  const rest = tracks.slice(TRACKS_COLLAPSED_COUNT).map(trackRowHtml).join('');

  if (!rest) { el.innerHTML = visible; return; }

  el.innerHTML = `
    <div id="tracksVisible">${visible}</div>
    <div id="tracksExtra" style="display:none;">${rest}</div>
    <button type="button" id="tracksToggleBtn" class="tracks-toggle-btn">Show ${tracks.length - TRACKS_COLLAPSED_COUNT} more</button>
  `;

  const extra = document.getElementById('tracksExtra');
  const toggleBtn = document.getElementById('tracksToggleBtn');
  toggleBtn.addEventListener('click', () => {
    const expanded = extra.style.display !== 'none';
    extra.style.display = expanded ? 'none' : 'block';
    toggleBtn.textContent = expanded ? `Show ${tracks.length - TRACKS_COLLAPSED_COUNT} more` : 'Show less';
  });
}

function renderArtists(artists) {
  const el = document.getElementById('artistGrid');
  if (!artists || !artists.length) { el.innerHTML = '<p class="empty-note">No top artists yet.</p>'; return; }
  el.innerHTML = artists.map(a => `
    <div class="artist-card">
      <div class="artist-swatch">${mediaTag({ url: a.imageUrl, name: a.name, seed: a.artistId })}</div>
      <div class="artist-name"><span class="rk mono">${a.rank}</span>${escapeHtml(a.name)}</div>
    </div>
  `).join('');
}

function renderGenres(genres) {
  const el = document.getElementById('genreList');
  if (!genres || !genres.length) { el.innerHTML = '<p class="empty-note">No genre data yet.</p>'; return; }
  el.innerHTML = genres.map(g => `
    <div class="genre-row">
      <span class="name">${escapeHtml(g.name)}</span>
      <span class="pct mono">${g.percentage}%</span>
    </div>
  `).join('');
}

const PLAYLISTS_COLLAPSED_COUNT = 5;

function playlistCardHtml(p) {
  playlistCache[p.playlistId] = p;
  return `
    <div class="playlist-card" data-playlist-id="${p.playlistId}">
      <button class="playlist-cover-btn" onclick="openPlaylist(${p.playlistId})" aria-label="Open ${escapeHtml(p.title)}">
        <div class="playlist-cover">${mediaTag({ url: p.coverUrl, name: p.title, seed: p.playlistId })}</div>
      </button>
      <div class="playlist-row">
        <div class="clickable-name" onclick="openPlaylist(${p.playlistId})">
          <p class="playlist-title">${escapeHtml(p.title)}</p>
          <p class="playlist-sub mono">${p.trackCount} tracks</p>
        </div>
        <button class="playlist-like-btn ${p.likedByCurrentUser ? 'liked' : ''}" data-liked="${!!p.likedByCurrentUser}"
                onclick="event.stopPropagation(); togglePlaylistLike(${p.playlistId}, this.dataset.liked === 'true')" title="Like this playlist">
          <svg viewBox="0 0 24 24"><path d="M12 20s-7-4.4-9.5-9A5.5 5.5 0 0 1 12 6a5.5 5.5 0 0 1 9.5 5c-2.5 4.6-9.5 9-9.5 9Z"/></svg>
          <span class="like-count">${p.likeCount ?? 0}</span>
        </button>
      </div>
    </div>
  `;
}

function renderPlaylists(playlists) {
  const el = document.getElementById('playlistGrid');
  if (!playlists || !playlists.length) { el.innerHTML = '<p class="empty-note">No playlists yet.</p>'; return; }

  if (playlists.length <= PLAYLISTS_COLLAPSED_COUNT) {
    el.innerHTML = playlists.map(playlistCardHtml).join('');
    return;
  }

  const visible = playlists.slice(0, PLAYLISTS_COLLAPSED_COUNT).map(playlistCardHtml).join('');
  const rest = playlists.slice(PLAYLISTS_COLLAPSED_COUNT).map(playlistCardHtml).join('');

  el.innerHTML = `
    <div id="playlistsVisible" class="playlist-grid">${visible}</div>
    <div id="playlistsExtra" class="playlist-grid" style="display:none;">${rest}</div>
    <button type="button" id="playlistsToggleBtn" class="playlists-toggle-btn">See all ${playlists.length} playlists</button>
  `;

  const extra = document.getElementById('playlistsExtra');
  const toggleBtn = document.getElementById('playlistsToggleBtn');
  toggleBtn.addEventListener('click', () => {
    const expanded = extra.style.display !== 'none';
    extra.style.display = expanded ? 'none' : 'grid';
    toggleBtn.textContent = expanded ? `See all ${playlists.length} playlists` : 'Show less';
  });
}

const playlistCache = {};
let tabBeforePlaylist = 'profile';

async function openPlaylist(playlistId) {
  const p = playlistCache[playlistId];
  if (!p) return;
  const activeTab = document.querySelector('.nav-link.active');
  tabBeforePlaylist = activeTab ? activeTab.dataset.tab : 'profile';
  document.querySelectorAll('.nav-link').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.page').forEach(pg => pg.classList.toggle('active', pg.id === 'page-playlist'));

  document.getElementById('playlistHeroCover').innerHTML = mediaTag({ url: p.coverUrl, name: p.title, seed: p.playlistId });
  document.getElementById('playlistHeroTitle').textContent = p.title;
  document.getElementById('playlistHeroMeta').textContent = `${p.trackCount} tracks`;
  const tracksEl = document.getElementById('playlistTracklist');
  tracksEl.innerHTML = '<p class="empty-note">Loading…</p>';
  window.scrollTo(0, 0);
  try {
    const tracks = await Api.getPlaylistTracks(playlistId);
    if (!tracks.length) { tracksEl.innerHTML = '<p class="empty-note">No tracks to show yet.</p>'; return; }
    tracksEl.innerHTML = tracks.map((t, i) => `
      <div class="pl-track-row">
        <span class="pl-track-rank mono">${i + 1}</span>
        <div class="pl-track-cover">${mediaTag({ url: t.coverUrl, name: t.title, seed: t.trackId })}</div>
        <div style="flex:1;min-width:0;">
          <div class="pl-track-title">${escapeHtml(t.title)}</div>
          <div class="pl-track-artist">${escapeHtml(t.artist)}</div>
        </div>
        ${t.duration ? `<span class="pl-track-duration mono">${escapeHtml(t.duration)}</span>` : ''}
      </div>
    `).join('');
  } catch (err) {
    tracksEl.innerHTML = `<p class="error-inline">${escapeHtml(err.message)}</p>`;
  }
}
window.openPlaylist = openPlaylist;

function closePlaylistPage() {
  activateTab(tabBeforePlaylist);
}

async function togglePlaylistLike(playlistId, liked) {
  try {
    const res = liked ? await Api.unlikePlaylist(playlistId) : await Api.likePlaylist(playlistId);
    const card = document.querySelector(`.playlist-card[data-playlist-id="${playlistId}"]`);
    if (!card) return;
    const btn = card.querySelector('.playlist-like-btn');
    btn.dataset.liked = String(res.likedByCurrentUser);
    btn.classList.toggle('liked', res.likedByCurrentUser);
    btn.querySelector('.like-count').textContent = res.likeCount;
  } catch (err) {
    alert('Could not update like — ' + err.message);
  }
}
window.togglePlaylistLike = togglePlaylistLike;

function renderComments(comments) {
  const el = document.getElementById('commentList');
  if (!comments || !comments.length) { el.innerHTML = '<p class="empty-note">No comments yet — be the first.</p>'; return; }
  el.innerHTML = comments.map(c => {
    peopleCache[c.author.userId] = c.author;
    return `
    <div class="comment" data-comment-id="${c.commentId}">
      <div class="c-badge clickable-name" onclick="viewProfile(${c.author.userId})">${mediaTag({ url: c.author.avatarUrl, name: c.author.displayName, seed: c.author.userId })}</div>
      <div style="flex:1;">
        <div class="comment-head">
          <span class="comment-name clickable-name" onclick="viewProfile(${c.author.userId})">${escapeHtml(c.author.displayName)}</span>
          <span class="comment-time">${formatRelativeTime(c.createdAt)}</span>
        </div>
        <p class="comment-text">${escapeHtml(c.text)}</p>
        <div class="comment-actions">
          <button class="like-btn ${c.likedByCurrentUser ? 'liked' : ''}" data-liked="${!!c.likedByCurrentUser}"
                  onclick="toggleLike(${c.commentId}, this.dataset.liked === 'true')">
            <svg viewBox="0 0 24 24"><path d="M12 20s-7-4.4-9.5-9A5.5 5.5 0 0 1 12 6a5.5 5.5 0 0 1 9.5 5c-2.5 4.6-9.5 9-9.5 9Z"/></svg>
            <span class="like-count">${c.likeCount ?? 0}</span>
          </button>
        </div>
      </div>
    </div>
  `;
  }).join('');
}

async function toggleLike(commentId, liked) {
  try {
    const res = liked ? await Api.unlikeComment(commentId) : await Api.likeComment(commentId);
    const row = document.querySelector(`[data-comment-id="${commentId}"]`);
    if (!row) return;
    const btn = row.querySelector('.like-btn');
    btn.dataset.liked = String(res.likedByCurrentUser);
    btn.classList.toggle('liked', res.likedByCurrentUser);
    btn.querySelector('.like-count').textContent = res.likeCount;
  } catch (err) {
    alert('Could not update like — ' + err.message);
  }
}
window.toggleLike = toggleLike;

async function handleCommentSubmit(e) {
  e.preventDefault();
  const textarea = document.getElementById('commentInput');
  const text = textarea.value.trim();
  if (!text) return;
  const btn = document.getElementById('commentSubmitBtn');
  btn.disabled = true;
  try {
    await Api.postComment(viewedUserId, text);
    textarea.value = '';
    await loadSection(() => Api.getComments(viewedUserId).then(renderComments), 'commentList');
  } catch (err) {
    alert('Could not post comment — ' + err.message);
  } finally {
    btn.disabled = false;
  }
}

// ------------------------------------------------------------------------ feed
async function loadFeedPage() {
  if (feedLoaded) return;
  await loadSection(() => Api.getFeed(CURRENT_USER_ID).then(renderFeed), 'feedList');
  feedLoaded = true;
}

function renderFeed(items) {
  const el = document.getElementById('feedList');
  if (!items || !items.length) { el.innerHTML = '<p class="empty-note">No activity yet.</p>'; return; }
  el.innerHTML = items.map(item => {
    peopleCache[item.actor.userId] = item.actor;
    const isQuote = item.type === 'COMMENTED' && item.subtitle;
    return `
    <div class="feed-item">
      <div class="feed-avatar clickable-name" onclick="viewProfile(${item.actor.userId})">${mediaTag({ url: item.actor.avatarUrl, name: item.actor.displayName, seed: item.actor.userId })}</div>
      <div class="feed-body">
        <div class="feed-row-top">
          <div class="feed-text"><b class="clickable-name" onclick="viewProfile(${item.actor.userId})">${escapeHtml(item.actor.displayName)}</b> ${escapeHtml(item.verb)} <b>${escapeHtml(item.target)}</b></div>
          <div class="feed-time">${formatRelativeTime(item.createdAt)}</div>
        </div>
        ${item.subtitle ? `<div class="feed-sub ${isQuote ? 'is-quote' : ''}">${escapeHtml(item.subtitle)}</div>` : ''}
      </div>
    </div>
  `;
  }).join('');
}

// --------------------------------------------------------------------- friends
async function getFriendsList() {
  if (!friendsListCache) {
    friendsListCache = await Api.getFollowing(CURRENT_USER_ID);
    friendsListCache.forEach(u => { peopleCache[u.userId] = u; });
  }
  return friendsListCache;
}

async function loadFriendsPage() {
  await loadSection(async () => {
    const friends = await getFriendsList();
    document.getElementById('peopleHeading').textContent = 'Your friends';
    renderPeopleGrid(friends, { showFollow: true, removeOnUnfollow: true });
  }, 'peopleGrid');
}

function renderPeopleGrid(people, { showFollow = false, removeOnUnfollow = false } = {}) {
  const el = document.getElementById('peopleGrid');
  if (!people || !people.length) { el.innerHTML = '<p class="empty-note">Nobody here yet.</p>'; return; }
  const friendIds = new Set((friendsListCache || []).map(u => u.userId));
  el.innerHTML = people.map(u => {
    const isFriend = friendIds.has(u.userId);
    return `
    <div class="person-card" data-user-id="${u.userId}">
      <div class="clickable-area" onclick="viewProfile(${u.userId})">
        <div class="person-avatar">${mediaTag({ url: u.avatarUrl, name: u.displayName, seed: u.userId })}</div>
        <div class="person-name">${escapeHtml(u.displayName)}</div>
        ${u.handle ? `<div class="person-handle mono">${escapeHtml(u.handle)}</div>` : ''}
        ${u.topGenre ? `<div class="person-sub">${escapeHtml(u.topGenre)}</div>` : ''}
      </div>
      ${showFollow ? `<button class="follow-btn ${isFriend ? 'following' : ''}" onclick="event.stopPropagation(); toggleFollow(${u.userId}, this, ${removeOnUnfollow})">${isFriend ? 'Following' : 'Follow'}</button>` : ''}
    </div>
  `;
  }).join('');
}

async function toggleFollow(userId, btn, removeOnUnfollow = false) {
  const isFollowing = btn.classList.contains('following');
  btn.disabled = true;
  try {
    if (isFollowing) {
      await Api.unfollowUser(userId);
      friendsListCache = (friendsListCache || []).filter(u => u.userId !== userId);
      if (removeOnUnfollow) {
        btn.closest('.person-card')?.remove();
        if (!document.querySelector('#peopleGrid .person-card')) {
          document.getElementById('peopleGrid').innerHTML = '<p class="empty-note">Nobody here yet.</p>';
        }
        return;
      }
      btn.classList.remove('following');
      btn.textContent = 'Follow';
    } else {
      await Api.followUser(userId);
      const person = peopleCache[userId];
      if (person && !(friendsListCache || []).some(u => u.userId === userId)) {
        (friendsListCache = friendsListCache || []).push(person);
      }
      btn.classList.add('following');
      btn.textContent = 'Following';
    }
  } catch (err) {
    alert('Could not update follow — ' + err.message);
  } finally {
    btn.disabled = false;
  }
}
window.toggleFollow = toggleFollow;

let searchDebounce = null;
function handleSearchInput(e) {
  const query = e.target.value.trim();
  clearTimeout(searchDebounce);
  if (!query) {
    document.getElementById('peopleHeading').textContent = 'Your friends';
    getFriendsList().then(friends => renderPeopleGrid(friends, { showFollow: true, removeOnUnfollow: true }));
    return;
  }
  searchDebounce = setTimeout(async () => {
    await loadSection(async () => {
      const results = (await Api.searchUsers(query)).filter(u => u.userId !== CURRENT_USER_ID);
      results.forEach(u => { peopleCache[u.userId] = u; });
      document.getElementById('peopleHeading').textContent = `Search results for "${query}"`;
      renderPeopleGrid(results, { showFollow: true });
    }, 'peopleGrid');
  }, 220);
}

// --------------------------------------------------------------- compatibility
let compatPickerLoaded = false;
let compatSelectedId = null;

async function loadCompatPage() {
  const row = document.getElementById('compatPickerRow');
  if (!compatPickerLoaded) {
    try {
      const friends = await getFriendsList();
      populateCompatPicker(friends);
      compatPickerLoaded = true;
    } catch (err) {
      document.getElementById('compatBody').innerHTML = `<p class="error-inline">${escapeHtml(err.message)}</p>`;
      return;
    }
  }
  if (compatSelectedId) loadCompat(compatSelectedId);
}

function populateCompatPicker(friends) {
  const row = document.getElementById('compatPickerRow');
  if (!friends.length) { row.innerHTML = '<p class="empty-note">Follow someone to compare taste.</p>'; return; }
  row.innerHTML = friends.map(u => `
    <button type="button" class="compat-pick ${u.userId === compatSelectedId ? 'active' : ''}" data-user-id="${u.userId}" onclick="selectCompatWith(${u.userId})">
      <span class="compat-pick-avatar">${mediaTag({ url: u.avatarUrl, name: u.displayName, seed: u.userId })}</span>
      <span class="compat-pick-name">${escapeHtml(u.displayName)}</span>
    </button>
  `).join('');
  if (!compatSelectedId) compatSelectedId = friends[0].userId;
}

function selectCompatWith(userId) {
  compatSelectedId = Number(userId);
  document.querySelectorAll('.compat-pick').forEach(b => b.classList.toggle('active', Number(b.dataset.userId) === compatSelectedId));
  loadCompat(compatSelectedId);
}
window.selectCompatWith = selectCompatWith;

/** Ensures `userId` has a picker card even if they're not a friend yet
 *  (e.g. found via search), then selects them. */
async function ensureCompatOption(userId, displayName) {
  if (!compatPickerLoaded) await loadCompatPage();
  const row = document.getElementById('compatPickerRow');
  if (!row.querySelector(`.compat-pick[data-user-id="${userId}"]`)) {
    const person = peopleCache[userId] || { userId, displayName, avatarUrl: null };
    row.insertAdjacentHTML('afterbegin', `
      <button type="button" class="compat-pick" data-user-id="${userId}" onclick="selectCompatWith(${userId})">
        <span class="compat-pick-avatar">${mediaTag({ url: person.avatarUrl, name: person.displayName, seed: userId })}</span>
        <span class="compat-pick-name">${escapeHtml(person.displayName)}</span>
      </button>
    `);
  }
  selectCompatWith(userId);
}

async function goToCompatWith(userId, displayName) {
  activateTab('compat'); // side-effect-free — safe to call without also triggering loadCompatPage()
  await ensureCompatOption(userId, displayName);
}

async function loadCompat(withUserId) {
  await loadSection(async () => {
    const data = await Api.getCompatibility(CURRENT_USER_ID, withUserId);
    renderCompat(data, withUserId);
  }, 'compatBody');
}

function renderCompat(data, withUserId) {
  const other = peopleCache[withUserId] || { displayName: '?' };
  const myName = currentProfile?.displayName || 'You';
  const firstName = n => (n || '').split(' ')[0];
  document.getElementById('duoMe').innerHTML = mediaTag({ url: currentProfile?.avatarUrl, name: myName, seed: currentProfile?.id });
  document.getElementById('duoOther').innerHTML = mediaTag({ url: other.avatarUrl, name: other.displayName, seed: withUserId });
  document.getElementById('duoMeLabel').textContent = myName;
  document.getElementById('duoOtherLabel').textContent = other.displayName || 'Them';
  document.getElementById('faderScaleA').textContent = firstName(myName);
  document.getElementById('faderScaleB').textContent = firstName(other.displayName);
  document.getElementById('compatValue').textContent = data.score;
  document.getElementById('compatCaption').textContent = data.caption || '';

  // The fader sits centered at a 50/50 split and leans toward whichever side
  // shares more — a plain number can't show that, the position does.
  const lean = Math.max(-40, Math.min(40, (data.score - 50) * 0.8));
  document.getElementById('faderFill').style.width = `${50 + lean}%`;
  document.getElementById('faderCap').style.left = `${50 + lean}%`;

  document.getElementById('sharedArtists').innerHTML = (data.sharedArtists || []).map(a => `<span class="chip">${escapeHtml(a)}</span>`).join('') || '<span class="empty-note">No overlap found.</span>';
  document.getElementById('sharedTracks').innerHTML = (data.sharedTracks || []).map(t => `<span class="chip">${escapeHtml(t)}</span>`).join('') || '<span class="empty-note">No overlap found.</span>';
}
