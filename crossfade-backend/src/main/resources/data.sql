-- Clear children before parents so FK constraints don't block re-runs
DELETE FROM feed_event;
DELETE FROM comment_like;
DELETE FROM comment;
DELETE FROM sync_log;
DELETE FROM playlist_like;
DELETE FROM playlist_track;
DELETE FROM user_top_track;
DELETE FROM user_top_artist;
DELETE FROM user_top_genre;
DELETE FROM follow;
DELETE FROM playlist;
UPDATE user SET now_playing_track_id = NULL;
DELETE FROM track;
DELETE FROM artist;
DELETE FROM user;

-- Artists
INSERT INTO artist (id, name, image_url) VALUES
  (1, 'Phoebe Bridgers', NULL),
  (2, 'Childish Gambino', NULL),
  (3, 'MGMT', NULL);

-- Users (now_playing_track_id set after tracks exist)
INSERT INTO user (id, display_name, handle, bio, avatar_url, now_playing_track_id, has_chosen_handle) VALUES
  (1, 'Maya Chen', '@mayabeats', 'Chronic playlist-maker.', NULL, NULL, true),
  (2, 'Jordan Reyes', '@jordanreyes', 'Dream pop enjoyer.', NULL, NULL, true),
  (3, 'Alex Kim', '@alexkim', 'Shoegaze all day.', NULL, NULL, true),
  (4, 'Sam Osei', '@samo', 'Vinyl collector.', NULL, NULL, true),
  (5, 'Priya Nair', '@priyaspins', 'Bedroom pop girlie.', NULL, NULL, true),
  (6, 'Jordan Reyes', '@jreyes_music', 'Different Jordan, same great taste.', NULL, NULL, true),
  (7, 'Riley Chen', '@rileyc', 'Maya''s cousin, also into indie.', NULL, NULL, true),
  (8, 'Devon Marsh', '@devonm', 'Post-punk revivalist.', NULL, NULL, true);

-- Tracks
INSERT INTO track (id, title, cover_url, artist_id) VALUES
  (1, 'Kyoto', NULL, 1),
  (2, 'Redbone', NULL, 2),
  (3, 'Motion Sickness', NULL, 1),
  (4, 'Electric Feel', NULL, 3);

-- Maya is currently listening to Kyoto
UPDATE user SET now_playing_track_id = 1 WHERE id = 1;

-- Playlists
INSERT INTO playlist (id, title, cover_url, owner_id) VALUES
  (1, 'Late Night Drive', NULL, 1);

INSERT INTO playlist_track (playlist_id, track_id, position) VALUES
  (1, 1, 1),
  (1, 2, 2),
  (1, 3, 3);

-- Maya's top tracks
INSERT INTO user_top_track (user_id, track_id, `rank`, play_count) VALUES
  (1, 2, 1, 342),
  (1, 1, 2, 290),
  (1, 3, 3, 150);

-- Maya's top artists
INSERT INTO user_top_artist (user_id, artist_id, `rank`) VALUES
  (1, 1, 1),
  (1, 2, 2);

-- Jordan and Alex's top artists, overlapping with Maya's so Compatibility
-- has real shared artists to show for the two people Maya actually follows.
INSERT INTO user_top_artist (user_id, artist_id, `rank`) VALUES
  (2, 1, 1),
  (2, 3, 2),
  (3, 1, 2);

-- Maya's top genres
INSERT INTO user_top_genre (user_id, genre_name, percentage) VALUES
  (1, 'Indie Pop', 34),
  (1, 'Dream Pop', 22);

-- Jordan's top genres, overlapping with Maya's on both Dream Pop and Indie Pop
INSERT INTO user_top_genre (user_id, genre_name, percentage) VALUES
  (2, 'Dream Pop', 30),
  (2, 'Indie Pop', 20);

-- Top genres for the other search-test users, so search results show a topGenre chip
-- (Alex also gets a smaller Indie Pop slice so she shares some taste with Maya too)
INSERT INTO user_top_genre (user_id, genre_name, percentage) VALUES
  (3, 'Shoegaze', 41),
  (3, 'Indie Pop', 15),
  (4, 'Classic Rock', 30),
  (5, 'Bedroom Pop', 38),
  (6, 'Dream Pop', 27),
  (7, 'Indie Rock', 33),
  (8, 'Post-Punk', 45);

-- Follows: Jordan follows Maya, Maya follows Jordan, Maya also follows Alex
INSERT INTO follow (follower_id, followee_id, created_at) VALUES
  (2, 1, '2026-08-01 00:00:00'),
  (1, 2, '2026-08-02 00:00:00'),
  (1, 3, '2026-08-03 00:00:00');

-- Feed events: Maya follows Jordan and Alex, so Maya's feed should show
-- their activity. Actor is whoever performed the action.
INSERT INTO feed_event (actor_id, type, verb, target, subtitle, created_at) VALUES
  (2, 'FOLLOWED', 'started following', 'Maya Chen', NULL, '2026-08-01 00:00:00'),
  (3, 'COMMENTED', 'commented on', 'Maya Chen''s profile', 'Your top tracks are so good this month.', '2026-08-03 12:00:00');
