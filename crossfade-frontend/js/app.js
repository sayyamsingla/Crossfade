// ---------------------------------------------------------------------------
// Crossfade frontend — rendering + wiring. All data comes from the backend
// via js/api.js. See README.md for the full endpoint contract.
// ---------------------------------------------------------------------------

const CURRENT_USER_ID = window.CROSSFADE_CONFIG.CURRENT_USER_ID;

const ICONS = {
  TRACK_ADDED: '<svg class="icon" viewBox="0 0 24 24"><path d="M9 18V5l10-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="16" cy="16" r="3"/></svg>',
  FOLLOWED: '<svg class="icon" viewBox="0 0 24 24"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.5-6 8-6s8 2 8 6"/></svg>',
  COMMENTED: '<svg class="icon" viewBox="0 0 24 24"><path d="M21 12a8 8 0 1 1-3.3-6.5L21 4l-1 4.2A8 8 0 0 1 21 12Z"/></svg>',
  TOP_TRACK: '<svg class="icon" viewBox="0 0 24 24"><path d="M3 17l6-6 4 4 8-9"/><path d="M15 6h6v6"/></svg>',
  LIKED_PLAYLIST: '<svg class="icon" viewBox="0 0 24 24"><path d="M12 20s-7-4.4-9.5-9A5.5 5.5 0 0 1 12 6a5.5 5.5 0 0 1 9.5 5c-2.5 4.6-9.5 9-9.5 9Z"/></svg>',
  NOW_LISTENING: '<svg class="icon" viewBox="0 0 24 24"><path d="M4 15v-3a8 8 0 0 1 16 0v3"/><rect x="2" y="14" width="5" height="7" rx="1.5"/><rect x="17" y="14" width="5" height="7" rx="1.5"/></svg>',
};

let currentProfile = null;      // your own profile (CURRENT_USER_ID) — cached once, used for compat "you" avatar
let viewedUserId = CURRENT_USER_ID; // whose profile the Profile tab is currently showing
let peopleCache = {};           // userId -> { userId, displayName, avatarUrl, topGenre } — filled in from every source (profile loads, friends list, search)
let friendsListCache = null;    // array — people you follow, used by both the Friends tab and the Compatibility picker
let feedLoaded = false;
let friendsLoaded = false;

// ---------------------------------------------------------------------- init
document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  initTabs();
  document.getElementById('retryBtn').addEventListener('click', () => loadProfilePage());
  document.getElementById('commentForm').addEventListener('submit', handleCommentSubmit);
  document.getElementById('searchForm').addEventListener('submit', handleSearchSubmit);
  document.getElementById('backToMeBtn').addEventListener('click', () => {
    viewedUserId = CURRENT_USER_ID;
    loadProfilePage();
  });
  document.getElementById('compareBtn').addEventListener('click', () => {
    const person = peopleCache[viewedUserId];
    goToCompatWith(viewedUserId, person ? person.displayName : 'them');
  });
  loadProfilePage();
});

function initTheme() {
  const btn = document.getElementById('themeToggle');
  btn.addEventListener('click', () => {
    const isLight = document.documentElement.getAttribute('data-theme') === 'light';
    document.documentElement.setAttribute('data-theme', isLight ? 'dark' : 'light');
    btn.textContent = isLight ? 'Light mode' : 'Dark mode';
  });
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
  loadSection(() => Api.getTopGenres(userId).then(renderGenres), 'genreList');
  loadSection(() => Api.getPlaylists(userId).then(renderPlaylists), 'playlistGrid');
  loadSection(() => Api.getComments(userId).then(renderComments), 'commentList');
}

function renderHero(p) {
  const isSelf = viewedUserId === CURRENT_USER_ID;
  document.getElementById('heroAvatarWrap').innerHTML = mediaTag({ url: p.avatarUrl, name: p.displayName, seed: p.id });
  document.getElementById('idChip').textContent = initialsOf(currentProfile ? currentProfile.displayName : p.displayName);
  document.getElementById('heroName').textContent = p.displayName;
  document.getElementById('heroFollowers').textContent = (p.followersCount ?? 0).toLocaleString();
  document.getElementById('heroFollowing').textContent = (p.followingCount ?? 0).toLocaleString();
  document.getElementById('heroTopGenre').textContent = p.topGenre || '—';
  const bioEl = document.getElementById('heroBio');
  if (p.bio) { bioEl.textContent = p.bio; bioEl.style.display = 'block'; } else { bioEl.style.display = 'none'; }

  const banner = document.getElementById('viewingBanner');
  const compareBtn = document.getElementById('compareBtn');
  banner.style.display = isSelf ? 'none' : 'flex';
  if (!isSelf) document.getElementById('viewingText').textContent = `Viewing ${p.displayName}'s profile`;
  compareBtn.style.display = isSelf ? 'none' : 'inline-block';
}

function renderNowPlaying(data) {
  const el = document.getElementById('nowPlayingStrip');
  if (!data || !data.playing || !data.track) { el.style.display = 'none'; return; }
  el.style.display = 'flex';
  el.innerHTML = `
    <div class="eq"><span></span><span></span><span></span></div>
    <div class="np-cover">${mediaTag({ url: data.track.coverUrl, name: data.track.title, seed: data.track.title })}</div>
    <div class="np-info"><b>${escapeHtml(data.track.title)}</b> <span class="sub">· ${escapeHtml(data.track.artist)}</span></div>
    <div class="np-tag">Listening now</div>
  `;
}

function renderTracks(tracks) {
  const el = document.getElementById('tracksList');
  if (!tracks || !tracks.length) { el.innerHTML = '<p class="empty-note">No top tracks yet.</p>'; return; }
  const max = Math.max(...tracks.map(t => t.playCount || 0), 1);
  el.innerHTML = tracks.map(t => `
    <div class="track-row">
      <div class="track-rank mono">${t.rank}</div>
      <div class="track-cover">${mediaTag({ url: t.coverUrl, name: t.title, seed: t.trackId })}</div>
      <div style="flex:1;min-width:0;">
        <div class="track-name">${escapeHtml(t.title)}</div>
        <div class="track-artist">${escapeHtml(t.artist)}</div>
      </div>
      <div class="track-bar"><div class="track-bar-fill" style="width:${((t.playCount || 0) / max * 100).toFixed(0)}%"></div></div>
      <div class="track-plays mono">${t.playCount ?? 0} plays</div>
    </div>
  `).join('');
}

function renderArtists(artists) {
  const el = document.getElementById('artistGrid');
  if (!artists || !artists.length) { el.innerHTML = '<p class="empty-note">No top artists yet.</p>'; return; }
  el.innerHTML = artists.map(a => `
    <div class="artist-card">
      <div class="artist-swatch">
        ${mediaTag({ url: a.imageUrl, name: a.name, seed: a.artistId })}
        <span class="rk">${a.rank}</span>
      </div>
      <div class="artist-name">${escapeHtml(a.name)}</div>
    </div>
  `).join('');
}

function renderGenres(genres) {
  const el = document.getElementById('genreList');
  if (!genres || !genres.length) { el.innerHTML = '<p class="empty-note">No genre data yet.</p>'; return; }
  el.innerHTML = genres.map((g, i) => `
    <div class="genre-pill">
      <span class="dot" style="background:var(--accent);opacity:${Math.max(1 - i * 0.15, 0.35)}"></span>
      <span class="name">${escapeHtml(g.name)}</span>
      <span class="pct mono">${g.percentage}%</span>
    </div>
  `).join('');
}

function renderPlaylists(playlists) {
  const el = document.getElementById('playlistGrid');
  if (!playlists || !playlists.length) { el.innerHTML = '<p class="empty-note">No playlists yet.</p>'; return; }
  el.innerHTML = playlists.map(p => `
    <div>
      <div class="playlist-cover">
        ${mediaTag({ url: p.coverUrl, name: p.title, seed: p.playlistId })}
        <span class="count mono">${p.trackCount}</span>
      </div>
      <p class="playlist-title">${escapeHtml(p.title)}</p>
      <p class="playlist-sub mono">${p.trackCount} tracks</p>
    </div>
  `).join('');
}

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
            ♥ <span class="like-count">${c.likeCount ?? 0}</span>
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
    await Api.postComment(viewedUserId, CURRENT_USER_ID, text);
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
    return `
    <div class="feed-item">
      <div class="feed-icon">${ICONS[item.type] || ICONS.TRACK_ADDED}</div>
      <div style="flex:1;">
        <div class="feed-row-top">
          <div class="feed-text"><b class="clickable-name" onclick="viewProfile(${item.actor.userId})">${escapeHtml(item.actor.displayName)}</b> ${escapeHtml(item.verb)} <b>${escapeHtml(item.target)}</b></div>
          <div class="feed-time">${formatRelativeTime(item.createdAt)}</div>
        </div>
        ${item.subtitle ? `<div class="feed-sub">${escapeHtml(item.subtitle)}</div>` : ''}
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
  if (friendsLoaded) return;
  await loadSection(async () => {
    const friends = await getFriendsList();
    document.getElementById('peopleHeading').textContent = 'Your friends';
    renderPeopleGrid(friends);
  }, 'peopleGrid');
  friendsLoaded = true;
}

function renderPeopleGrid(people) {
  const el = document.getElementById('peopleGrid');
  if (!people || !people.length) { el.innerHTML = '<p class="empty-note">Nobody here yet.</p>'; return; }
  el.innerHTML = people.map(u => `
    <div class="person-card" onclick="viewProfile(${u.userId})">
      <div class="person-avatar">${mediaTag({ url: u.avatarUrl, name: u.displayName, seed: u.userId })}</div>
      <div class="person-name">${escapeHtml(u.displayName)}</div>
      ${u.topGenre ? `<div class="person-sub">${escapeHtml(u.topGenre)}</div>` : ''}
    </div>
  `).join('');
}

async function handleSearchSubmit(e) {
  e.preventDefault();
  const input = document.getElementById('searchInput');
  const query = input.value.trim();
  if (!query) {
    document.getElementById('peopleHeading').textContent = 'Your friends';
    renderPeopleGrid(await getFriendsList());
    return;
  }
  await loadSection(async () => {
    const results = (await Api.searchUsers(query)).filter(u => u.userId !== CURRENT_USER_ID);
    results.forEach(u => { peopleCache[u.userId] = u; });
    document.getElementById('peopleHeading').textContent = `Search results for "${query}"`;
    renderPeopleGrid(results);
  }, 'peopleGrid');
}

// --------------------------------------------------------------- compatibility
async function loadCompatPage() {
  const select = document.getElementById('compatSelect');
  if (!select.dataset.loaded) {
    try {
      const friends = await getFriendsList();
      populateCompatSelect(friends);
      select.dataset.loaded = '1';
    } catch (err) {
      document.getElementById('compatBody').innerHTML = `<p class="error-inline">${escapeHtml(err.message)}</p>`;
      return;
    }
  }
  if (select.value) loadCompat(select.value);
}

function populateCompatSelect(friends) {
  const select = document.getElementById('compatSelect');
  if (!friends.length) { select.innerHTML = '<option>No friends yet</option>'; return; }
  select.innerHTML = friends.map(u => `<option value="${u.userId}">${escapeHtml(u.displayName)}</option>`).join('');
  select.addEventListener('change', () => loadCompat(select.value));
}

/** Ensures `userId` exists as an option in the Compatibility picker even if
 *  they're not a friend yet (e.g. found via search), then selects them. */
async function ensureCompatOption(userId, displayName) {
  const select = document.getElementById('compatSelect');
  if (!select.dataset.loaded) await loadCompatPage();
  if (!select.querySelector(`option[value="${userId}"]`)) {
    const opt = document.createElement('option');
    opt.value = userId;
    opt.textContent = displayName;
    select.prepend(opt);
  }
  select.value = userId;
}

async function goToCompatWith(userId, displayName) {
  activateTab('compat'); // side-effect-free — safe to call without also triggering loadCompatPage()
  await ensureCompatOption(userId, displayName);
  loadCompat(userId);
}

async function loadCompat(withUserId) {
  await loadSection(async () => {
    const data = await Api.getCompatibility(CURRENT_USER_ID, withUserId);
    renderCompat(data, withUserId);
  }, 'compatBody');
}

function renderCompat(data, withUserId) {
  const other = peopleCache[withUserId] || { displayName: '?' };
  document.getElementById('duoMe').innerHTML = mediaTag({ url: currentProfile?.avatarUrl, name: currentProfile?.displayName, seed: currentProfile?.id });
  document.getElementById('duoOther').innerHTML = mediaTag({ url: other.avatarUrl, name: other.displayName, seed: withUserId });
  document.getElementById('compatValue').textContent = `${data.score}%`;
  document.getElementById('compatCaption').textContent = data.caption || '';
  document.getElementById('sharedArtists').innerHTML = (data.sharedArtists || []).map(a => `<span class="chip">${escapeHtml(a)}</span>`).join('') || '<span class="empty-note">No overlap found.</span>';
  document.getElementById('sharedGenres').innerHTML = (data.sharedGenres || []).map(g => `<span class="chip">${escapeHtml(g)}</span>`).join('') || '<span class="empty-note">No overlap found.</span>';
}
