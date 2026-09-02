// ---------------------------------------------------------------------------
// TEMP FAKE DATA — for previewing the frontend before the backend exists.
//
// Delete this whole thing when the real backend is ready:
//   1. Remove this file (js/mock-data.js)
//   2. Remove its <script src="js/mock-data.js"> line in index.html
//   3. Remove/flip USE_MOCK_DATA in js/config.js
// Nothing else in the app references this file directly — api.js checks the
// flag and calls into here, that's the whole integration point.
// ---------------------------------------------------------------------------

(function () {
  if (!window.CROSSFADE_CONFIG.USE_MOCK_DATA) return;

  // Stable, seeded placeholder photos (picsum.photos returns the same image
  // for the same seed every time) — stand-ins for real artist/cover/avatar
  // images until the backend serves the real thing.
  const img = (seed, size = 200) => `https://picsum.photos/seed/${encodeURIComponent(seed)}/${size}/${size}`;

  const USERS = {
    1: { id: 1, displayName: 'Maya Chen', handle: '@mayabeats', bio: 'Vinyl collector. Bass-heavy playlists.', avatarUrl: img('maya-chen'), followersCount: 1204, followingCount: 389, topGenre: 'Indie Pop' },
    2: { id: 2, displayName: 'Jordan Reyes', handle: '@jordanlistens', bio: 'Dream pop enjoyer. Making a mixtape a month.', avatarUrl: img('jordan-reyes'), followersCount: 812, followingCount: 240, topGenre: 'Dream Pop' },
    3: { id: 3, displayName: 'Alex Kim', handle: '@alexk', bio: 'Shoegaze is a personality trait at this point.', avatarUrl: img('alex-kim'), followersCount: 356, followingCount: 190, topGenre: 'Shoegaze' },
    4: { id: 4, displayName: 'Priya Nair', handle: '@priyanspins', bio: 'DJ on weekends, spreadsheet person on weekdays.', avatarUrl: img('priya-nair'), followersCount: 2031, followingCount: 512, topGenre: 'House' },
  };

  const NOW_PLAYING = {
    1: { playing: true, track: { title: 'Kyoto', artist: 'Phoebe Bridgers', coverUrl: img('kyoto-phoebe-bridgers', 100) } },
    2: { playing: true, track: { title: 'Space Song', artist: 'Beach House', coverUrl: img('space-song-beach-house', 100) } },
    3: { playing: false },
    4: { playing: true, track: { title: 'Xtal', artist: 'Aphex Twin', coverUrl: img('xtal-aphex-twin', 100) } },
  };

  const TOP_TRACKS = {
    1: [
      { rank: 1, trackId: 1, title: 'Redbone', artist: 'Childish Gambino', coverUrl: img('redbone-childish-gambino', 120), playCount: 342 },
      { rank: 2, trackId: 2, title: 'Motion Sickness', artist: 'Phoebe Bridgers', coverUrl: img('motion-sickness-phoebe-bridgers', 120), playCount: 298 },
      { rank: 3, trackId: 3, title: 'Little Dark Age', artist: 'MGMT', coverUrl: img('little-dark-age-mgmt', 120), playCount: 210 },
      { rank: 4, trackId: 4, title: 'Myth', artist: 'Beach House', coverUrl: img('myth-beach-house', 120), playCount: 176 },
      { rank: 5, trackId: 5, title: 'Cherry Wine', artist: 'Hozier', coverUrl: img('cherry-wine-hozier', 120), playCount: 154 },
    ],
    2: [
      { rank: 1, trackId: 6, title: 'Space Song', artist: 'Beach House', coverUrl: img('space-song-beach-house', 120), playCount: 401 },
      { rank: 2, trackId: 7, title: 'Motion Sickness', artist: 'Phoebe Bridgers', coverUrl: img('motion-sickness-phoebe-bridgers', 120), playCount: 355 },
      { rank: 3, trackId: 8, title: 'Silver Soul', artist: 'Beach House', coverUrl: img('silver-soul-beach-house', 120), playCount: 288 },
    ],
    3: [
      { rank: 1, trackId: 9, title: 'Only Shallow', artist: 'My Bloody Valentine', coverUrl: img('only-shallow-mbv', 120), playCount: 512 },
      { rank: 2, trackId: 10, title: 'Souvlaki Space Station', artist: 'Slowdive', coverUrl: img('souvlaki-space-station-slowdive', 120), playCount: 470 },
    ],
    4: [
      { rank: 1, trackId: 11, title: 'Xtal', artist: 'Aphex Twin', coverUrl: img('xtal-aphex-twin', 120), playCount: 620 },
      { rank: 2, trackId: 12, title: 'Music Sounds Better With You', artist: 'Stardust', coverUrl: img('music-sounds-better-stardust', 120), playCount: 588 },
    ],
  };

  const TOP_ARTISTS = {
    1: [
      { rank: 1, artistId: 1, name: 'Phoebe Bridgers', imageUrl: img('artist-phoebe-bridgers') },
      { rank: 2, artistId: 2, name: 'MGMT', imageUrl: img('artist-mgmt') },
      { rank: 3, artistId: 3, name: 'Beach House', imageUrl: img('artist-beach-house') },
      { rank: 4, artistId: 4, name: 'Hozier', imageUrl: img('artist-hozier') },
    ],
    2: [
      { rank: 1, artistId: 3, name: 'Beach House', imageUrl: img('artist-beach-house') },
      { rank: 2, artistId: 1, name: 'Phoebe Bridgers', imageUrl: img('artist-phoebe-bridgers') },
      { rank: 3, artistId: 5, name: 'Men I Trust', imageUrl: img('artist-men-i-trust') },
    ],
    3: [
      { rank: 1, artistId: 6, name: 'My Bloody Valentine', imageUrl: img('artist-my-bloody-valentine') },
      { rank: 2, artistId: 7, name: 'Slowdive', imageUrl: img('artist-slowdive') },
    ],
    4: [
      { rank: 1, artistId: 8, name: 'Aphex Twin', imageUrl: img('artist-aphex-twin') },
      { rank: 2, artistId: 9, name: 'Stardust', imageUrl: img('artist-stardust') },
    ],
  };

  const TOP_GENRES = {
    1: [
      { name: 'Indie Pop', percentage: 34 },
      { name: 'Dream Pop', percentage: 26 },
      { name: 'Bedroom Pop', percentage: 18 },
      { name: 'Folk', percentage: 12 },
      { name: 'Electronic', percentage: 10 },
    ],
    2: [
      { name: 'Dream Pop', percentage: 41 },
      { name: 'Indie Pop', percentage: 22 },
      { name: 'Shoegaze', percentage: 20 },
      { name: 'Bedroom Pop', percentage: 17 },
    ],
    3: [
      { name: 'Shoegaze', percentage: 52 },
      { name: 'Dream Pop', percentage: 28 },
      { name: 'Noise Rock', percentage: 20 },
    ],
    4: [
      { name: 'House', percentage: 38 },
      { name: 'IDM', percentage: 24 },
      { name: 'Techno', percentage: 22 },
      { name: 'Ambient', percentage: 16 },
    ],
  };

  const PLAYLISTS = {
    1: [
      { playlistId: 1, title: 'Late Night Drive', trackCount: 42, coverUrl: img('playlist-late-night-drive', 160), likeCount: 14, likedByCurrentUser: false },
      { playlistId: 2, title: 'Sunday Reset', trackCount: 18, coverUrl: img('playlist-sunday-reset', 160), likeCount: 5, likedByCurrentUser: false },
      { playlistId: 3, title: 'Vinyl Only', trackCount: 27, coverUrl: img('playlist-vinyl-only', 160), likeCount: 9, likedByCurrentUser: true },
    ],
    2: [
      { playlistId: 4, title: 'Monthly Mixtape #14', trackCount: 12, coverUrl: img('playlist-monthly-mixtape-14', 160), likeCount: 3, likedByCurrentUser: false },
      { playlistId: 5, title: 'Rainy Day', trackCount: 33, coverUrl: img('playlist-rainy-day', 160), likeCount: 1, likedByCurrentUser: false },
    ],
    3: [
      { playlistId: 6, title: 'Wall of Sound', trackCount: 21, coverUrl: img('playlist-wall-of-sound', 160), likeCount: 0, likedByCurrentUser: false },
    ],
    4: [
      { playlistId: 7, title: 'Warehouse Set', trackCount: 55, coverUrl: img('playlist-warehouse-set', 160), likeCount: 22, likedByCurrentUser: false },
      { playlistId: 8, title: 'Afters', trackCount: 40, coverUrl: img('playlist-afters', 160), likeCount: 6, likedByCurrentUser: false },
    ],
  };

  // Only a handful of tracks per playlist are mocked (not up to trackCount) —
  // this is preview data, not a real tracklist.
  const PLAYLIST_TRACKS = {
    1: [
      { trackId: 1, title: 'Redbone', artist: 'Childish Gambino', coverUrl: img('redbone-childish-gambino', 120), duration: '5:27' },
      { trackId: 2, title: 'Motion Sickness', artist: 'Phoebe Bridgers', coverUrl: img('motion-sickness-phoebe-bridgers', 120), duration: '4:04' },
      { trackId: 13, title: 'Nightcall', artist: 'Kavinsky', coverUrl: img('nightcall-kavinsky', 120), duration: '4:18' },
      { trackId: 14, title: 'The Less I Know The Better', artist: 'Tame Impala', coverUrl: img('the-less-i-know-tame-impala', 120), duration: '3:36' },
    ],
    2: [
      { trackId: 15, title: 'Holocene', artist: 'Bon Iver', coverUrl: img('holocene-bon-iver', 120), duration: '5:36' },
      { trackId: 16, title: 'Skinny Love', artist: 'Bon Iver', coverUrl: img('skinny-love-bon-iver', 120), duration: '3:58' },
    ],
    3: [
      { trackId: 9, title: 'Only Shallow', artist: 'My Bloody Valentine', coverUrl: img('only-shallow-mbv', 120), duration: '4:17' },
      { trackId: 10, title: 'Souvlaki Space Station', artist: 'Slowdive', coverUrl: img('souvlaki-space-station-slowdive', 120), duration: '4:11' },
      { trackId: 17, title: 'Alison', artist: 'Slowdive', coverUrl: img('alison-slowdive', 120), duration: '4:53' },
    ],
    4: [
      { trackId: 6, title: 'Space Song', artist: 'Beach House', coverUrl: img('space-song-beach-house', 120), duration: '5:22' },
      { trackId: 8, title: 'Silver Soul', artist: 'Beach House', coverUrl: img('silver-soul-beach-house', 120), duration: '4:32' },
    ],
    5: [
      { trackId: 18, title: 'Rainy Day Women', artist: 'Bob Dylan', coverUrl: img('rainy-day-women-dylan', 120), duration: '4:35' },
    ],
    6: [
      { trackId: 11, title: 'Xtal', artist: 'Aphex Twin', coverUrl: img('xtal-aphex-twin', 120), duration: '4:51' },
    ],
    7: [
      { trackId: 12, title: 'Music Sounds Better With You', artist: 'Stardust', coverUrl: img('music-sounds-better-stardust', 120), duration: '7:42' },
      { trackId: 19, title: 'One More Time', artist: 'Daft Punk', coverUrl: img('one-more-time-daft-punk', 120), duration: '5:20' },
    ],
    8: [
      { trackId: 20, title: 'Losing It', artist: 'FISHER', coverUrl: img('losing-it-fisher', 120), duration: '5:34' },
    ],
  };

  const COMMENTS = {
    1: [
      { commentId: 1, author: { userId: 2, displayName: 'Jordan Reyes', avatarUrl: img('jordan-reyes', 60) }, text: 'Your top tracks this month are basically my whole personality, how', createdAt: '2026-08-20T05:00:00Z', likeCount: 6, likedByCurrentUser: false },
      { commentId: 2, author: { userId: 3, displayName: 'Alex Kim', avatarUrl: img('alex-kim', 60) }, text: 'ok the Late Night Drive playlist is unreasonably good', createdAt: '2026-08-18T21:15:00Z', likeCount: 3, likedByCurrentUser: true },
    ],
    2: [
      { commentId: 3, author: { userId: 1, displayName: 'Maya Chen', avatarUrl: img('maya-chen', 60) }, text: 'send me the rest of that mixtape', createdAt: '2026-08-19T10:00:00Z', likeCount: 2, likedByCurrentUser: false },
    ],
    3: [],
    4: [
      { commentId: 4, author: { userId: 1, displayName: 'Maya Chen', avatarUrl: img('maya-chen', 60) }, text: 'the Warehouse Set is unreal, saved it', createdAt: '2026-08-21T02:30:00Z', likeCount: 8, likedByCurrentUser: false },
    ],
  };

  const FEED = {
    1: [
      { feedId: 1, type: 'TRACK_ADDED', actor: { userId: 2, displayName: 'Jordan Reyes', avatarUrl: img('jordan-reyes', 60) }, verb: 'added 3 tracks to', target: 'Monthly Mixtape #14', subtitle: 'Monthly Mixtape #14 · 12 tracks', createdAt: '2026-08-20T07:48:00Z' },
      { feedId: 2, type: 'FOLLOWED', actor: { userId: 4, displayName: 'Priya Nair', avatarUrl: img('priya-nair', 60) }, verb: 'started following', target: 'you', subtitle: '', createdAt: '2026-08-19T02:00:00Z' },
      { feedId: 3, type: 'COMMENTED', actor: { userId: 3, displayName: 'Alex Kim', avatarUrl: img('alex-kim', 60) }, verb: 'commented on', target: 'your profile', subtitle: '"ok the Late Night Drive playlist is unreasonably good"', createdAt: '2026-08-18T21:15:00Z' },
      { feedId: 4, type: 'TOP_TRACK', actor: { userId: 2, displayName: 'Jordan Reyes', avatarUrl: img('jordan-reyes', 60) }, verb: 'is now most into', target: 'Space Song by Beach House', subtitle: '', createdAt: '2026-08-17T14:00:00Z' },
      { feedId: 5, type: 'LIKED_PLAYLIST', actor: { userId: 4, displayName: 'Priya Nair', avatarUrl: img('priya-nair', 60) }, verb: 'liked your playlist', target: 'Vinyl Only', subtitle: '', createdAt: '2026-08-16T09:20:00Z' },
      { feedId: 6, type: 'NOW_LISTENING', actor: { userId: 3, displayName: 'Alex Kim', avatarUrl: img('alex-kim', 60) }, verb: 'is listening to', target: 'Only Shallow by My Bloody Valentine', subtitle: '', createdAt: '2026-08-16T02:00:00Z' },
    ],
  };

  const FOLLOWING = {
    1: [
      { userId: 2, displayName: 'Jordan Reyes', avatarUrl: img('jordan-reyes', 80), topGenre: 'Dream Pop' },
      { userId: 3, displayName: 'Alex Kim', avatarUrl: img('alex-kim', 80), topGenre: 'Shoegaze' },
      { userId: 4, displayName: 'Priya Nair', avatarUrl: img('priya-nair', 80), topGenre: 'House' },
    ],
  };

  const ALL_PEOPLE = Object.values(USERS).map(u => ({ userId: u.id, displayName: u.displayName, avatarUrl: u.avatarUrl, topGenre: u.topGenre }));

  const COMPATIBILITY = {
    '1:2': { score: 74, caption: 'You and Jordan share a lot of the same lane — mostly overlapping in indie pop and dream pop.', sharedArtists: ['Phoebe Bridgers', 'MGMT', 'Beach House'], sharedTracks: ['Motion Sickness'] },
    '1:3': { score: 38, caption: 'A little overlap in dream pop, but Alex leans a lot heavier into shoegaze than you do.', sharedArtists: ['Beach House'], sharedTracks: [] },
    '1:4': { score: 21, caption: 'Not much crossover — Priya is deep in house and IDM, pretty different lane from yours.', sharedArtists: [], sharedTracks: [] },
  };

  const delay = (ms = 220) => new Promise(res => setTimeout(res, ms));

  const MockApi = {
    async getProfile(userId) { await delay(); return USERS[userId] || USERS[1]; },
    async getNowPlaying(userId) { await delay(); return NOW_PLAYING[userId] || { playing: false }; },
    async getTopTracks(userId) { await delay(); return TOP_TRACKS[userId] || []; },
    async getTopArtists(userId) { await delay(); return TOP_ARTISTS[userId] || []; },
    async getTopGenres(userId) { await delay(); return TOP_GENRES[userId] || []; },
    async getPlaylists(userId) { await delay(); return PLAYLISTS[userId] || []; },
    async getPlaylistTracks(playlistId) { await delay(); return PLAYLIST_TRACKS[playlistId] || []; },

    async getComments(userId) { await delay(); return COMMENTS[userId] || []; },
    async postComment(userId, authorId, text) {
      await delay();
      const comment = {
        commentId: Date.now(),
        author: { userId: authorId, displayName: USERS[authorId]?.displayName || 'You', avatarUrl: USERS[authorId]?.avatarUrl || null },
        text,
        createdAt: new Date().toISOString(),
        likeCount: 0,
        likedByCurrentUser: false,
      };
      (COMMENTS[userId] = COMMENTS[userId] || []).unshift(comment);
      return comment;
    },
    async likeComment(commentId) {
      await delay(120);
      for (const list of Object.values(COMMENTS)) {
        const c = list.find(c => c.commentId === commentId);
        if (c) { c.likedByCurrentUser = true; c.likeCount += 1; return { likeCount: c.likeCount, likedByCurrentUser: true }; }
      }
      return { likeCount: 1, likedByCurrentUser: true };
    },
    async unlikeComment(commentId) {
      await delay(120);
      for (const list of Object.values(COMMENTS)) {
        const c = list.find(c => c.commentId === commentId);
        if (c) { c.likedByCurrentUser = false; c.likeCount = Math.max(0, c.likeCount - 1); return { likeCount: c.likeCount, likedByCurrentUser: false }; }
      }
      return { likeCount: 0, likedByCurrentUser: false };
    },

    async getFeed(userId) { await delay(); return FEED[userId] || []; },

    async getFollowing(userId) { await delay(); return FOLLOWING[userId] || []; },
    async searchUsers(query) {
      await delay(120);
      const q = query.trim().toLowerCase();
      if (!q) return [];
      return ALL_PEOPLE.filter(p => p.displayName.toLowerCase().includes(q));
    },
    async followUser(userId) {
      await delay(150);
      const list = (FOLLOWING[1] = FOLLOWING[1] || []);
      if (!list.some(u => u.userId === userId)) {
        const person = ALL_PEOPLE.find(p => p.userId === userId);
        if (person) list.push(person);
      }
      return { following: true };
    },
    async unfollowUser(userId) {
      await delay(150);
      const list = FOLLOWING[1] || [];
      FOLLOWING[1] = list.filter(u => u.userId !== userId);
      return { following: false };
    },
    async likePlaylist(playlistId) {
      await delay(120);
      for (const list of Object.values(PLAYLISTS)) {
        const p = list.find(p => p.playlistId === playlistId);
        if (p) { p.likedByCurrentUser = true; p.likeCount = (p.likeCount || 0) + 1; return { likeCount: p.likeCount, likedByCurrentUser: true }; }
      }
      return { likeCount: 1, likedByCurrentUser: true };
    },
    async unlikePlaylist(playlistId) {
      await delay(120);
      for (const list of Object.values(PLAYLISTS)) {
        const p = list.find(p => p.playlistId === playlistId);
        if (p) { p.likedByCurrentUser = false; p.likeCount = Math.max(0, (p.likeCount || 0) - 1); return { likeCount: p.likeCount, likedByCurrentUser: false }; }
      }
      return { likeCount: 0, likedByCurrentUser: false };
    },
    async getCompatibility(userId, withUserId) {
      await delay();
      return COMPATIBILITY[`${userId}:${withUserId}`] || COMPATIBILITY[`${withUserId}:${userId}`] || {
        score: 50, caption: 'Not enough shared listening history yet.', sharedArtists: [], sharedTracks: [],
      };
    },
  };

  window.MockApi = MockApi;
})();
