# SUPERAPP PORTFOLIO - PHASE ROADMAP DETAILED

## Nguyên tắc chia phase
- Mỗi phase phải có demo chạy được.
- Mỗi phase có tiêu chí pass/fail rõ ràng.
- Ưu tiên module tạo giá trị portfolio sớm (AI chat, auth, CMS core).

---

## Phase 0 - Foundation & Runtime Baseline
### Mục tiêu
- Ổn định nền tảng chạy đa app (FE/BE/Py/Mobile/Windows).

### Deliverables
- Cấu trúc repo chuẩn hóa.
- Script chạy từ root.
- Tracker + session log hoạt động.
- FE/Mobile thống nhất JavaScript/JSX.

### Exit criteria
- `FE` build pass.
- `Py` chạy health endpoint.
- `Windows` mở shell được.
- Tài liệu tracker được cập nhật sau mỗi session.

### Trạng thái
- `Done` (đã hoàn thành phần lớn, còn Maven wrapper cho BE).

---

## Phase 1 - Core Identity + Portfolio Surface
### Mục tiêu
- Có bản portfolio dùng được và có lõi user/auth cơ bản.

### Deliverables
- Web landing portfolio (hero, project showcase, contact).
- BE auth skeleton: user, role, login/register endpoints (mock hoặc DB-first).
- RBAC role model.
- Profile basic model (displayName, avatar, bio).

### Exit criteria
- User đăng ký/đăng nhập được.
- Có route bảo vệ theo role.
- Portfolio public pages truy cập tốt trên web.

### Trạng thái
- `In progress`.

---

## Phase 2 - AI Flagship (SSE)
### Mục tiêu
- Có demo AI chat token-streaming làm điểm nhấn portfolio.

### Deliverables
- SSE endpoint ổn định.
- FE chat UI stream token.
- Mobile chat screen (giai đoạn đầu có thể web target qua Expo).
- Lưu lịch sử chat tối thiểu.

### Exit criteria
- Prompt gửi từ FE nhận stream liên tục.
- Có retry/error state rõ ràng.
- Có log request cơ bản phía backend.

### Trạng thái
- `In progress` (FE + BE skeleton done, Mobile pending).

---

## Phase 3 - Content Engine (CMS + Search)
### Mục tiêu
- Biến portfolio thành nền tảng nội dung có quản trị.

### Deliverables
- Bài viết, category/tag, draft/publish.
- Search cơ bản + filter.
- Media upload validation.
- Admin content list + editor cơ bản.

### Exit criteria
- CRUD bài viết hoàn chỉnh.
- Search trả đúng kết quả theo keyword.
- Upload chỉ nhận định dạng hợp lệ.

---

## Phase 4 - Commerce & Engagement
### Mục tiêu
- Thêm dòng doanh thu và tương tác cộng đồng.

### Deliverables
- Product/service catalog.
- Cart + coupon cơ bản.
- Order status flow.
- Comment/review/reaction.
- Notification trong app.

### Exit criteria
- Tạo đơn hàng từ FE thành công.
- Comment thread hoạt động.
- Dashboard hiển thị số liệu cơ bản.

---

## Phase 5 - Advanced AI + Automation
### Mục tiêu
- Nâng chiều sâu kỹ thuật AI theo doc.

### Deliverables
- Summarization/translation/sentiment.
- CV parser pipeline.
- Recommendation baseline.
- Multimodal input placeholder.

### Exit criteria
- Ít nhất 2 pipeline AI chạy production-like.
- Có API contract rõ ràng cho FE/Mobile.

---

## Phase 6 - Multi-tenant SaaS + Intranet
### Mục tiêu
- Chứng minh năng lực kiến trúc enterprise.

### Deliverables
- Tenant model + tenant isolation strategy.
- Workspace/workflow cơ bản.
- Geofence/check-in design (demo-level).

### Exit criteria
- Tách dữ liệu theo tenant hoạt động đúng.
- User tenant A không đọc được tenant B.

---

## Phase 7 - Specialty Modules (Web3, Web Archive, Gamification)
### Mục tiêu
- Hoàn thiện toàn bộ “đủ 11 loại website”.

### Deliverables
- Web3 wallet connect + event sync demo.
- Web snapshot crawler demo.
- Point/tier/badge/lucky spin demo.

### Exit criteria
- Mỗi module có 1 user flow end-to-end.
- Có video/demo script để trình diễn portfolio.

---

## Phase 8 - Hardening & Showcase Release
### Mục tiêu
- Đóng gói thành portfolio cấp Staff/Principal.

### Deliverables
- Security hardening, audit log, backup docs.
- Observability (Prometheus/Grafana) baseline.
- Demo script + kiến trúc diagram + case study viết hoàn chỉnh.

### Exit criteria
- Có bộ tài liệu showcase hoàn chỉnh.
- Có checklist smoke test trước khi demo.

