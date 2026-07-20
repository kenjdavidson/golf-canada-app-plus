# golf-canada-app-plus
Golf Canada App mini-app provides enhanced game modes, data tracking and follower notifications

## GitHub Codespaces / Dev Container

This repository includes a dev container configuration (`.devcontainer/devcontainer.json`) so you can develop in GitHub Codespaces or any compatible dev container environment.

The container is based on the Java 21 devcontainer image and installs Node.js LTS automatically. It comes pre-configured with VS Code extensions for Java, Kotlin, Gradle, TypeScript, and React (Prettier + ESLint).

### Required secrets / environment variables

The backend **will not start** without `JWT_SIGNING_SECRET` and `GOLF_CANADA_TOKEN_ENCRYPTION_KEY`. How you supply them depends on where you run the dev container:

**GitHub Codespaces** — add them as [Codespaces secrets](https://github.com/settings/codespaces) in your GitHub account (or at the repository level). Codespaces automatically injects repository and user secrets as environment variables inside the container; no extra configuration is required.

**Local dev container (Docker Desktop / VS Code Remote - Containers)** — the devcontainer configuration forwards these variables from your local machine's environment via the `remoteEnv` section. Export them in your shell before opening the container:

```bash
export JWT_SIGNING_SECRET="your-long-random-signing-secret"
export GOLF_CANADA_TOKEN_ENCRYPTION_KEY="your-long-random-encryption-key"
```

These correspond to the [required environment variables](#required-environment-variables) described in the Backend Security section below.

### Running in the dev container

Once the container is ready:

```bash
# Install frontend dependencies (run automatically via postCreateCommand, but re-run if needed)
npm --prefix frontend install

# Start the frontend dev server (port 5173 is forwarded automatically)
npm --prefix frontend run dev

# Start the backend (port 8080 is forwarded automatically)
./gradlew :backend:run
```

## Backend SSL certificate

The Micronaut backend ships with `backend/src/main/resources/ssl/golfcanada.pem` and loads it into the default JVM trust chain during application startup.

## Backend Security

The backend uses Micronaut Security with cookie-based JWT authentication backed by the Golf Canada authentication API.

### Required environment variables

The application **will not start** without the following environment variables:

| Variable | Description |
|---|---|
| `JWT_SIGNING_SECRET` | HS256 signing secret for Micronaut JWT cookies. Use a long random string (≥ 32 characters). |
| `GOLF_CANADA_TOKEN_ENCRYPTION_KEY` | Passphrase used to derive an AES-256 key for encrypting Golf Canada tokens at rest. Use a long random string. |

### Optional environment variables

| Variable | Default | Description |
|---|---|---|
| `GOLF_CANADA_PRIMARY_DB_PATH` | `./golfapp.db` | File path only (not a full JDBC URL) for the primary SQLite application database. |
| `GOLF_CANADA_SESSION_DB_PATH` | `./sessions.db` | File path only (not a full JDBC URL) for the SQLite database that stores encrypted user sessions. |

The backend uses two SQLite databases:

- `golfapp.db` (or `GOLF_CANADA_PRIMARY_DB_PATH`) for the primary application schema
- `sessions.db` (or `GOLF_CANADA_SESSION_DB_PATH`) for encrypted Golf Canada session tokens

### Login

`POST /api/login` — accepts a JSON body `{"username": "...", "password": "..."}`.

Append `?rememberMe=true` to enable proactive Golf Canada access-token refresh:

```
POST /api/login?rememberMe=true
Content-Type: application/json

{"username": "user@example.com", "password": "secret"}
```

On success a JWT cookie is set (valid for 30 days). On failure the response includes an authentication failure reason.

### Logout

`POST /api/logout` — clears the JWT cookie.

### API security

All routes under `/api/**` (except `/api/login`) require an authenticated JWT cookie.  Static frontend assets served at `/**` are public.

### Token management

Golf Canada access tokens are stored encrypted (AES-256-GCM) in a local SQLite database.  When a token is near expiry and `rememberMe` is enabled, `GolfCanadaTokenManager` silently refreshes it using the stored refresh token. A `Mutex` ensures only one refresh runs at a time to prevent the OAuth race condition where two concurrent requests both try to redeem an already-invalidated refresh token.

If the Golf Canada refresh token itself expires or is revoked, the local session is cleared and the browser is redirected to `/login` with an expired cookie header.

## Golf Canada OpenAPI client

Golf Canada client APIs are generated at build time from `backend/src/main/openapi/golf-canada-api.yaml` using OpenAPI Generator.

- Generation task: `./gradlew :backend:generateGolfCanadaClient`
- Generation is wired into compilation so generated sources are available on every build
- Generated APIs are registered for Micronaut injection via `GolfCanadaApiFactory`
- Base URL is configurable with `golf-canada.api.base-url` (defaults to `https://scg.golfcanada.ca`)

Currently available generated API groups and operations:

- `AuthenticationApi`
  - `authenticate` (`POST /connect/token`) — supports `grant_type=password` and `grant_type=refresh_token`
- `MembersApi`
  - `getProfile` (`GET /api/scores/getProfile`)
  - `searchMembers` (`GET /api/members/search`)
  - `getHistory` (`GET /api/scores/getHistory`)
- `ScoresApi`
  - `getScoreDetails` (`GET /api/scores/getScoreDetails`)

## Releases

Releases are created manually from GitHub Actions using the `Release` workflow (`.github/workflows/release.yml`).

- Trigger it from **Actions → Release → Run workflow**
- Select a source branch (`main` is the default)
- Leave `tag` blank to use `v<backend Gradle version>` automatically, or provide a tag override (e.g., `v1.0.0`)

The workflow builds backend/frontend release artifacts and creates a GitHub release with autogenerated release notes and attached downloadable artifacts.
