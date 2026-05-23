# SUPERAPP PORTFOLIO - PROGRESS TRACKER

## Legend
- `[x]` Done
- `[-]` In progress
- `[ ]` Not started

## Last update
- Date: 2026-05-23
- Session focus: Dead-letter compaction + retention policy endpoints

---

## A. Platform Foundation (Hybrid)
- [x] Root folders: `FE`, `BE`, `Py`, `Mobile`, `Windows`
- [x] Initial bootstrap docs created
- [x] FE initialized (Vite + React)
- [x] Mobile initialized (Expo React Native)
- [x] Windows initialized (Electron shell)
- [x] Py initialized (FastAPI basic health endpoint)
- [x] BE skeleton initialized (Spring Boot structure + health endpoint + security config)
- [-] Root monorepo standardization (Nx/Turborepo)
- [-] Docker compose local stack (Postgres/Redis/MinIO + monitoring + Alertmanager + notifier bridge mode; production channels tuning pending)
- [ ] CI pipeline (lint/test/build/deploy preview)

## B. Coding Standard Alignment
- [x] FE switched to JavaScript/JSX
- [x] Mobile switched to JavaScript/JSX
- [x] TypeScript removed from FE and Mobile app source
- [ ] Shared coding conventions document
- [ ] Shared eslint/prettier config across FE/Mobile/Windows

## C. Flagship Module - AI Chat SSE
- [x] BE SSE endpoint `/api/ai/chat/stream` created
- [x] BE CORS for FE local dev
- [x] Security rule allows SSE endpoint
- [x] FE chat UI consuming SSE stream with incremental token rendering
- [-] Mobile SSE chat screen (implemented, backend verified; device/emulator verification pending)
- [ ] Replace mock streamed text with real LLM provider
- [ ] Conversation persistence/history API

---

## D. Full Scope Tracker (Mapped from Gom hết.docx)

### 1) Hệ thống & Người dùng (User Management)
- [-] Register/Login (email + phone identifier skeleton ready; JWT + password policy backend-side)
- [-] OAuth2 social login (Google FE+BE flow integrated; awaiting live provider verification with real credentials)
- [-] 2FA/MFA (OTP + TOTP/recovery step-up ready; distributed persistence/ops hardening pending)
- [-] Profile management (get/update `displayName` + email/phone read-ready; avatar/address pending)
- [-] Device/session management + logout all devices (metadata + FE session panel + prune maintenance ready)
- [ ] Privacy settings
- [-] RBAC roles (enum + JWT filter + protected admin/editor demo endpoints + standardized 401/403 response)
- [-] Account recovery flow (BE+FE wired; one-time token + expiration + cooldown + audit events)

### 2) Nội dung & Tìm kiếm (Content & Navigation)
- [ ] Advanced search + autosuggest
- [ ] Faceted filters
- [ ] Typo tolerance + synonym handling
- [ ] Taxonomy category tree + tags + breadcrumbs
- [ ] Media engine (video/audio adaptive, gallery, lazy load)
- [ ] Upload/download subsystem with validation + secure links

### 3) Thương mại điện tử (E-Commerce)
- [ ] Dynamic shopping cart
- [ ] Realtime totals/tax/shipping recalculation
- [ ] Payment gateways (local/international/COD)
- [ ] Order lifecycle + logistics integration
- [ ] Coupon/voucher engine
- [ ] Flexible pricing (quantity tier/flash sale)

### 4) Tương tác & Cộng đồng (Engagement)
- [ ] Ratings/reviews + verified purchase
- [ ] Threaded comments + mentions + reactions
- [ ] Content moderation filter queue
- [ ] Live chat + chatbot flows
- [ ] Social sharing + Open Graph config
- [ ] Realtime in-app notifications + push notifications

### 5) Tiện ích & Trải nghiệm (UX/UI Utilities)
- [ ] i18n + locale detection + currency adaptation
- [ ] Theme engine (dark/light + system sync)
- [ ] Font scaling without layout break
- [ ] Dynamic validated forms (contact/quote/newsletter)

### 6) Quản trị & Bảo mật (Admin/Backend & Security)
- [ ] Advanced CMS editor
- [ ] Media library with folders/search/reuse
- [ ] Analytics dashboard
- [ ] SSL/HTTPS hardening
- [ ] Anti-spam/bot protections (reCAPTCHA/Turnstile)
- [ ] WAF/rate-limit/brute-force controls
- [ ] Automated backup strategy

### 7) AI & Automation
- [-] NLP engine baseline (SSE streaming skeleton complete)
- [ ] Summarization/translation/sentiment modules
- [ ] Generative content for blog/image/TTS
- [ ] Recommendation engine
- [ ] Multimodal input (image search, voice search)
- [ ] Code sandbox/runner

### 8) Gamification
- [ ] Points + tiers
- [ ] Quests + badges
- [ ] Lucky spin / minigame

### 9) Integrations & API
- [ ] REST/GraphQL external API
- [ ] Webhook event dispatcher
- [ ] Third-party integrations (CRM/Zapier/Make)

---

## E. 11 Website Type Coverage (Portfolio Super App)
- [ ] 1. News/Blog
- [ ] 2. E-commerce
- [ ] 3. Social/Forum
- [ ] 4. Corporate/Landing
- [-] 5. AI Chat platform (initial skeleton done)
- [ ] 6. Web3/DApp
- [ ] 7. Recruitment/Jobs
- [ ] 8. Intranet
- [ ] 9. Web Archive
- [ ] 10. SaaS platform
- [ ] 11. Wiki/Knowledge base

---

## F. Immediate Next Milestones
- [x] Add Maven Wrapper (`mvnw`) so BE can run without global Maven
- [x] Bring up BE and test SSE endpoint live
- [-] Connect Mobile chat screen to same SSE endpoint
- [x] Add auth module skeleton (JWT token auth + role model + user entity with runtime smoke test)
- [-] Run live Google OAuth login end-to-end with real credentials
- [-] Add FE toast UX polish for async flows (base integrated; tuning pending)
