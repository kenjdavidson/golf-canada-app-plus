# golf-canada-app-plus
Golf Canada App mini-app provides enhanced game modes, data tracking and follower notifications

## Backend SSL certificate

The Micronaut backend ships with `backend/src/main/resources/ssl/golfcanada.pem` and loads it into the default JVM trust chain during application startup.

## Golf Canada OpenAPI client

Golf Canada client APIs are generated at build time from `backend/src/main/openapi/golf-canada-api.yaml` using OpenAPI Generator.

- Generation task: `./gradlew :backend:generateGolfCanadaClient`
- Generation is wired into compilation so generated sources are available on every build
- Generated APIs are registered for Micronaut injection via `GolfCanadaApiFactory`
- Base URL is configurable with `golf-canada.api.base-url` (defaults to `https://scg.golfcanada.ca`)

Currently available generated API groups and operations:

- `AuthenticationApi`
  - `authenticate` (`POST /connect/token`)
- `MembersApi`
  - `getProfile` (`GET /api/scores/getProfile`)
  - `searchMembers` (`GET /api/members/search`)
  - `getHistory` (`GET /api/scores/getHistory`)
- `ScoresApi`
  - `getScoreDetails` (`GET /api/scores/getScoreDetails`)
