import { useEffect, useState } from "react";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const ACCESS_KEY = "access_token";
const REFRESH_KEY = "refresh_token";
const DEVICE_LABEL = "web-portfolio";

export default function App() {
  const [prompt, setPrompt] = useState("");
  const [messages, setMessages] = useState([]);
  const [isStreaming, setIsStreaming] = useState(false);
  const [email, setEmail] = useState("demo@huyverse.dev");
  const [phone, setPhone] = useState("");
  const [identifier, setIdentifier] = useState("demo@huyverse.dev");
  const [password, setPassword] = useState("Password123");
  const [displayName, setDisplayName] = useState("Huy Demo");
  const [token, setToken] = useState(() => localStorage.getItem(ACCESS_KEY) || "");
  const [refreshToken, setRefreshToken] = useState(() => localStorage.getItem(REFRESH_KEY) || "");
  const [me, setMe] = useState(null);
  const [authMessage, setAuthMessage] = useState("");
  const [toast, setToast] = useState(null);
  const [rbacResult, setRbacResult] = useState("");
  const [sessions, setSessions] = useState([]);
  const [auditItems, setAuditItems] = useState([]);
  const [auditPage, setAuditPage] = useState(0);
  const [auditSize, setAuditSize] = useState(5);
  const [auditTotalPages, setAuditTotalPages] = useState(0);
  const [auditFilterType, setAuditFilterType] = useState("");
  const [auditFilterEmail, setAuditFilterEmail] = useState("");
  const [auditFilterFrom, setAuditFilterFrom] = useState("");
  const [auditFilterTo, setAuditFilterTo] = useState("");
  const [resetEmail, setResetEmail] = useState("admin@huyverse.dev");
  const [resetToken, setResetToken] = useState("");
  const [resetNewPassword, setResetNewPassword] = useState("NewPassword123");
  const [mfaCode, setMfaCode] = useState("");
  const [loginMfaTicket, setLoginMfaTicket] = useState("");
  const [loginMfaCode, setLoginMfaCode] = useState("");
  const [totpSecret, setTotpSecret] = useState("");
  const [totpOtpAuthUrl, setTotpOtpAuthUrl] = useState("");
  const [totpQrDataUrl, setTotpQrDataUrl] = useState("");
  const [totpVerifyCode, setTotpVerifyCode] = useState("");
  const [totpRecoveryCodes, setTotpRecoveryCodes] = useState([]);

  const readApiError = async (res, fallbackMessage) => {
    try {
      const data = await res.json();
      if (data?.message) return data.message;
      if (data?.error) return data.error;
      return fallbackMessage;
    } catch {
      try {
        const text = await res.text();
        return text || fallbackMessage;
      } catch {
        return fallbackMessage;
      }
    }
  };

  const notify = (type, text) => {
    setToast({ type, text });
    setTimeout(() => {
      setToast((prev) => (prev && prev.text === text ? null : prev));
    }, 2800);
  };

  const saveTokens = (accessToken, refresh) => {
    if (accessToken) {
      localStorage.setItem(ACCESS_KEY, accessToken);
      setToken(accessToken);
    }
    if (refresh) {
      localStorage.setItem(REFRESH_KEY, refresh);
      setRefreshToken(refresh);
    }
  };

  const clearTokens = () => {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    setToken("");
    setRefreshToken("");
  };

  const refreshAccessToken = async () => {
    if (!refreshToken) return null;
    const res = await fetch(`${API_BASE}/api/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return null;
    const data = await res.json();
    const nextAccess = data?.access?.token || "";
    if (!nextAccess) return null;
    saveTokens(nextAccess, data.refreshToken || refreshToken);
    return nextAccess;
  };

  const authFetch = async (path, options = {}, retry = true) => {
    const headers = { ...(options.headers || {}) };
    if (token) headers.Authorization = `Bearer ${token}`;
    let res = await fetch(`${API_BASE}${path}`, { ...options, headers });
    if (res.status === 401 && retry && refreshToken) {
      const nextAccess = await refreshAccessToken();
      if (!nextAccess) return res;
      const retryHeaders = { ...(options.headers || {}), Authorization: `Bearer ${nextAccess}` };
      res = await fetch(`${API_BASE}${path}`, { ...options, headers: retryHeaders });
    }
    return res;
  };

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const code = params.get("code");
    const state = params.get("state");
    if (!code || !state) return;

    fetch(`${API_BASE}/api/auth/oauth2/google/callback?code=${encodeURIComponent(code)}&state=${encodeURIComponent(state)}`)
      .then((r) => r.json())
      .then((data) => {
        const accessToken = data?.tokens?.access?.token || "";
        const refresh = data?.tokens?.refreshToken || "";
        if (!accessToken) {
          setAuthMessage("Google callback failed.");
          notify("error", "Google callback failed.");
          return;
        }
        saveTokens(accessToken, refresh);
        setAuthMessage(`Google login success (${data.created ? "new account" : "linked account"})`);
        notify("success", "Google login success.");
        window.history.replaceState({}, "", window.location.pathname);
      })
      .catch(() => {
        setAuthMessage("Google callback failed.");
        notify("error", "Google callback failed.");
      });
  }, []);

  const register = async () => {
    setAuthMessage("");
    const res = await fetch(`${API_BASE}/api/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Device-Label": DEVICE_LABEL },
      body: JSON.stringify({ email, phone, password, displayName }),
    });
    const data = await res.json();
    const accessToken = data?.access?.token || "";
    const refresh = data?.refreshToken || "";
    if (!res.ok || !accessToken) {
      const msg = data?.message || data?.error || "Register failed.";
      setAuthMessage(msg);
      notify("error", msg);
      return;
    }
    saveTokens(accessToken, refresh);
    setAuthMessage("Register success.");
    notify("success", "Register success.");
  };

  const login = async () => {
    setAuthMessage("");
    const res = await fetch(`${API_BASE}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Device-Label": DEVICE_LABEL },
      body: JSON.stringify({ identifier, password }),
    });
    const data = await res.json();
    if (data?.mfaRequired) {
      setLoginMfaTicket(data.mfaChallengeTicket || "");
      setLoginMfaCode(data.devMfaCode || "");
      setAuthMessage("MFA required. Enter code then verify login.");
      notify("info", "MFA required.");
      return;
    }
    const accessToken = data?.access?.token || "";
    const refresh = data?.refreshToken || "";
    if (!res.ok || !accessToken) {
      const msg = data?.message || data?.error || "Login failed.";
      setAuthMessage(msg);
      notify("error", msg);
      return;
    }
    saveTokens(accessToken, refresh);
    setAuthMessage("Login success.");
    notify("success", "Login success.");
  };

  const verifyLoginMfa = async () => {
    const res = await fetch(`${API_BASE}/api/auth/login/mfa/verify`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Device-Label": DEVICE_LABEL },
      body: JSON.stringify({ challengeTicket: loginMfaTicket, code: loginMfaCode }),
    });
    const data = await res.json();
    const accessToken = data?.access?.token || "";
    const refresh = data?.refreshToken || "";
    if (!res.ok || !accessToken) {
      const msg = data?.message || data?.error || "MFA login verify failed.";
      setAuthMessage(msg);
      notify("error", msg);
      return;
    }
    saveTokens(accessToken, refresh);
    setLoginMfaTicket("");
    setAuthMessage("Login success (after MFA).");
    notify("success", "Login success.");
  };

  const loginWithGoogle = async () => {
    setAuthMessage("");
    const res = await fetch(`${API_BASE}/api/auth/oauth2/google/authorize-url`);
    const data = await res.json();
    if (!data.authorizeUrl || data.status === "not_configured") {
      setAuthMessage("Google OAuth is not configured on backend.");
      notify("error", "Google OAuth is not configured on backend.");
      return;
    }
    window.location.href = data.authorizeUrl;
  };

  const fetchMe = async () => {
    const res = await authFetch("/api/profile/me");
    if (!res.ok) {
      setAuthMessage(await readApiError(res, "Profile fetch failed."));
      notify("error", "Profile fetch failed.");
      return;
    }
    setMe(await res.json());
    notify("success", "Profile loaded.");
  };

  const logout = async () => {
    if (refreshToken) {
      const out = await fetch(`${API_BASE}/api/auth/logout`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
      });
      if (!out.ok) {
        setAuthMessage(await readApiError(out, "Logout failed."));
        notify("error", "Logout failed.");
      }
    }
    clearTokens();
    setMe(null);
    setAuthMessage("Logged out.");
    notify("info", "Logged out.");
    setRbacResult("");
  };

  const logoutAll = async () => {
    const res = await authFetch("/api/auth/logout-all", { method: "DELETE" });
    if (!res.ok) {
      setAuthMessage(await readApiError(res, "Logout all failed."));
      notify("error", "Logout all failed.");
      return;
    }
    clearTokens();
    setMe(null);
    setAuthMessage("Logged out all devices.");
    notify("info", "Logged out all devices.");
    setRbacResult("");
    setSessions([]);
  };

  const fetchSessions = async () => {
    const res = await authFetch("/api/auth/sessions", {
      headers: refreshToken ? { "X-Refresh-Token": refreshToken } : {},
    });
    if (!res.ok) {
      setAuthMessage(await readApiError(res, "Load sessions failed."));
      notify("error", "Load sessions failed.");
      return;
    }
    const data = await res.json();
    setSessions(Array.isArray(data) ? data : []);
  };

  const revokeSession = async (sessionId) => {
    const res = await authFetch(`/api/auth/sessions/${sessionId}`, { method: "DELETE" });
    if (!res.ok) {
      setAuthMessage(await readApiError(res, "Revoke session failed."));
      notify("error", "Revoke session failed.");
      return;
    }
    setAuthMessage("Session revoked.");
    notify("success", "Session revoked.");
    fetchSessions();
  };

  const requestPasswordReset = async () => {
    const res = await fetch(`${API_BASE}/api/auth/password-reset/request`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: resetEmail }),
    });
    if (!res.ok) {
      const msg = await readApiError(res, "Request reset failed.");
      setAuthMessage(msg);
      notify("error", msg);
      return;
    }
    const data = await res.json();
    if (data?.devResetToken) {
      setResetToken(data.devResetToken);
      notify("success", "Reset token generated (dev mode).");
    } else {
      notify("info", "Reset request accepted.");
    }
    setAuthMessage(data?.message || "Reset request sent.");
  };

  const confirmPasswordReset = async () => {
    const res = await fetch(`${API_BASE}/api/auth/password-reset/confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token: resetToken, newPassword: resetNewPassword }),
    });
    if (!res.ok) {
      const msg = await readApiError(res, "Confirm reset failed.");
      setAuthMessage(msg);
      notify("error", msg);
      return;
    }
    const data = await res.json();
    setAuthMessage(data?.message || "Password reset successful.");
    notify("success", "Password reset successful.");
  };

  const requestMfaChallenge = async () => {
    const res = await authFetch("/api/auth/mfa/challenge", { method: "POST" });
    if (!res.ok) {
      const msg = await readApiError(res, "Request MFA challenge failed.");
      setAuthMessage(msg);
      notify("error", msg);
      return;
    }
    const data = await res.json();
    if (data?.devCode) {
      setMfaCode(data.devCode);
    }
    setAuthMessage(data?.message || "MFA challenge generated.");
    notify("info", "MFA challenge generated.");
  };

  const verifyMfaChallenge = async () => {
    const res = await authFetch("/api/auth/mfa/verify", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ code: mfaCode }),
    });
    if (!res.ok) {
      const msg = await readApiError(res, "MFA verification failed.");
      setAuthMessage(msg);
      notify("error", msg);
      return;
    }
    const data = await res.json();
    setAuthMessage(data?.message || "MFA verification successful.");
    notify("success", "MFA verification successful.");
  };

  const updateMfaSetting = async (enabled) => {
    const res = await authFetch("/api/auth/mfa/settings", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ enabled }),
    });
    if (!res.ok) {
      const msg = await readApiError(res, "Update MFA setting failed.");
      setAuthMessage(msg);
      notify("error", msg);
      return;
    }
    const data = await res.json();
    setAuthMessage(data?.message || "MFA setting updated.");
    notify("success", `MFA ${enabled ? "enabled" : "disabled"}.`);
    fetchMe();
  };

  const startTotpSetup = async () => {
    const res = await authFetch("/api/auth/mfa/totp/setup", { method: "POST" });
    if (!res.ok) {
      const msg = await readApiError(res, "TOTP setup start failed.");
      setAuthMessage(msg);
      notify("error", msg);
      return;
    }
    const data = await res.json();
    setTotpSecret(data.secret || "");
    setTotpOtpAuthUrl(data.otpauthUrl || "");
    setTotpRecoveryCodes(Array.isArray(data.recoveryCodes) ? data.recoveryCodes : []);
    if (data.otpauthUrl) {
      const qrRes = await authFetch("/api/auth/mfa/totp/qrcode", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ otpauthUrl: data.otpauthUrl }),
      });
      if (qrRes.ok) {
        const qrData = await qrRes.json();
        setTotpQrDataUrl(qrData.imageDataUrl || "");
      } else {
        setTotpQrDataUrl("");
      }
    }
    setAuthMessage("TOTP setup started. Add secret to authenticator and confirm code.");
    notify("info", "TOTP setup started.");
  };

  const confirmTotpSetup = async () => {
    const res = await authFetch("/api/auth/mfa/totp/confirm", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ code: totpVerifyCode }),
    });
    if (!res.ok) {
      const msg = await readApiError(res, "TOTP setup confirm failed.");
      setAuthMessage(msg);
      notify("error", msg);
      return;
    }
    setAuthMessage("TOTP setup confirmed.");
    notify("success", "TOTP enabled.");
    fetchMe();
  };

  const disableTotp = async () => {
    const res = await authFetch("/api/auth/mfa/totp/disable", { method: "POST" });
    if (!res.ok) {
      const msg = await readApiError(res, "Disable TOTP failed.");
      setAuthMessage(msg);
      notify("error", msg);
      return;
    }
    setAuthMessage("TOTP disabled.");
    notify("info", "TOTP disabled.");
    fetchMe();
  };

  const regenerateRecoveryCodes = async () => {
    const res = await authFetch("/api/auth/mfa/totp/recovery/regenerate", { method: "POST" });
    if (!res.ok) {
      const msg = await readApiError(res, "Regenerate recovery codes failed.");
      setAuthMessage(msg);
      notify("error", msg);
      return;
    }
    const data = await res.json();
    setTotpRecoveryCodes(Array.isArray(data.recoveryCodes) ? data.recoveryCodes : []);
    setAuthMessage(data?.message || "Recovery codes regenerated.");
    notify("success", "Recovery codes regenerated.");
  };

  const loadAuditEvents = async (pageOverride = auditPage) => {
    const params = new URLSearchParams();
    params.set("page", String(pageOverride));
    params.set("size", String(auditSize));
    if (auditFilterType.trim()) params.set("eventType", auditFilterType.trim());
    if (auditFilterEmail.trim()) params.set("userEmail", auditFilterEmail.trim());
    if (auditFilterFrom.trim()) params.set("from", auditFilterFrom.trim());
    if (auditFilterTo.trim()) params.set("to", auditFilterTo.trim());

    const res = await authFetch(`/api/admin/maintenance/sessions/events?${params.toString()}`, { method: "POST" });
    if (!res.ok) {
      const errText = await readApiError(res, "Load audit events failed.");
      setAuthMessage(`Load audit events failed. ${errText}`);
      notify("error", `Load audit events failed. ${errText}`);
      return;
    }
    const data = await res.json();
    setAuditItems(Array.isArray(data.items) ? data.items : []);
    setAuditPage(data.page || 0);
    setAuditTotalPages(data.totalPages || 0);
  };

  const callProtected = async (path) => {
    if (!token) {
      setRbacResult("No token.");
      return;
    }
    const res = await authFetch(path);
    const text = await res.text();
    setRbacResult(`${path} -> ${res.status} ${text}`);
  };

  const sendPrompt = (event) => {
    event.preventDefault();
    const text = prompt.trim();
    if (!text || isStreaming) return;

    const userMessage = { role: "user", content: text };
    const assistantMessage = { role: "assistant", content: "" };

    setMessages((prev) => [...prev, userMessage, assistantMessage]);
    setPrompt("");
    setIsStreaming(true);

    const url = `${API_BASE}/api/ai/chat/stream?prompt=${encodeURIComponent(text)}`;
    const source = new EventSource(url);

    source.onmessage = (e) => {
      if (e.data === "[DONE]") {
        source.close();
        setIsStreaming(false);
        return;
      }

      setMessages((prev) => {
        const next = [...prev];
        const last = next[next.length - 1];
        if (last && last.role === "assistant") {
          last.content += e.data;
        }
        return next;
      });
    };

    source.onerror = () => {
      source.close();
      setIsStreaming(false);
      setMessages((prev) => {
        const next = [...prev];
        const last = next[next.length - 1];
        if (last && last.role === "assistant" && !last.content) {
          last.content = "Streaming failed. Please try again.";
        }
        return next;
      });
    };
  };

  return (
    <main style={{ maxWidth: 920, margin: "0 auto", padding: "2rem 1rem", display: "grid", gap: 16 }}>
      {toast ? (
        <div
          style={{
            position: "sticky",
            top: 8,
            zIndex: 20,
            borderRadius: 10,
            padding: "10px 12px",
            color: "#fff",
            background:
              toast.type === "success"
                ? "#166534"
                : toast.type === "error"
                  ? "#991b1b"
                  : "#1f2937",
            boxShadow: "0 8px 20px rgba(0,0,0,0.18)",
          }}
        >
          {toast.text}
        </div>
      ) : null}
      <h1 style={{ marginBottom: 8 }}>HuyVerse AI Chat (SSE Demo)</h1>
      <p style={{ marginTop: 0, opacity: 0.8 }}>
        Super Portfolio flagship module: token streaming from Spring Boot.
      </p>

      <section style={{ border: "1px solid #ddd", borderRadius: 12, padding: 16, background: "#fff" }}>
        <h3 style={{ marginTop: 0 }}>Auth Demo (JWT + Google OAuth)</h3>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr 1fr", gap: 8 }}>
          <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="email" />
          <input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="phone (optional)" />
          <input
            value={identifier}
            onChange={(e) => setIdentifier(e.target.value)}
            placeholder="login email or phone"
          />
          <input value={password} onChange={(e) => setPassword(e.target.value)} placeholder="password" />
          <input
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            placeholder="display name"
            style={{ gridColumn: "span 4" }}
          />
        </div>
        <div style={{ display: "flex", gap: 8, marginTop: 10, flexWrap: "wrap" }}>
          <button onClick={register}>Register</button>
          <button onClick={login}>Login</button>
          <button onClick={loginWithGoogle}>Login with Google</button>
          <button onClick={fetchMe} disabled={!token}>
            Profile /me
          </button>
          <button onClick={logout}>Logout</button>
          <button onClick={logoutAll} disabled={!token}>
            Logout All
          </button>
          <button onClick={() => callProtected("/api/editor/ping")} disabled={!token}>
            Test Editor
          </button>
          <button onClick={() => callProtected("/api/admin/ping")} disabled={!token}>
            Test Admin
          </button>
          <button onClick={() => updateMfaSetting(true)} disabled={!token}>
            Enable MFA
          </button>
          <button onClick={() => updateMfaSetting(false)} disabled={!token}>
            Disable MFA
          </button>
        </div>
        {loginMfaTicket ? (
          <div style={{ display: "flex", gap: 8, marginTop: 8, flexWrap: "wrap" }}>
            <input
              value={loginMfaCode}
              onChange={(e) => setLoginMfaCode(e.target.value)}
              placeholder="login mfa code"
              style={{ width: 160 }}
            />
            <button onClick={verifyLoginMfa}>Verify Login MFA</button>
          </div>
        ) : null}
        <p style={{ marginBottom: 0, opacity: 0.85 }}>{authMessage || "No auth action yet."}</p>
        {rbacResult ? <p style={{ marginBottom: 0, opacity: 0.85 }}>{rbacResult}</p> : null}
        {me ? (
          <pre style={{ background: "#f8fafc", padding: 8, borderRadius: 8, overflow: "auto" }}>
            {JSON.stringify(me, null, 2)}
          </pre>
        ) : null}

        <div style={{ marginTop: 12 }}>
          <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <strong>Session Manager</strong>
            <button onClick={fetchSessions} disabled={!token}>
              Load Sessions
            </button>
          </div>
          {sessions.length === 0 ? (
            <p style={{ opacity: 0.8, marginTop: 8 }}>No sessions loaded.</p>
          ) : (
            <div style={{ marginTop: 8, display: "grid", gap: 8 }}>
              {sessions.map((s) => (
                <div
                  key={s.id}
                  style={{
                    border: "1px solid #e2e8f0",
                    borderRadius: 8,
                    padding: 8,
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    gap: 8,
                  }}
                >
                  <div style={{ fontSize: 13 }}>
                    <div>
                      <strong>Session #{s.id}</strong> {s.current ? "(current)" : ""}
                    </div>
                    <div>Created: {s.createdAt}</div>
                    <div>Expires: {s.expiresAt}</div>
                    <div>Revoked: {String(s.revoked)}</div>
                    <div>IP: {s.ipAddress || "-"}</div>
                    <div>Device: {s.deviceLabel || "-"}</div>
                    <div title={s.userAgent || "-"}>UA: {s.userAgent ? `${s.userAgent.slice(0, 70)}...` : "-"}</div>
                  </div>
                  <button
                    onClick={() => revokeSession(s.id)}
                    disabled={s.current || s.revoked}
                    title={s.current ? "Use Logout for current session" : "Revoke this session"}
                  >
                    Revoke
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        <div style={{ marginTop: 16 }}>
          <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
            <strong>Session Audit Events</strong>
            <select
              value={auditFilterType}
              onChange={(e) => setAuditFilterType(e.target.value)}
              style={{ width: 140 }}
            >
              <option value="">All events</option>
              <option value="SESSION_CREATED">SESSION_CREATED</option>
              <option value="SESSION_REVOKED">SESSION_REVOKED</option>
              <option value="SESSION_PRUNED">SESSION_PRUNED</option>
              <option value="PASSWORD_RESET_REQUESTED">PASSWORD_RESET_REQUESTED</option>
              <option value="PASSWORD_RESET_CONFIRMED">PASSWORD_RESET_CONFIRMED</option>
            </select>
            <input
              value={auditFilterEmail}
              onChange={(e) => setAuditFilterEmail(e.target.value)}
              placeholder="userEmail"
              style={{ width: 180 }}
            />
            <input
              value={auditFilterFrom}
              onChange={(e) => setAuditFilterFrom(e.target.value)}
              placeholder="from (ISO-8601)"
              style={{ width: 180 }}
            />
            <input
              value={auditFilterTo}
              onChange={(e) => setAuditFilterTo(e.target.value)}
              placeholder="to (ISO-8601)"
              style={{ width: 180 }}
            />
            <input
              value={auditSize}
              onChange={(e) => setAuditSize(Number(e.target.value || 5))}
              placeholder="size"
              style={{ width: 70 }}
            />
            <button onClick={() => loadAuditEvents(0)} disabled={!token}>
              Load Events
            </button>
          </div>

          {auditItems.length === 0 ? (
            <p style={{ opacity: 0.8, marginTop: 8 }}>No audit events loaded.</p>
          ) : (
            <div style={{ marginTop: 8, display: "grid", gap: 8 }}>
              {auditItems.map((ev) => (
                <div key={ev.id} style={{ border: "1px solid #e2e8f0", borderRadius: 8, padding: 8 }}>
                  <div>
                    <strong>#{ev.id}</strong> {ev.eventType}
                  </div>
                  <div>User: {ev.userEmail || "-"}</div>
                  <div>IP: {ev.ipAddress || "-"}</div>
                  <div>Device: {ev.deviceLabel || "-"}</div>
                  <div>Detail: {ev.detail || "-"}</div>
                  <div>At: {ev.createdAt}</div>
                </div>
              ))}
              <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                <button onClick={() => loadAuditEvents(Math.max(auditPage - 1, 0))} disabled={auditPage <= 0}>
                  Prev
                </button>
                <span>
                  Page {auditPage + 1} / {Math.max(auditTotalPages, 1)}
                </span>
                <button
                  onClick={() => loadAuditEvents(auditPage + 1)}
                  disabled={auditPage + 1 >= auditTotalPages}
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>

        <div style={{ marginTop: 16 }}>
          <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
            <strong>Account Recovery (Password Reset)</strong>
            <input
              value={resetEmail}
              onChange={(e) => setResetEmail(e.target.value)}
              placeholder="account email"
              style={{ width: 200 }}
            />
            <button onClick={requestPasswordReset}>Request Reset</button>
          </div>
          <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap", marginTop: 8 }}>
            <input
              value={resetToken}
              onChange={(e) => setResetToken(e.target.value)}
              placeholder="reset token"
              style={{ width: 260 }}
            />
            <input
              value={resetNewPassword}
              onChange={(e) => setResetNewPassword(e.target.value)}
              placeholder="new password"
              style={{ width: 180 }}
            />
            <button onClick={confirmPasswordReset}>Confirm Reset</button>
          </div>
        </div>

        <div style={{ marginTop: 16 }}>
          <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
            <strong>MFA Skeleton (OTP)</strong>
            <button onClick={requestMfaChallenge} disabled={!token}>
              Request MFA Code
            </button>
            <input
              value={mfaCode}
              onChange={(e) => setMfaCode(e.target.value)}
              placeholder="mfa code"
              style={{ width: 140 }}
            />
            <button onClick={verifyMfaChallenge} disabled={!token}>
              Verify MFA
            </button>
          </div>
        </div>

        <div style={{ marginTop: 16 }}>
          <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
            <strong>TOTP (Authenticator) Skeleton</strong>
            <button onClick={startTotpSetup} disabled={!token}>
              Start TOTP Setup
            </button>
            <input
              value={totpVerifyCode}
              onChange={(e) => setTotpVerifyCode(e.target.value)}
              placeholder="totp code"
              style={{ width: 140 }}
            />
            <button onClick={confirmTotpSetup} disabled={!token}>
              Confirm Setup
            </button>
            <button onClick={regenerateRecoveryCodes} disabled={!token}>
              Regen Recovery Codes
            </button>
            <button onClick={disableTotp} disabled={!token}>
              Disable TOTP
            </button>
          </div>
          {totpSecret ? <p style={{ margin: "8px 0 0 0", opacity: 0.85 }}>Secret: {totpSecret}</p> : null}
          {totpOtpAuthUrl ? (
            <p style={{ margin: "4px 0 0 0", opacity: 0.85, wordBreak: "break-all" }}>otpauth: {totpOtpAuthUrl}</p>
          ) : null}
          {totpQrDataUrl ? (
            <div style={{ marginTop: 8 }}>
              <img src={totpQrDataUrl} alt="TOTP QR" style={{ width: 160, height: 160, border: "1px solid #ddd" }} />
            </div>
          ) : null}
          {totpRecoveryCodes.length > 0 ? (
            <p style={{ margin: "4px 0 0 0", opacity: 0.85 }}>
              Recovery: {totpRecoveryCodes.join(", ")}
            </p>
          ) : null}
        </div>
      </section>

      <section
        style={{
          border: "1px solid #ddd",
          borderRadius: 12,
          minHeight: 360,
          padding: 16,
          marginBottom: 16,
          background: "#fff",
        }}
      >
        {messages.length === 0 ? (
          <p style={{ opacity: 0.7 }}>Start by asking a question.</p>
        ) : (
          messages.map((m, idx) => (
            <p key={idx}>
              <strong>{m.role === "user" ? "You" : "AI"}:</strong> {m.content}
            </p>
          ))
        )}
      </section>

      <form onSubmit={sendPrompt} style={{ display: "flex", gap: 8 }}>
        <input
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          placeholder="Type your prompt..."
          style={{ flex: 1, padding: "0.75rem", borderRadius: 8, border: "1px solid #ccc" }}
        />
        <button
          type="submit"
          disabled={isStreaming}
          style={{ padding: "0.75rem 1rem", borderRadius: 8, border: "1px solid #333" }}
        >
          {isStreaming ? "Streaming..." : "Send"}
        </button>
      </form>
    </main>
  );
}
