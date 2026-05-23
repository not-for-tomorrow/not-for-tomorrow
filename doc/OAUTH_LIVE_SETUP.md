# Google OAuth Live Setup (Local)

## 1) Google Cloud Console
1. Create OAuth client (Web application).
2. Add authorized redirect URI:
   - `http://localhost:8080/api/auth/oauth2/google/callback`

## 2) Set environment variables (PowerShell)
```powershell
$env:GOOGLE_CLIENT_ID="your-client-id"
$env:GOOGLE_CLIENT_SECRET="your-client-secret"
$env:GOOGLE_REDIRECT_URI="http://localhost:8080/api/auth/oauth2/google/callback"
$env:APP_AUTH_JWT_SECRET="your-32-char-plus-secret"
$env:APP_AUTH_JWT_EXPIRATION_SECONDS="86400"
```

## 3) Run backend + frontend
```powershell
cd BE
.\mvnw.cmd spring-boot:run
```

In another terminal:
```powershell
cd FE
npm run dev
```

## 4) Test flow
1. Open FE app.
2. Click `Login with Google`.
3. Complete Google consent screen.
4. Verify FE receives token and `Profile /me` works.

## 5) Quick troubleshooting
- If FE shows `Google OAuth is not configured on backend`:
  - Re-check env vars in the same terminal that runs backend.
- If redirect mismatch error:
  - Ensure redirect URI in Google Console exactly matches backend callback URI.

