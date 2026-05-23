# SUPERAPP PORTFOLIO - MASTER PLAN

## 1) Tầm nhìn sản phẩm
Xây dựng một **Super Hybrid App** đa nền tảng (Web, Mobile, Windows) lấy chủ đề chính là **Portfolio cá nhân**, nhưng mở rộng đầy đủ các nhóm chức năng trong `Gom hết.docx` để thể hiện năng lực kiến trúc và triển khai ở cấp Staff/Principal.

Tên gợi ý: **HuyVerse Super Portfolio**

## 2) Product Concept (Portfolio làm lõi)
- Landing/Portfolio là mặt tiền thương hiệu cá nhân.
- Bên trong là hệ sinh thái module: blog, e-commerce, social/community, AI chat, intranet demo, web archive demo, SaaS multi-tenant demo.
- Mỗi module vừa có giá trị người dùng thật, vừa là “showcase kỹ thuật” cho nhà tuyển dụng.

## 3) Kiến trúc tổng thể
- `FE`: React + Vite + TypeScript (Portal Web chính)
- `Mobile`: React Native Expo (khách hàng di động)
- `Windows`: Electron shell (bản desktop cho admin/operator demo)
- `BE`: Spring Boot (REST API, auth, business logic, integrations)
- `Py`: FastAPI (AI services, CV parser, crawler jobs)
- Database: PostgreSQL (+ PostGIS cho geofencing)
- Cache/Queue: Redis
- Object Storage: MinIO
- Observability: Prometheus + Grafana

## 4) Mapping 9 nhóm chức năng vào Portfolio

### Nhóm 1 - User Management
- Auth email/phone + password strength.
- OAuth2 (Google/GitHub/Facebook), 2FA OTP.
- Hồ sơ cá nhân nâng cao (avatar crop/compress, privacy settings).
- RBAC: SuperAdmin, Admin, Editor, Moderator, Member, VIP.
- Account recovery + session/device management.

### Nhóm 2 - Content & Navigation
- CMS bài viết dự án/case-study.
- Category/Tag/Breadcrumb.
- Search thông minh + autosuggest + filter.
- Upload/download subsystem có validate định dạng và secure link.

### Nhóm 3 - E-Commerce
- Bán dịch vụ cá nhân: tư vấn, template, khóa học mini, asset số.
- Cart + coupon + pricing rule + checkout đa cổng.
- Quản lý đơn hàng + tracking trạng thái.

### Nhóm 4 - Engagement
- Comment lồng nhau + reaction + moderation queue.
- Review cho dịch vụ/sản phẩm số.
- Notification realtime (WebSocket/SSE), push notification.
- Live chat và AI bot FAQ.

### Nhóm 5 - UX/UI Utilities
- i18n (vi/en/ja), auto-detect locale.
- Theme engine dark/light + font scaling.
- Dynamic form builder (contact/quote/newsletter).

### Nhóm 6 - Admin & Security
- Admin CMS + media library.
- Analytics dashboard (traffic, conversion, revenue demo).
- reCAPTCHA/Turnstile + rate limit + anti brute force.
- Backup strategy + audit log.

### Nhóm 7 - AI & Automation
- AI Chat (SSE streaming token).
- AI summary/translate/sentiment.
- CV parser (PDF/DOCX).
- Recommendation engine (portfolio/project suggestion cá nhân hóa).
- Multimodal search (image/voice).

### Nhóm 8 - Gamification
- Điểm thưởng theo hành vi (đọc bài, comment, mua hàng).
- Tier + badge + quest.
- Mini game vòng quay voucher.

### Nhóm 9 - Integrations & API
- REST API/GraphQL mở cho mobile/windows.
- Webhook events (order.created, comment.flagged, user.tier.upgraded).
- Integrations CRM, Zapier/Make, Sheets.

## 5) Các vertical website (11 loại) trong cùng hệ thống
- Blog/News: module CMS + SEO pages.
- E-commerce: digital products/services.
- Social/Forum: community feed + thread.
- Corporate/Landing: portfolio landing + brand pages.
- AI Platform: AI chat workspace.
- Web3 DApp: wallet connect + on-chain event demo.
- Job Board: tuyển dụng/cộng tác + CV pipeline.
- Intranet: demo workflow/chấm công/geofence.
- Web Archive: demo snapshot + time-travel view.
- SaaS: workspace multi-tenant.
- Knowledge Wiki/Database: docs dự án + version history.

## 6) Lộ trình triển khai (khuyến nghị)

### Phase 0 - Foundation
- Chuẩn hóa monorepo scripts.
- Env management + docker compose local stack.
- Auth cơ bản + healthcheck + CI lint/test.

### Phase 1 - Portfolio Core (MVP có thể show)
- Landing + profile + project showcase + blog.
- Auth/OAuth + RBAC cơ bản.
- Contact form + admin CMS cơ bản.

### Phase 2 - AI Highlight
- AI chat SSE (Web trước, Mobile sau).
- AI content assistant cho bài blog.
- Notification realtime.

### Phase 3 - Commerce + Community
- Product catalog + cart + coupon + order.
- Review/comment/reaction/moderation.
- Dashboard analytics.

### Phase 4 - Advanced Showcase
- Web3 sync on-chain/off-chain.
- CV parser + job board.
- Web archive crawler snapshot.
- Gamification system.

### Phase 5 - SaaS + Intranet Demo
- Multi-tenant schema-per-tenant.
- Workflow + geofence check-in demo.
- Hardening security + backup + observability hoàn chỉnh.

## 7) Definition of Done (cho từng module)
- Có API spec (OpenAPI) + test cơ bản.
- Có UI flow web; nếu cần thì có mobile flow.
- Có role/permission check.
- Có logging + metrics + error handling.
- Có docs ngắn: mục đích, luồng chính, cách chạy.

## 8) Rủi ro kỹ thuật chính
- Scope quá lớn: cần bám phase, không làm đồng thời tất cả.
- Node/Java version mismatch: cần chuẩn hóa toolchain.
- Tích hợp quá nhiều dịch vụ ngoài: ưu tiên mock provider trước, bật thật sau.

## 9) Bước tiếp theo ngay bây giờ
1. Khóa phiên bản môi trường (Node, Java, Python) và thêm hướng dẫn setup.
2. Tạo root scripts để chạy FE/BE/Py/Mobile/Windows từ một chỗ.
3. Bắt đầu module đầu tiên: **AI Chat SSE** (điểm nhấn portfolio mạnh nhất).

## 10) Tài liệu theo dõi tiến độ
- `doc/PROGRESS_TRACKER.md`: Checklist toàn bộ phạm vi theo `Gom hết.docx`, có trạng thái done/in-progress/not started.
- `doc/SESSION_LOG.md`: Nhật ký từng buổi làm việc để theo dõi sát tiến độ và blocker.
- `doc/PHASE_ROADMAP_DETAILED.md`: Kế hoạch chia phase chi tiết (mục tiêu, deliverables, exit criteria).
