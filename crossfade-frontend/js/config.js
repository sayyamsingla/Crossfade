// ---------------------------------------------------------------------------
// Crossfade frontend config
// ---------------------------------------------------------------------------
// Change API_BASE if your Spring Boot app runs on a different host/port.
// The frontend itself is served on http://localhost:5173 (see package.json).
// Your backend MUST allow that origin via CORS — see README.md.
// ---------------------------------------------------------------------------

window.CROSSFADE_CONFIG = {
  API_BASE: 'http://127.0.0.1:8080',
  API_PREFIX: 'http://127.0.0.1:8080/api',

  // Real Spotify login now. CURRENT_USER_ID starts null and is filled in by
  // Api.getMe() (GET /api/me, resolved from the session cookie) once the app
  // loads — see js/app.js's init flow.
  CURRENT_USER_ID: null,

  // TEMP: set to false (or delete js/mock-data.js and its <script> tag in
  // index.html) once the real backend is ready — this flag is the only
  // thing wiring the fake data in.
  USE_MOCK_DATA: false,
};
