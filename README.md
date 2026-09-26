# 🛡️ InsurMatch Backend — Spring Boot 3 & PostgreSQL

> **InsurMatch** là giải pháp **B2B SaaS CRM** chuyên biệt cho các Công ty / Đại lý Bảo hiểm (Insurance Agencies & Brokerages). Nền tảng số hóa toàn diện quy trình tiếp nhận nhu cầu, quản lý hồ sơ khách hàng 360°, theo dõi tiến độ hợp đồng (Deals), chăm sóc khách hàng hậu mãi (Ticketing) và quyết toán hoa hồng (Commissions) — được thiết kế và đồng bộ **khớp 100% với giao diện Frontend (`EXE201-FE`)**.

---

## 📌 Mục lục
- [1. Danh mục Phân hệ Chức năng (Khớp FE)](#1-danh-mục-phân-hệ-chức-năng-khớp-fe)
- [2. Bảng Đối Chiếu 39 API Endpoints (FE ↔ BE)](#2-bảng-đối-chiếu-39-api-endpoints-fe--be)
- [3. Tài Khoản Demo Mặc Định](#3-tài-khoản-demo-mặc-định)
- [4. Hướng Dẫn Chạy Môi Trường Local](#4-hướng-dẫn-chạy-môi-trường-local)
- [5. Thông Tin Triển Khai Render (Production)](#5-thông-tin-triển-khai-render-production)
- [6. Ghi Chú Chi Tiết Theo Từng Nhóm API (`api.js`)](#6-ghi-chú-chi-tiết-theo-từng-nhóm-api-apijs)

---

## 1. Danh mục Phân hệ Chức năng (Khớp FE)

### 🔐 Phân hệ 1: Xác thực & Phân quyền (Authentication & RBAC)
*Khớp với: `LoginPage.jsx`, `authService.js`, `SecurityConfig.java`, `AuthController.java`*

| Chức năng Backend | Method & Endpoint | Khớp với UI Frontend |
|:---|:---|:---|
| **Đăng nhập & Cấp JWT** | `POST /api/auth/login` | Form đăng nhập phân quyền cho 3 Actor: `ADMIN`, `STAFF`, `AGENT`. |
| **Lấy thông tin phiên đăng nhập** | `GET /api/auth/me` | Navbar, User Profile, Header Avatar và điều hướng Router theo Role. |
| **Đăng xuất** | `POST /api/auth/logout` | Đăng xuất và dọn dẹp token ở Client. |

---

### 📋 Phân hệ 2: Tiếp nhận Nhu cầu & Ghép Agent (Lead Intake & Quote Matching)
*Khớp với: `QuotePage.jsx`, `QuoteModal.jsx`, `AdminDashboard.jsx`, `QuoteController.java`*

| Chức năng Backend | Method & Endpoint | Khớp với UI Frontend |
|:---|:---|:---|
| **Submit yêu cầu báo giá (Lead)** | `POST /api/quotes` | Intake flow 4 bước trên Web (Zip code, State, loại bảo hiểm ACA/Medicare/Life, Household size, Income). |
| **Xem danh sách Lead cho Admin/Staff** | `GET /api/admin/quotes` | Màn hình Match Inquiries trên `AdminDashboard` và `StaffDashboard`. |
| **Ghép Lead / Chỉ định Agent** | `PUT /api/admin/quotes/{id}/assign` | Thao tác phân bổ Lead cho Agent chăm sóc (Assign Agent). |

---

### 👥 Phân hệ 3: Quản lý Khách hàng & Quan hệ (CRM & Contacts)
*Khớp với: `ContactsPage.jsx`, `ContactDetailPage.jsx`, `ContactController.java`*

| Chức năng Backend | Method & Endpoint | Khớp với UI Frontend |
|:---|:---|:---|
| **Danh sách khách hàng** | `GET /api/contacts` | Danh sách CRM có lọc theo Search, Owner, Stage, Tag. |
| **Chi tiết khách hàng** | `GET /api/contacts/{id}` | Chi tiết thông tin cá nhân, hồ sơ bảo hiểm, timeline hoạt động. |
| **Tạo mới khách hàng** | `POST /api/contacts` | Modal thêm liên hệ mới từ Agent/Staff. |
| **Thêm ghi chú khách hàng** | `POST /api/contacts/{id}/notes` | Tab Notes trong trang Contact Detail. |
| **Thêm nhiệm vụ liên hệ** | `POST /api/contacts/{id}/tasks` | Tab Tasks liên quan trực tiếp đến khách hàng. |
| **Ghi nhận lịch sử hoạt động** | `POST /api/contacts/{id}/activities` | Timeline ghi nhận cuộc gọi, email, buổi gặp tư vấn. |

---

### 💼 Phân hệ 4: Quản trị Cơ hội / Hợp đồng Bảo hiểm (Deals & Pipeline)
*Khớp với: `DealsPage.jsx`, `DealDetailPage.jsx`, `DealController.java`*

| Chức năng Backend | Method & Endpoint | Khớp với UI Frontend |
|:---|:---|:---|
| **Kanban Pipeline & Danh sách Deal** | `GET /api/deals` | Bảng Kanban Deal (New, Contacted, Quoted, Enrolled, Bound, Closed). |
| **Chi tiết Deal** | `GET /api/deals/{id}` | Chi tiết giá trị hợp đồng, carrier, plan, hoa hồng dự kiến. |
| **Cập nhật tiến độ Deal** | `PUT /api/deals/{id}` | Kéo thả Kanban hoặc Agent cập nhật stage, giá trị. |
| **Admin duyệt / Quản trị Deal** | `PUT /api/deals/{id}/admin` | Quyền Admin can thiệp vào NPN, Broker Effective Date, trạng thái hợp đồng. |

---

### 📁 Phân hệ 5: Quản lý Hồ sơ & Tài liệu (Document Hub)
*Khớp với: `DocumentsPage.jsx`, `DocumentController.java`*

| Chức năng Backend | Method & Endpoint | Khớp với UI Frontend |
|:---|:---|:---|
| **Lấy danh mục hồ sơ tài liệu** | `GET /api/documents/{id}` | Checklist hồ sơ (ID card, Income proof, Tax return, Policy form). |
| **Upload tệp đính kèm** | `POST /api/documents/{docId}/files` | Nút Upload tài liệu (PDF, PNG, JPG...). |
| **Xóa tệp đính kèm** | `DELETE /api/documents/{docId}/files/{fileId}` | Thao tác quản lý/dọn dẹp file trong danh mục. |

---

### 🎫 Phân hệ 6: Chăm sóc Khách hàng & Hậu mãi Dịch vụ (Customer Care & Ticket Servicing)
*Khớp với: `TicketsPage.jsx`, `TicketDetailPage.jsx`, `TicketController.java`*

> 💡 **Định vị nghiệp vụ**: InsurMatch là nền tảng B2B SaaS CRM bán cho các Agency bảo hiểm. Phân hệ Ticket hoạt động như một Helpdesk chăm sóc khách hàng mua bảo hiểm (Policyholders) do Agency quản lý, phục vụ các nghiệp vụ hậu mãi: Khiếu nại bồi thường (Claim bill), chọn/đổi bác sĩ gia đình (Choose Doctor), xử lý lỗi thanh toán (Payment Issues) và bổ sung hồ sơ chứng từ (Collect Document).

| Chức năng Backend | Method & Endpoint | Khớp với UI Frontend |
|:---|:---|:---|
| **Danh sách Ticket dịch vụ khách hàng** | `GET /api/tickets` | Bộ lọc theo Pipelines chăm sóc khách hàng (`CLIENT_SUPPORT`, `PAYMENT`, `COLLECT_DOCUMENT`, `CHOOSE_DOCTOR`). |
| **Chi tiết Ticket** | `GET /api/tickets/{id}` | Màn hình tiếp nhận và xử lý sự vụ hậu mãi của khách hàng. |
| **Tạo Ticket mới** | `POST /api/tickets` | Mở sự vụ hỗ trợ khách hàng mới. |
| **Cập nhật Ticket** | `PUT /api/tickets/{id}` | Cập nhật tiến độ giải quyết (OPEN, IN_PROGRESS, RESOLVED, CLOSED). |
| **Bình luận / Thảo luận Ticket** | `POST /api/tickets/{id}/comments` | Khung trao đổi nội bộ giữa các thành sự vụ về hồ sơ khách. |

---

### ⏱️ Phân hệ 7: Quản lý Công việc & Nhắc việc (Task Management)
*Khớp với: `TasksPage.jsx`, `TaskController.java`*

| Chức năng Backend | Method & Endpoint | Khớp với UI Frontend |
|:---|:---|:---|
| **Danh sách Task** | `GET /api/tasks` | Danh sách việc cần làm của Agent/Staff (Follow up, Call, Submit form). |
| **Chi tiết Task** | `GET /api/tasks/{id}` | Xem hạn chót (due date), mô tả, độ ưu tiên. |
| **Tạo Task mới** | `POST /api/tasks` | Thêm công việc mới kèm ngày hết hạn. |
| **Cập nhật trạng thái Task** | `PUT /api/tasks/{id}` | Đánh dấu hoàn thành (DONE) hoặc đổi trạng thái. |

---

### 💰 Phân hệ 8: Quản lý Hoa hồng Đại lý & Quyết toán (Agent Commission Management & Payouts)
*Khớp với: `CommissionsPage.jsx`, `CommissionController.java`*

| Chức năng Backend | Method & Endpoint | Khớp với UI Frontend |
|:---|:---|:---|
| **Danh sách chi trả hoa hồng** | `GET /api/commissions` | Bảng quản lý hoa hồng đại lý theo Kỳ (Period), Hãng bảo hiểm (Carrier), Trạng thái. |
| **Báo cáo tổng kết hoa hồng** | `GET /api/commissions/summary` | Widget tổng Gross, Net Amount theo từng Đại lý (Agent). |
| **Ghi nhận khoản hoa hồng** | `POST /api/commissions` | Nhập dữ liệu hoa hồng nhận từ Hãng bảo hiểm đối soát cho Agent. |
| **Cập nhật hoa hồng** | `PUT /api/commissions/{id}` | Điều chỉnh số liệu hoặc phê duyệt (PENDING, APPROVED, PAID). |
| **Tính toán tự động hoa hồng** | `POST /api/commissions/calculate` | Tự động tính toán hoa hồng theo hợp đồng và chính sách chi trả của Agency. |

---

### 📊 Phân hệ 9: Thống kê & Báo cáo (Dashboard & Analytics)
*Khớp với: `DashboardOverview.jsx`, `AdminDashboard.jsx`, `DashboardController.java`*

| Chức năng Backend | Method & Endpoint | Khớp với UI Frontend |
|:---|:---|:---|
| **Tổng quan Dashboard** | `GET /api/dashboard/stats` | Thống kê số lượng Deal, Doanh thu, Khách hàng mới, Tỷ lệ chốt. |
| **Chỉ số quản trị cấp cao** | `GET /api/admin/stats` | Thống kê toàn sàn cho Admin (Doanh thu tháng, Active Agents, Lead Volume). |

---

### ⚙️ Phân hệ 10: Quản trị Hệ thống & Nhật ký (Admin System & Audit Logs)
*Khớp với: `AdminAccountsPage.jsx`, `AdminAuditLogsPage.jsx`, `AdminController.java`*

| Chức năng Backend | Method & Endpoint | Khớp với UI Frontend |
|:---|:---|:---|
| **Danh sách tài khoản** | `GET /api/admin/accounts` | Quản lý người dùng toàn hệ thống (Admin, Staff, Agent). |
| **Tạo tài khoản mới** | `POST /api/admin/accounts` | Admin cấp tài khoản cho nhân viên / đại lý mới. |
| **Cập nhật tài khoản** | `PUT /api/admin/accounts/{id}` | Phân quyền, đổi trạng thái kích hoạt tài khoản. |
| **Nhật ký hệ thống (Audit Logs)** | `GET /api/admin/audit-logs` | Bảng theo dõi lịch sử thao tác nhạy cảm của người dùng. |

---

### 🩺 Phân hệ 11: Kiểm tra Sức khỏe & Dữ liệu mẫu (DevOps & Testing)
*Khớp với: `HealthController.java`, `SeedController.java`*

| Chức năng Backend | Method & Endpoint | Mục đích |
|:---|:---|:---|
| **Health Check** | `GET /api/health` | Kiểm tra tình trạng kết nối Database và Server online. |
| **Seed Data** | `POST /api/seed` | Nạp sẵn 4 tài khoản mẫu và dữ liệu demo cho toàn bộ bảng. |

---

## 2. Bảng Đối Chiếu 39 API Endpoints (FE ↔ BE)

| # | Tên hàm trong `api.js` (FE) | HTTP Method | Backend Spring Boot Endpoint | Controller tương ứng |
|:---|:---|:---|:---|:---|
| 1 | `checkBackendHealth()` | `GET` | `/api/health` | `HealthController` |
| 2 | `login(credentials)` | `POST` | `/api/auth/login` | `AuthController` |
| 3 | `logout()` | `POST` | `/api/auth/logout` | `AuthController` |
| 4 | `getContacts(params)` | `GET` | `/api/contacts` | `ContactController` |
| 5 | `getContact(id)` | `GET` | `/api/contacts/{id}` | `ContactController` |
| 6 | `createContact(data)` | `POST` | `/api/contacts` | `ContactController` |
| 7 | `addContactNote(id, data)` | `POST` | `/api/contacts/{id}/notes` | `ContactController` |
| 8 | `addContactTask(id, data)` | `POST` | `/api/contacts/{id}/tasks` | `ContactController` |
| 9 | `addContactActivity(id, data)` | `POST` | `/api/contacts/{id}/activities` | `ContactController` |
| 10 | `getDeals(params)` | `GET` | `/api/deals` | `DealController` |
| 11 | `getDeal(id)` | `GET` | `/api/deals/{id}` | `DealController` |
| 12 | `updateDeal(id, data)` | `PUT` | `/api/deals/{id}` | `DealController` |
| 13 | `updateDealAdmin(id, data)` | `PUT` | `/api/deals/{id}/admin` | `DealController` |
| 14 | `getDocument(id)` | `GET` | `/api/documents/{id}` | `DocumentController` |
| 15 | `addDocumentFile(docId, file)` | `POST` | `/api/documents/{docId}/files` | `DocumentController` |
| 16 | `deleteDocumentFile(docId, fileId)` | `DELETE` | `/api/documents/{docId}/files/{fileId}` | `DocumentController` |
| 17 | `getTickets(params)` | `GET` | `/api/tickets` | `TicketController` |
| 18 | `getTicket(id)` | `GET` | `/api/tickets/{id}` | `TicketController` |
| 19 | `createTicket(data)` | `POST` | `/api/tickets` | `TicketController` |
| 20 | `updateTicket(id, data)` | `PUT` | `/api/tickets/{id}` | `TicketController` |
| 21 | `addTicketComment(id, data)` | `POST` | `/api/tickets/{id}/comments` | `TicketController` |
| 22 | `getTasks(params)` | `GET` | `/api/tasks` | `TaskController` |
| 23 | `getTask(id)` | `GET` | `/api/tasks/{id}` | `TaskController` |
| 24 | `createTask(data)` | `POST` | `/api/tasks` | `TaskController` |
| 25 | `updateTask(id, data)` | `PUT` | `/api/tasks/{id}` | `TaskController` |
| 26 | `getCommissions(params)` | `GET` | `/api/commissions` | `CommissionController` |
| 27 | `getCommissionSummary(agent)` | `GET` | `/api/commissions/summary` | `CommissionController` |
| 28 | `createCommission(data)` | `POST` | `/api/commissions` | `CommissionController` |
| 29 | `updateCommission(id, data)` | `PUT` | `/api/commissions/{id}` | `CommissionController` |
| 30 | `calculateCommissions()` | `POST` | `/api/commissions/calculate` | `CommissionController` |
| 31 | `getDashboardStats()` | `GET` | `/api/dashboard/stats` | `DashboardController` |
| 32 | `getAdminStats()` | `GET` | `/api/admin/stats` | `AdminController` |
| 33 | `getAdminAccounts()` | `GET` | `/api/admin/accounts` | `AdminController` |
| 34 | `createAdminAccount(data)` | `POST` | `/api/admin/accounts` | `AdminController` |
| 35 | `updateAdminAccount(id, data)` | `PUT` | `/api/admin/accounts/{id}` | `AdminController` |
| 36 | `getAdminQuotes()` | `GET` | `/api/admin/quotes` | `AdminController` |
| 37 | `assignAdminQuote(id, data)` | `PUT` | `/api/admin/quotes/{id}/assign` | `AdminController` |
| 38 | `getAdminAuditLogs()` | `GET` | `/api/admin/audit-logs` | `AdminController` |
| 39 | `resetAndSeedDatabase()` | `POST` | `/api/seed` | `SeedController` |

---

## 3. Tài Khoản Demo Mặc Định

Dữ liệu đã được nạp sẵn khi gọi `/api/seed`:

| Role | Email | Mật khẩu | Phạm vi quyền |
|:---|:---|:---|:---|
| **Admin** | `admin@insurmatch.us` | `Admin@123` | Quản trị toàn hệ thống Agency, cấu hình tài khoản & audit logs |
| **Staff** | `staff@insurmatch.us` | `Staff@123` | Nhân viên CSKH, hỗ trợ xử lý Ticket hậu mãi, kiểm tra chứng từ |
| **Agent** | `agent@insurmatch.us` | `Agent@123` | Đại lý bảo hiểm quản lý khách hàng, cơ hội bán hàng (Deal), theo dõi hoa hồng |
| **Manager** | `manager@insurmatch.us` | `Manager@123` | Quản lý đội ngũ đại lý và theo dõi báo cáo chỉ số KPI |

---

## 4. Hướng Dẫn Chạy Môi Trường Local

```bash
# 1. Khởi động PostgreSQL qua Docker
docker compose up -d

# 2. Chạy ứng dụng Spring Boot
./mvnw spring-boot:run

# 3. Khởi tạo dữ liệu mẫu lần đầu
curl -X POST http://localhost:8080/api/seed

# 4. Kiểm tra sức khỏe hệ thống
curl http://localhost:8080/api/health
```

---

## 5. Thông Tin Triển Khai Render (Production)

- **API Base URL:** `https://insurmatch-api.onrender.com`
- **Database:** PostgreSQL Cloud trên Render (`insurmatch-db`)
- **Health Check:** `https://insurmatch-api.onrender.com/api/health`
- **Frontend Config:**
  ```env
  VITE_API_URL=https://insurmatch-api.onrender.com/api
  ```

---

## 6. Ghi Chú Chi Tiết Theo Từng Nhóm API (`api.js`)

### 1. Cấu hình cơ bản & Kiểm tra kết nối (Lines 1–45)
- `API_BASE`: Nhận `VITE_API_URL` hoặc mặc định `/api`.
- `request(endpoint, options)`: Helper chuẩn hóa fetch, tự động parse JSON và xử lý HTTP error status.
- `checkBackendHealth()`: `GET /api/health` — kiểm tra trạng thái Backend và Database.

### 2. Quản lý Khách hàng & Tương tác (Lines 48–66 & 107–126)
- `getContacts(params)`: `GET /api/contacts?search=&owner=` — Tìm kiếm và lọc danh sách Contact theo Owner.
- `getContact(id)`: `GET /api/contacts/:id` — Lấy chi tiết hồ sơ 360° của khách hàng.
- `createContact(data)`: `POST /api/contacts` — Thêm khách hàng mới.
- **Tương tác trực tiếp trên hồ sơ khách hàng:**
  - `addContactNote(contactId, data)`: `POST /api/contacts/:id/notes` — Lưu Pin Note/Ghi chú.
  - `addContactTask(contactId, data)`: `POST /api/contacts/:id/tasks` — Tạo Task trực tiếp cho Contact.
  - `addContactActivity(contactId, data)`: `POST /api/contacts/:id/activities` — Ghi nhận Call Log / Meeting / Email.

### 3. Quản lý Hợp đồng & Gói bảo hiểm (Deals) (Lines 68–86 & 266–271)
- `getDeals(params)`: `GET /api/deals?search=&stage=&pipeline=` — Lấy danh sách hợp đồng cho Kanban board hoặc Table.
- `getDeal(id)`: `GET /api/deals/:id` — Xem chi tiết hợp đồng bảo hiểm.
- `updateDeal(id, data)`: `PUT /api/deals/:id` — Cập nhật thông tin gói, chuyển Deal Stage.
- `updateDealAdmin(id, adminData)`: `PUT /api/deals/:id/admin` — Cập nhật thông tin quản trị hợp đồng (NPN, Broker Effective Date...).

### 4. Quản lý Tệp tin & Chứng từ (Documents) (Lines 89–104)
- `getDocument(id)`: `GET /api/documents/:id` — Lấy thông tin tài liệu.
- `addDocumentFile(docId, fileData)`: `POST /api/documents/:docId/files` — Thêm tệp chứng từ (SSN, Thẻ xanh, Bill thanh toán, LOE...).
- `deleteDocumentFile(docId, fileId)`: `DELETE /api/documents/:docId/files/:fileId` — Xóa tệp.

### 5. Chăm sóc Khách hàng & Vé Hỗ trợ (Tickets) (Lines 136–161)
- `getTickets(params)`: `GET /api/tickets?pipeline=&status=&priority=&contactId=&dealId=` — Lọc theo các Pipelines dịch vụ khách hàng (Claim Bill, Payment, Collect Document, Choose Doctor).
- `getTicket(id)`: `GET /api/tickets/:id` — Xem chi tiết Ticket sự vụ.
- `createTicket(data)`: `POST /api/tickets` — Mở Ticket hỗ trợ khách hàng mới.
- `updateTicket(id, data)`: `PUT /api/tickets/:id` — Cập nhật tiến độ giải quyết và đổi Due Date.
- `addTicketComment(ticketId, data)`: `POST /api/tickets/:id/comments` — Thêm bình luận trao đổi xử lý trong Ticket.

### 6. Quản lý Nhiệm vụ (Tasks) (Lines 164–185)
- `getTasks(params)`: `GET /api/tasks?status=&priority=&assignedTo=&contactId=&dealId=` — Danh sách Todo theo người phụ trách và mức độ ưu tiên.
- `getTask(id)`: `GET /api/tasks/:id` — Chi tiết công việc.
- `createTask(data)`: `POST /api/tasks` — Giao nhiệm vụ mới.
- `updateTask(id, data)`: `PUT /api/tasks/:id` — Cập nhật trạng thái (OPEN ➔ COMPLETED).

### 7. Quản lý Hoa hồng Đại lý (Commissions) (Lines 188–213)
- `getCommissions(params)`: `GET /api/commissions?agentName=&period=&status=&carrier=` — Bảng danh sách hoa hồng đại lý.
- `getCommissionSummary(agentName)`: `GET /api/commissions/summary?agentName=` — Thống kê tổng hoa hồng theo đại lý.
- `createCommission(data)`: `POST /api/commissions` — Thêm bản ghi hoa hồng đối soát từ hãng.
- `updateCommission(id, data)`: `PUT /api/commissions/:id` — Cập nhật trạng thái duyệt / chi trả.
- `calculateCommissions(data)`: `POST /api/commissions/calculate` — Quyết toán hoa hồng cho đại lý.

### 8. Thống kê & Quản trị Hệ thống (Admin & Stats) (Lines 216–280)
- `getDashboardStats()`: `GET /api/dashboard/stats` — Thống kê KPI chung cho Staff/Dashboard.
- `getAdminStats()`: `GET /api/admin/stats` — Chỉ số hệ thống dành riêng cho Admin.
- `getAdminAccounts()` / `createAdminAccount()` / `updateAdminAccount()`: Quản lý CRUD tài khoản hệ thống (Staff, Agent, Admin).
- `getAdminQuotes()` / `assignAdminQuote(id, data)`: Tiếp nhận và gán Lead (QuoteRequest) cho Agent phụ trách.
- `getAdminAuditLogs()`: `GET /api/admin/audit-logs` — Nhật ký kiểm toán hệ thống.
- `resetAndSeedDatabase()`: `POST /api/seed` — Reset và nạp dữ liệu mẫu ban đầu.
