# API Quickstart (Current Skeleton)

Base URL: `http://localhost:8080`

## 0) Local infrastructure stack
Start local dependencies:
```bash
docker compose up -d
```

Stop stack:
```bash
docker compose down
```

Services:
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`

Start monitoring stack:
```bash
docker compose -f docker-compose.monitoring.yml up -d
```

If monitoring stack is already running and you changed provisioning files:
```bash
docker compose -f docker-compose.monitoring.yml down
docker compose -f docker-compose.monitoring.yml up -d
```

Monitoring endpoints:
- Prometheus UI: `http://localhost:9090`
- Grafana UI: `http://localhost:3000` (`admin` / `admin`)
- Alertmanager UI: `http://localhost:9093`
- Webhook catcher API: `http://localhost:5001`
- Auto-provisioned dashboard folder: `SuperApp`
- Starter dashboard: `SuperApp BE Overview`
- Alert rules loaded from: `ops/prometheus/rules/superapp-alerts.yml`
- Alertmanager config: `ops/alertmanager/alertmanager.yml`

Stop monitoring stack:
```bash
docker compose -f docker-compose.monitoring.yml down
```

Run BE against Postgres + Redis (PowerShell example):
```powershell
cd BE
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/superapp"
$env:SPRING_DATASOURCE_USERNAME="superapp"
$env:SPRING_DATASOURCE_PASSWORD="superapp"
$env:SPRING_JPA_HIBERNATE_DDL_AUTO="update"
$env:SPRING_REDIS_HOST="localhost"
$env:SPRING_REDIS_PORT="6379"
$env:APP_AUTH_MFA_CHALLENGE_STORE="redis"
.\mvnw.cmd spring-boot:run
```

Prometheus scrape endpoint:
- `http://localhost:8080/actuator/prometheus`

Quick alert verification:
1. Open `http://localhost:9090/alerts` to see active/pending alerts.
2. Open `http://localhost:9090/rules` to inspect loaded rule groups.
3. Stop backend for >2 minutes to trigger `SuperAppBackendDown`.
4. Open `http://localhost:9093/#/alerts` to see routed alerts at Alertmanager.

Webhook notes:
- Default receiver endpoint (container-internal): `http://webhook-catcher:5001/alerts/default`
- Critical receiver endpoint (container-internal): `http://webhook-catcher:5001/alerts/critical`
- Check received alert payloads:
  - `http://localhost:5001/alerts/recent`
- Check notifier status:
  - `http://localhost:5001/alerts/notifier`
- Check failed deliveries (dead-letter):
  - `http://localhost:5001/alerts/dead-letter`
  - persisted file view: `http://localhost:5001/alerts/dead-letter/file`
  - replay memory: `POST http://localhost:5001/alerts/dead-letter/replay?source=memory&limit=10`
  - replay file: `POST http://localhost:5001/alerts/dead-letter/replay?source=file&limit=10&markSuccess=true`
  - compact file: `POST http://localhost:5001/alerts/dead-letter/compact?removeProcessed=true&keepLast=1000&maxAgeHours=168`

Notifier adapter modes (webhook catcher):
- `ALERT_NOTIFIER_MODE=log` (default dev mode)
- `ALERT_NOTIFIER_MODE=slack` with `SLACK_WEBHOOK_URL=...`
- `ALERT_NOTIFIER_MODE=teams` with `TEAMS_WEBHOOK_URL=...`

Notifier reliability:
- Outbound webhook delivery uses retry/backoff (3 attempts, exponential delay).
- Failed deliveries are stored in local dead-letter memory buffer for debugging.
- Failed deliveries are also persisted to JSONL file when `ALERT_DEAD_LETTER_FILE` is set.
- File replay can mark successful records as processed (`processedAt`) to reduce repeated retries.
- File compaction can remove processed rows and enforce retention by record count/time window.

Example (PowerShell):
```powershell
$env:ALERT_NOTIFIER_MODE="slack"
$env:SLACK_WEBHOOK_URL="https://hooks.slack.com/services/xxx/yyy/zzz"
docker compose -f docker-compose.monitoring.yml up -d --build
```

Teams example (PowerShell):
```powershell
$env:ALERT_NOTIFIER_MODE="teams"
$env:TEAMS_WEBHOOK_URL="https://outlook.office.com/webhook/..."
docker compose -f docker-compose.monitoring.yml up -d --build
```

## 1) Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"demo@huyverse.dev\",\"phone\":\"+84901234567\",\"password\":\"Password123\",\"displayName\":\"Huy Demo\"}"
```

## 2) Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Device-Label: web-portfolio" \
  -d "{\"identifier\":\"demo@huyverse.dev\",\"password\":\"Password123\"}"
```

Login identifier supports:
- email (e.g. `demo@huyverse.dev`)
- phone (e.g. `+84901234567`)

If MFA is enabled for the account, login returns:
- `mfaRequired: true`
- `mfaChallengeTicket`
- optional `devMfaCode` (when dev exposure is enabled)

Complete login by verifying MFA challenge:
```bash
curl -X POST http://localhost:8080/api/auth/login/mfa/verify \
  -H "Content-Type: application/json" \
  -H "X-Device-Label: web-portfolio" \
  -d "{\"challengeTicket\":\"<MFA_TICKET>\",\"code\":\"123456\"}"
```
`code` supports:
- temporary OTP challenge code
- TOTP app code (if TOTP is enabled)
- recovery code (one-time)

Take `access.token` and `refreshToken` from login response. Response now includes:
- `tokenType` (Bearer)
- `expiresIn` (seconds)
- `refreshToken`
- `refreshExpiresIn`

## 2.1) Refresh access token
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"<REFRESH_TOKEN>\"}"
```

## 2.2) Logout current session
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"<REFRESH_TOKEN>\"}"
```

## 2.3) Logout all sessions/devices
```bash
curl -X DELETE http://localhost:8080/api/auth/logout-all \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

## 2.4) List active sessions/devices
```bash
curl http://localhost:8080/api/auth/sessions \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "X-Refresh-Token: <CURRENT_REFRESH_TOKEN>"
```

Session object includes:
- `ipAddress`
- `userAgent`
- `deviceLabel`

## 2.5) Revoke one session by id
```bash
curl -X DELETE http://localhost:8080/api/auth/sessions/{id} \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

## 2.6) Admin maintenance: prune expired sessions
```bash
curl -X POST http://localhost:8080/api/admin/maintenance/sessions/prune \
  -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>"
```

Response includes:
- `message`
- `deletedCount`

## 2.7) Admin maintenance: session metrics snapshot
```bash
curl -X POST http://localhost:8080/api/admin/maintenance/sessions/metrics \
  -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>"
```

Response includes:
- `sessionCreatedCount`
- `sessionRevokedCount`
- `sessionPrunedCount`

## 2.8) Admin maintenance: recent session audit events
```bash
curl -X POST http://localhost:8080/api/admin/maintenance/sessions/events \
  -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>"
```

Supports query params:
- `eventType` (e.g. `SESSION_CREATED`, `SESSION_REVOKED`, `SESSION_PRUNED`, `MFA_CHALLENGE_REQUESTED`, `MFA_VERIFIED`)
- `userEmail`
- `from` / `to` (ISO-8601 datetime)
- `page` (default `0`)
- `size` (default `20`, max `200`)

Datetime validation:
- Invalid `from/to` format returns HTTP `400`.

Example:
```bash
curl -X POST "http://localhost:8080/api/admin/maintenance/sessions/events?eventType=SESSION_CREATED&userEmail=admin@huyverse.dev&page=0&size=5" \
  -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>"
```

Frontend note:
- FE now stores both `access_token` and `refresh_token`.
- On `401`, FE attempts `POST /api/auth/refresh` and retries the request once.

Automated smoke test:
```powershell
cd BE
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-auth.ps1
```

## 3) Get profile
```bash
curl http://localhost:8080/api/profile/me \
  -H "Authorization: Bearer <TOKEN>"
```

## 3.1) Update profile display name
```bash
curl -X PATCH http://localhost:8080/api/profile/me \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d "{\"displayName\":\"Huy Updated\"}"
```

Password policy for register:
- minimum 8 chars
- at least 1 uppercase letter
- at least 1 lowercase letter
- at least 1 digit

## 4) SSE chat stream
Open on browser:
`http://localhost:8080/api/ai/chat/stream?prompt=hello`

Or use FE chat demo in `FE`.

## 5) OAuth2 skeleton
```bash
curl http://localhost:8080/api/auth/oauth2/providers
curl http://localhost:8080/api/auth/oauth2/google/authorize-url
```

Enable real Google OAuth by setting env:
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `GOOGLE_REDIRECT_URI` (default: `http://localhost:8080/api/auth/oauth2/google/callback`)
- `APP_AUTH_JWT_SECRET`
- `APP_AUTH_JWT_EXPIRATION_SECONDS`

Then:
1. Open `authorizeUrl` from `/api/auth/oauth2/google/authorize-url`
2. Google redirects to callback with `code` and `state`
3. Backend exchanges token, fetches user info, issues local token pair

OAuth callback response shape:
- `provider`
- `created`
- `tokens.access.token`
- `tokens.refreshToken`

Detailed setup guide:
- `doc/OAUTH_LIVE_SETUP.md`

## 6) RBAC demo endpoints
Seeded dev users:
- `admin@huyverse.dev` / `Password123`
- `editor@huyverse.dev` / `Password123`

Protected routes:
- `GET /api/admin/ping` requires `ADMIN` or `SUPER_ADMIN`
- `GET /api/editor/ping` requires `EDITOR` or higher

Security error format:
- `401` -> `{"error":"unauthorized","message":"Authentication required"}`
- `403` -> `{"error":"forbidden","message":"Insufficient permission"}`

## 7) Account recovery (password reset skeleton)
Request reset token (dev mode returns token for local testing):
```bash
curl -X POST http://localhost:8080/api/auth/password-reset/request \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@huyverse.dev\"}"
```

Confirm reset with token:
```bash
curl -X POST http://localhost:8080/api/auth/password-reset/confirm \
  -H "Content-Type: application/json" \
  -d "{\"token\":\"<DEV_RESET_TOKEN>\",\"newPassword\":\"NewPassword123\"}"
```

Notes:
- Reset token is stored hashed in DB, with expiration and one-time use.
- Default expiration: `1800` seconds (`app.auth.password-reset-expiration-seconds`).
- Request cooldown default: `60` seconds (`app.auth.password-reset-request-cooldown-seconds`).
- If requested too frequently, API returns `429 Too Many Requests`.
- Dev token exposure toggle:
  - `app.auth.password-reset-expose-token=true` (default local)
- Reset actions are now logged in audit events:
  - `PASSWORD_RESET_REQUESTED`
  - `PASSWORD_RESET_CONFIRMED`

## 8) MFA skeleton (OTP challenge/verify)
Request MFA challenge (authenticated):
```bash
curl -X POST http://localhost:8080/api/auth/mfa/challenge \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Verify MFA code:
```bash
curl -X POST http://localhost:8080/api/auth/mfa/verify \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d "{\"code\":\"123456\"}"
```

Notes:
- Dev mode can expose OTP code in response: `app.auth.mfa.expose-code=true`.
- Code TTL default: `300s` (`app.auth.mfa.expiration-seconds`).
- Request cooldown default: `30s` (`app.auth.mfa.cooldown-seconds`).
- Max failed attempts default: `5` (`app.auth.mfa.max-failed-attempts`).

Update MFA setting for current user:
```bash
curl -X POST http://localhost:8080/api/auth/mfa/settings \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d "{\"enabled\":true}"
```

## 9) TOTP authenticator skeleton
Start TOTP setup:
```bash
curl -X POST http://localhost:8080/api/auth/mfa/totp/setup \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Generate QR image from otpauth URL:
```bash
curl -X POST http://localhost:8080/api/auth/mfa/totp/qrcode \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d "{\"otpauthUrl\":\"otpauth://totp/...\"}"
```

Confirm TOTP setup using authenticator code:
```bash
curl -X POST http://localhost:8080/api/auth/mfa/totp/confirm \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d "{\"code\":\"123456\"}"
```

Regenerate recovery codes:
```bash
curl -X POST http://localhost:8080/api/auth/mfa/totp/recovery/regenerate \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Disable TOTP:
```bash
curl -X POST http://localhost:8080/api/auth/mfa/totp/disable \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Security hardening:
- TOTP secret is now encrypted at rest in DB.
- Optional encryption key:
  - `APP_AUTH_CRYPTO_KEY_BASE64` (AES key in Base64; 16/24/32-byte raw key)
  - if omitted, system derives fallback key from `APP_AUTH_JWT_SECRET` for local/dev convenience.

MFA challenge store mode:
- `APP_AUTH_MFA_CHALLENGE_STORE=memory` (default)
- `APP_AUTH_MFA_CHALLENGE_STORE=redis`

When using Redis mode, configure Spring Redis connection (example):
- `SPRING_REDIS_HOST=localhost`
- `SPRING_REDIS_PORT=6379`
