// ---------------------------------------------------------------------------
// Crossfade frontend config
// ---------------------------------------------------------------------------
// Change API_BASE if your Spring Boot app runs on a different host/port.
// The frontend itself is served on http://localhost:5173 (see package.json).
// Your backend MUST allow that origin via CORS — see README.md.
// ---------------------------------------------------------------------------

window.CROSSFADE_CONFIG = {
  API_BASE: 'http://localhost:8080',
  API_PREFIX: 'http://localhost:8080/api',

  // No auth/login in this demo — every request acts as this user id.
  // Your backend should have a user with this id seeded for the demo to work.
  CURRENT_USER_ID: 1,

  // TEMP: set to false (or delete js/mock-data.js and its <script> tag in
  // index.html) once the real backend is ready — this flag is the only
  // thing wiring the fake data in.
  USE_MOCK_DATA: false,
};
