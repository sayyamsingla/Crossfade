-- Clear children before parents so FK constraints don't block re-runs
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
INSERT INTO user (id, display_name, handle, bio, avatar_url, now_playing_track_id) VALUES
  (1, 'Maya Chen', '@mayabeats', 'Chronic playlist-maker.', NULL, NULL),
  (2, 'Jordan Reyes', '@jordanreyes', 'Dream pop enjoyer.', NULL, NULL);

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

-- Maya's top genres
INSERT INTO user_top_genre (user_id, genre_name, percentage) VALUES
  (1, 'Indie Pop', 34),
  (1, 'Dream Pop', 22);

-- Follows: Jordan follows Maya, Maya follows Jordan
INSERT INTO follow (follower_id, followee_id, created_at) VALUES
  (2, 1, '2026-08-01 00:00:00'),
  (1, 2, '2026-08-02 00:00:00');
