# Crossfade

Crossfade is a social app for people who care about music. You connect your
Spotify account, and the app pulls your top tracks, top artists, and top
genres to build a profile of your taste. From there you can follow other
users, see a compatibility score that measures how closely your music taste
overlaps with theirs, comment on and like each other's playlists, and see a
feed of what the people you follow are doing.

The idea behind the project was to build something that actually needed a
real backend, not just a CRUD app. Compatibility scoring needed an actual
algorithm instead of a simple lookup. Syncing Spotify data needed retry
handling and rate limiting because Spotify's API can reject requests.
Repeated profile lookups needed caching so the database wasn't hit on every
request. A feed of follow, comment, and like events needed something that
could handle events without blocking the request that triggered them. Those
requirements are what shaped the backend design below.

## Why this exists

Most portfolio projects are simple enough that a single service and a single
database table can handle everything. This project was built specifically to
avoid that. Every piece of infrastructure here (Redis, Kafka, OAuth2, a
custom caching layer) was added because a real requirement needed it, not
because it looks good on a resume. The sections below explain what each
piece is doing and why it's there.

## Tech stack

- **Backend:** Java, Spring Boot, Spring Data JPA, Spring Security
- **Database:** MySQL
- **Caching:** Caffeine (in memory) and Redis (shared, persists across
  restarts)
- **Messaging:** Kafka
- **Auth:** Spotify OAuth2
- **Frontend:** Plain HTML, CSS, and JavaScript with no framework or build
  step

## Architecture overview

### Data model

The schema is built with Spring Data JPA across 20 entities. Most
many to many relationships (playlist tracks, a user's top artists, a user's
top tracks) are modeled as their own entities with composite keys instead of
plain join tables, because each of those relationships carries its own data
like rank or track position, not just a link between two rows.

A few relationships needed two separate foreign keys to the same entity
instead of one. A follow has a follower and a followee, and those are two
different users playing two different roles in the same row. A comment has
an author and a recipient for the same reason. Modeling these as a single
ambiguous relationship would have made the two roles impossible to tell
apart.

Fields that can be computed from existing data, like a user's follower
count or a playlist's track count, are not stored as columns. They're
computed at query time so there's no risk of that number drifting out of
sync with the rows it's counting.

### Authentication

Login happens through Spotify OAuth2, handled by Spring Security. On a
user's first login, the app creates a local user record and links it to
their Spotify account, so every user in the system maps back to a real
Spotify identity. Access and refresh tokens are stored in MySQL instead of
in memory, so a user doesn't have to log in again if the server restarts.

### Caching

Profile lookups, top tracks, top artists, and playlists are all read far
more often than they're written, so those endpoints are cached. Caching
runs in two tiers. The first tier is Caffeine, an in memory cache that's
extremely fast but disappears if the server restarts. The second tier is
Redis, which is slower than in memory access but holds onto data across
restarts. A read checks Caffeine first, falls back to Redis on a miss, and
if Redis has the value, it gets written back into Caffeine so the next read
is fast again. This means a server restart doesn't force every user's data
to be rebuilt from the database from scratch.

### Compatibility scoring

The compatibility score between two users is not a static value pulled from
a table. It's calculated by comparing each user's top 20 artists and top 20
tracks, weighting shared entries by how highly each user ranks them, and
combining the artist and track overlap into a single score from 0 to 100.
Two users who both rank the same artist near the top of their list score
higher than two users who share an artist that's low on both lists.

### Spotify sync

A background sync pipeline pulls a user's profile, top tracks, top artists,
and playlists from the Spotify Web API. Spotify's API returns a rate limit
error if too many requests come in too quickly, so the sync includes retry
logic that reads the response headers Spotify sends back and waits the
correct amount of time before trying again instead of failing outright.

### Feed and Kafka

Following someone, commenting, and liking a playlist all publish an event
to Kafka instead of writing directly to the feed table in the same request.
A separate consumer picks up those events and writes them to the feed
table. This keeps the action a user actually triggered, like following
someone, fast and free of any dependency on feed processing. If feed
writing were slow or failed, it wouldn't block or fail the follow itself.

## API reference

All endpoints are prefixed with `/api`. Endpoints marked with a lock
require the user to be logged in.

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

The frontend is a plain HTML, CSS, and JavaScript app with no framework and
no build step. It calls the endpoints listed above directly and renders the
responses. There's no separate frontend architecture to document since it's
just static files talking to the API.
