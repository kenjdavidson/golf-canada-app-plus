# Backend API Overview

This Micronaut backend provides the authenticated application layer for the Golf Canada app. It handles local session management, JWT cookie authentication, and calls into the external Golf Canada APIs generated from `backend/src/main/openapi/golf-canada-api.yaml`.

## Authentication and session flow

The app uses Micronaut Security with cookie-based JWT authentication.

### Required runtime configuration

The backend will not start without these environment variables:

- `JWT_SIGNING_SECRET`
- `GOLF_CANADA_TOKEN_ENCRYPTION_KEY`

Optional database overrides:

- `GOLF_CANADA_PRIMARY_DB_PATH` (default: `./golfapp.db`)
- `GOLF_CANADA_SESSION_DB_PATH` (default: `./sessions.db`)

### Local app endpoints

| Method | Path | Auth | Purpose |
|---|---|---:|---|
| `POST` | `/api/login` | No | Authenticates the user against Golf Canada and issues a JWT cookie. Accepts JSON like `{ "username": "user@example.com", "password": "secret" }`. Add `?rememberMe=true` to enable proactive token refresh. |
| `POST` | `/api/logout` | Yes | Clears the JWT cookie and ends the local session. |

All routes under `/api/**` require an authenticated JWT cookie, except `/api/login`.

### Example login request

```http
POST /api/login?rememberMe=true
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "secret"
}
```

Successful authentication sets a 30-day JWT cookie. Failed login attempts return an auth failure result from the provider.

## External Golf Canada API client surface

The backend generates a typed client from the OpenAPI spec at `backend/src/main/openapi/golf-canada-api.yaml` and exposes it via Micronaut injection.

### Authentication API

| Client | Method | Endpoint | Notes |
|---|---|---|---|
| `AuthenticationApi` | `authenticate` | `POST /connect/token` | Exchanges username/password or refresh token for a Golf Canada access token. |

### Members API

| Client | Method | Endpoint | Required query params | Notes |
|---|---|---|---|---|
| `MembersApi` | `getProfile` | `GET /api/scores/getProfile` | `individualId` | Returns member profile data for a given individual. |
| `MembersApi` | `searchMembers` | `GET /api/members/search` | Optional `$skip`, `$top`, `text` | Searches for members by name / query text. |
| `MembersApi` | `getHistory` | `GET /api/scores/getHistory` | `$skip`, `$top`, `individualId` | Retrieves historical handicap / score data. |

### Scores API

| Client | Method | Endpoint | Required query params | Notes |
|---|---|---|---|---|
| `ScoresApi` | `getScoreDetails` | `GET /api/scores/getScoreDetails` | `scoreId` | Returns detailed score information for a specific score. |

## Feature level

Current backend functionality is focused on:

- user sign-in / sign-out with secure, cookie-backed JWT sessions
- encrypted storage of Golf Canada tokens in SQLite
- session refresh support for long-lived remember-me sessions
- member profile lookup, member search, handicap history, and score details via the Golf Canada API client

This app does not currently expose additional backend write endpoints for course/hole management or score submission beyond the implemented Golf Canada integration layer.

## Related files

- `backend/src/main/resources/application.yml` — security and datasource configuration
- `backend/src/main/openapi/golf-canada-api.yaml` — generated external API contract
- `backend/src/main/kotlin/com/kenjdavidson/golfcanada/api/GolfCanadaApiFactory.kt` — client registration
- `backend/src/main/kotlin/com/kenjdavidson/golfcanada/security/` — auth provider, token storage, refresh logic
