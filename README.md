# Crossfade

Crossfade is a social app for people who care about music. You log in with Spotify, and it pulls your top tracks, top artists, and top genres to build a profile of your taste. From there you can follow other users, get a compatibility score against them, comment on and like each other's playlists, and check a feed of what the people you follow are doing.

## Demo

https://github.com/user-attachments/assets/54990cdf-54cc-4a60-957c-29da1bc8f27f

This isn't deployed anywhere public. Spotify's API caps a development-mode app at 5 allowlisted users, so wider access needs Spotify to approve an extended quota request first.

## Tech stack

- Backend: Java, Spring Boot, Spring Data JPA, Spring Security
- Database: MySQL
- Caching: Caffeine (in memory) and Redis (shared, survives restarts)
- Messaging: Kafka
- Auth: Spotify OAuth2
- Frontend: plain HTML, CSS, and JavaScript, no framework, no build step

## Running it locally

Needs Java 25 plus MySQL, Redis, and Kafka. `docker-compose.yml` at the repo root brings up all three:

```bash
DB_PASSWORD=yourpassword docker compose up -d
```
This starts MySQL on `3306` with a `crossfade` database already created, Redis on `6379`, and a single-node Kafka broker (KRaft mode, no Zookeeper) on `9092`.

Environment variables the backend reads:

- `DB_USERNAME` (defaults to `root`)
- `DB_PASSWORD` (required, no default)
- `SPOTIFY_CLIENT_ID` / `SPOTIFY_CLIENT_SECRET`, from a Spotify developer app with `http://127.0.0.1:8080/login/oauth2/code/spotify` registered as a redirect URI
- `FRONTEND_BASE_URL` (defaults to `http://127.0.0.1:5173`)

```bash
cd crossfade-backend
./mvnw spring-boot:run
```
Runs on `http://localhost:8080` against a MySQL database named `crossfade`, a Redis instance on `localhost:6379`, and Kafka on `localhost:9092`. The schema updates itself on startup (`ddl-auto=update`), and `data.sql` reseeds a fixed set of test users every time the app restarts.

```bash
cd crossfade-frontend
npm install
npm start
```
Runs on `http://localhost:5173`. The backend's CORS config only allows that origin, so serving the frontend from anywhere else will fail silently in the browser even though curl works fine.

## Architecture overview

### Data model

The schema runs across 20 JPA entities. Seven of them are junction tables built with `@IdClass` instead of a generated id: `Follow` (follower id plus followee id), `PlaylistTrack` (playlist plus track plus position), `UserTopArtist` and `UserTopTrack` (user plus artist or track plus rank), `UserTopGenre` (user plus genre name plus percentage), and `CommentLike`/`PlaylistLike` (comment or playlist plus user). Rank and position needed somewhere to live, so those relationships became entities instead of plain join tables.

`Follow` and `Comment` each carry two foreign keys to `User` instead of one. A follow has a follower and a followee, and a comment has an author and a recipient, and those are two different roles on the same row. Collapsing that into a single relationship would make it impossible to tell which user is which.

Derived fields like a user's follower count, following count, and a playlist's track count aren't columns. `ProfileService` computes them at read time so they can't drift out of sync with the rows they're counting.

### Authentication

Login goes through Spotify OAuth2 via Spring Security. `SpotifyOAuth2UserService` handles provisioning: on first login it looks up the Spotify user id, and if there's no match, it creates a local `User` row right there. `CrossfadeOAuth2User` wraps the raw OAuth2 user object and carries the local user id alongside it, so the rest of the app can go from a Spotify identity to a local one without an extra lookup.

Tokens don't sit in Spring's default in-memory client service. `OAuth2ClientConfig` swaps in `JdbcOAuth2AuthorizedClientService`, which persists access and refresh tokens in MySQL, so a server restart doesn't force everyone to log back in.

### Caching

`userProfile`, `topTracks`, `topArtists`, `topGenres`, `playlists`, and `playlistTracks` all go through a two-tier cache manager, `TwoLevelCacheManager`. Caffeine sits in front as L1: in memory, fast, gone on restart. Redis sits behind it as L2, serialized with `GenericJacksonJsonRedisSerializer` with default typing turned on, so a read comes back as the actual DTO instead of a `LinkedHashMap`. A lookup checks L1 first, falls back to L2 on a miss, and writes that value back into L1 so the next read is fast again. Both tiers share a 10 hour TTL, and evictions go through both, so a cache invalidation from a sync or a follow can't leave a stale copy sitting in Redis after Caffeine's copy is already gone.

### Compatibility scoring

`CompatibilityService` compares each user's top 20 artists and top 20 tracks with a rank-weighted Ruzicka similarity. A shared item's weight is `MAX_COMPARE_ITEMS` minus its rank plus one, so an artist both people rank near the top counts for a lot more than one buried near the bottom. Artist similarity and track similarity are computed separately, then combined as `artistSim * 0.6 + trackSim * 0.4` and rounded into a score from 0 to 100.

### Spotify sync

`SpotifySyncService` pulls a user's profile, top 50 artists, top genres (derived by counting genre tags across those artists), top 50 tracks, and up to 100 tracks per playlist, all inside one `@Transactional` method. `SpotifyApiClient` handles the retries: on a 429 it reads the `Retry-After` header, clamps it between 1 and 5 seconds, and retries up to twice. Syncs are also capped at 3 per user per 7 day sliding window, tracked through `SyncLogRepository`, so a user can't hammer the endpoint and eat into Spotify's rate limit.

### Feed and Kafka

Follows, comments, and playlist likes get published to a Kafka topic, `feed-events`, instead of writing to the feed table inline. `KafkaFeedProducer` publishes three event types, `FOLLOWED`, `LIKED_PLAYLIST`, and `COMMENTED`, keyed by the actor's user id so one user's events stay in order within a partition. The topic has 3 partitions, and `KafkaFeedConsumer` runs with concurrency 3 to match it, one listener thread per partition. `FeedService` then reads back a merged feed with a single `IN` query across all followee ids instead of one query per followee.

## API reference

All endpoints are prefixed with `/api` and return JSON. Endpoints marked with a lock require the user to be logged in via the Spotify OAuth2 session cookie, not a bearer token, since there's no separate API auth layer.

### Profile

| Method | Endpoint | Description |
|---|---|---|
| GET | `/me` | Get the logged in user's own profile (locked) |
| GET | `/users/{id}` | Get a user's profile by id |
| POST | `/me/handle` | Set the logged in user's handle (locked) |
| GET | `/users/{id}/now-playing` | Get the track a user is currently listening to |
| GET | `/users/{id}/top-tracks?range=&limit=` | Get a user's top tracks |
| GET | `/users/{id}/top-artists?range=&limit=` | Get a user's top artists |
| GET | `/users/{id}/top-genres?limit=` | Get a user's top genres |
| GET | `/users/{id}/playlists?viewerId=` | Get a user's playlists |
| GET | `/playlists/{playlistId}/tracks` | Get the tracks on a playlist |

### Comments and likes

| Method | Endpoint | Description |
|---|---|---|
| POST | `/users/{id}/comments` | Add a comment to a user's profile (locked) |
| GET | `/users/{id}/comments?viewerId=` | Get the comments on a user's profile |
| POST | `/comments/{commentId}/like` | Like a comment (locked) |
| DELETE | `/comments/{commentId}/like` | Unlike a comment (locked) |
| POST | `/playlists/{playlistId}/like` | Like a playlist (locked) |
| DELETE | `/playlists/{playlistId}/like` | Unlike a playlist (locked) |

### Friends and search

| Method | Endpoint | Description |
|---|---|---|
| GET | `/users/{id}/following` | Get who a user follows |
| GET | `/users/search?q=&viewerId=` | Search for users |
| POST | `/users/{id}/follow` | Follow a user (locked) |
| DELETE | `/users/{id}/follow` | Unfollow a user (locked) |

### Feed

| Method | Endpoint | Description |
|---|---|---|
| GET | `/users/{id}/feed` | Get the activity feed for a user's followees |

### Compatibility

| Method | Endpoint | Description |
|---|---|---|
| GET | `/compatibility?userId=&withUserId=` | Get the compatibility score between two users |

### Spotify sync

| Method | Endpoint | Description |
|---|---|---|
| POST | `/sync/spotify` | Trigger a fresh Spotify data sync (locked) |
| GET | `/sync/spotify/status` | Get the status of the last sync (locked) |

## Frontend

The frontend is plain HTML, CSS, and JavaScript, no framework, no build step. It calls the endpoints above directly and renders the responses.
