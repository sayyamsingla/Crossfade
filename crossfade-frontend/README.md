# Crossfade — frontend

Plain HTML/CSS/JS frontend for Crossfade. No framework, no build step. It expects
a backend (you're building it in Spring Boot) running at `http://localhost:8080`
and talks to it entirely over `fetch()` — every piece of data on screen, including
images, comes from the API described below. There is no mock data left in this
project; if the backend isn't running you'll see a "can't reach backend" banner
instead of a broken page.

## Running it

```bash
npm install
npm start        # serves on http://localhost:5173
# or: npm run dev   (same, but opens your browser automatically)
```

It's a static file server (`http-server`) serving `index.html` as-is — nothing to
build or compile.

## Before anything will render: CORS

The frontend runs on `http://localhost:5173`, the backend on `http://localhost:8080`
— different origins, so the backend must explicitly allow the frontend's origin or
every request will fail in the browser (even if it works fine in Postman/curl).
In Spring Boot, the simplest global setup:

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:5173")
                        .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
```

## Current user

There's no login flow in this demo — every request acts as a fixed user id,
set in `js/config.js` (`CURRENT_USER_ID`, defaults to `1`). Seed a user with that
id in your database and everything else (top tracks, playlists, comments, etc.)
should hang off of it.

## Viewing other people's profiles

`CURRENT_USER_ID` is only who you're *acting as* (who posts comments, whose feed
loads, whose friends list is shown). Every profile endpoint (`/users/{id}`,
`/users/{id}/top-tracks`, `/playlists`, `/comments`, etc.) takes whatever id is
being *viewed*, which can be anyone — the Friends tab, search results, comment
authors, and feed actors are all clickable and load that person's profile using
the same endpoints with their id instead. Your backend doesn't need to know the
difference; it's purely a frontend concept.

## Images

Every image-bearing field (`avatarUrl`, `imageUrl`, `coverUrl`) is optional.
Return either an absolute URL (`"https://cdn.example.com/x.jpg"`) or a path
rooted at your backend (`"/images/artists/12.jpg"`, resolved against
`http://localhost:8080`). If the field is `null`/missing, or the image fails to
load, the frontend falls back to a colored circle with the name's initials — so
you can build and test the whole app before wiring up real image storage.

---

## API contract

Base path: `http://localhost:8080/api`. All bodies are JSON.

### `GET /users/{id}`
Profile header.
```json
{
  "id": 1,
  "displayName": "Maya Chen",
  "handle": "@mayabeats",
  "bio": "Vinyl collector. Bass-heavy playlists.",
  "avatarUrl": null,
  "followersCount": 1204,
  "followingCount": 389,
  "topGenre": "Indie Pop"
}
```

### `GET /users/{id}/now-playing`
```json
{ "playing": true, "track": { "title": "Kyoto", "artist": "Phoebe Bridgers", "coverUrl": null } }
```
or `{ "playing": false }` — the frontend hides the "listening now" strip entirely in that case.

### `GET /users/{id}/top-tracks?range=4weeks&limit=5`
```json
[
  { "rank": 1, "trackId": 1, "title": "Redbone", "artist": "Childish Gambino", "coverUrl": null, "playCount": 342 }
]
```

### `GET /users/{id}/top-artists?range=4weeks&limit=6`
```json
[ { "rank": 1, "artistId": 1, "name": "Phoebe Bridgers", "imageUrl": null } ]
```

### `GET /users/{id}/top-genres?limit=5`
```json
[ { "name": "Indie Pop", "percentage": 34 } ]
```
Return sorted highest-to-lowest — the frontend renders them in the order given.

### `GET /users/{id}/playlists`
```json
[ { "playlistId": 1, "title": "Late Night Drive", "trackCount": 42, "coverUrl": null } ]
```

### `GET /users/{id}/comments?viewerId={id}`
Comments on this user's profile wall. There's no auth, so `viewerId` (who's
asking — always `CURRENT_USER_ID` from the frontend) is passed explicitly as a
query param so the backend can compute `likedByCurrentUser` per comment.
`viewerId` is optional; omit it and every comment comes back with
`likedByCurrentUser: false`.
```json
[
  {
    "commentId": 1,
    "author": { "userId": 2, "displayName": "Jordan Reyes", "avatarUrl": null },
    "text": "Your top tracks this month are basically my whole personality, how",
    "createdAt": "2026-08-20T05:00:00Z",
    "likeCount": 6,
    "likedByCurrentUser": false
  }
]
```
`createdAt` must be an ISO-8601 timestamp — the frontend computes "2h", "3d" etc. itself.

### `POST /users/{id}/comments`
Body: `{ "authorId": 1, "text": "..." }` → returns `{ "authorId": 1, "text": "..." }`.

### `POST /comments/{commentId}/like?userId={id}` and `DELETE /comments/{commentId}/like?userId={id}`
Like / unlike. Same no-auth reasoning as above — `userId` (always
`CURRENT_USER_ID`) says who's doing the liking. Both return:
```json
{ "likeCount": 7, "likedByCurrentUser": true }
```

### `GET /users/{id}/feed`
Activity from people the user follows.
```json
[
  {
    "feedId": 1,
    "type": "TRACK_ADDED",
    "actor": { "userId": 2, "displayName": "Jordan Reyes", "avatarUrl": null },
    "verb": "added 3 tracks to",
    "target": "Late Night Drive",
    "subtitle": "Late Night Drive · 42 tracks",
    "createdAt": "2026-08-20T07:48:00Z"
  }
]
```
The frontend renders `"<b>{actor.displayName}</b> {verb} <b>{target}</b>"` plus `subtitle` underneath —
so keep `verb`/`target` as plain fragments, not pre-formatted HTML.

`type` picks the icon shown and must be one of:
`TRACK_ADDED`, `FOLLOWED`, `COMMENTED`, `TOP_TRACK`, `LIKED_PLAYLIST`, `NOW_LISTENING`.

### `GET /users/{id}/following`
The people this user follows — powers both the **Friends tab** and the
**Compatibility picker** (which only offers friends by default).
```json
[ { "userId": 2, "displayName": "Jordan Reyes", "avatarUrl": null, "topGenre": "Dream Pop" } ]
```
`topGenre` is optional — shown as a small tag under the name on the friend card if present.

### `GET /users/search?q={query}`
Backs the search box on the Friends tab — find any user by (partial, case-insensitive)
display name, not just friends. Same response shape as `/following` above. The
frontend excludes the current user from the results itself, but excluding them
server-side too is fine.
```json
[ { "userId": 5, "displayName": "Alex Kim", "avatarUrl": null, "topGenre": "Shoegaze" } ]
```

### `GET /compatibility?userId={id}&withUserId={id2}`
```json
{
  "score": 74,
  "caption": "You and Jordan share a lot of the same lane — mostly overlapping in indie pop and dream pop.",
  "sharedArtists": ["Phoebe Bridgers", "MGMT", "Beach House"],
  "sharedGenres": ["Indie Pop", "Bedroom Pop", "Dream Pop"]
}
```

---

## Project layout

```
index.html          markup + section containers the JS renders into
css/style.css        all styling
js/config.js         API base URL + current user id — edit this first
js/api.js             fetch wrapper + one function per endpoint above
js/utils.js           HTML escaping, relative time, image-with-fallback rendering
js/app.js              rendering + event wiring (tabs, theme toggle, comment form, likes)
```

Every network call goes through `js/api.js` — if you rename or restructure an
endpoint on the backend, that's the only file that needs to change.
