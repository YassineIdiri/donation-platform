# Donation Platform — Full-Stack (Spring Boot + Angular)

A production-ready donation platform featuring Stripe payments, a secure admin back-office, automated CI, PDF tax receipts, and a clean Angular front-end. Built with a strong focus on **security**, **developer experience**, and **real-world deployment constraints** (cloud hosting, env configs, CI pipelines).

---

## ✨ Highlights

- **Stripe Checkout** payments (test mode) + webhook handling
- **Tax receipt generation (PDF)** with PDFBox (server-side)
- **Admin back-office** secured with JWT (refresh/logout)
- **CI-ready**: GitHub Actions for backend + frontend (tests, build)
- **PostgreSQL** + Flyway migrations
- Environment-based configuration (local/CI/prod)
- Clean layered architecture: controller → service → repository

---

## 🧱 Tech Stack

### Backend
- Java **21**
- Spring Boot **4**
- Spring Security (JWT)
- Spring Data JPA + Hibernate
- PostgreSQL
- Flyway
- PDFBox (PDF receipts)

### Frontend
- Angular
- RxJS
- Responsive UI
- API integration with proper error handling

### DevOps
- GitHub Actions (CI)
- Render deployment (backend)
- (Optional) Custom domain for frontend

---

## 🔐 Security Overview

- Admin endpoints protected by a **JWT auth filter**
- Access restricted to the configured admin email (`AdminProps`)
- Token parsing & validation handled in `JwtService`
- Stateless authentication (no server sessions)

> Public donation flow is accessible without admin auth; admin APIs live under `/api/admin/**`.

---

## 🧾 Tax Receipt (PDF)

After a successful payment, users can request a **tax receipt**:

- Generates a PDF on the server (PDFBox)
- Stores receipt metadata in DB (`TaxReceipt`)
- Supports a download endpoint (and optional email sending)

⚠️ Note: On some cloud platforms (including certain Render setups), **SMTP outbound connections can be blocked**.  
This project therefore supports a reliable **“generate & download directly”** approach for demos.

---

## 🗂️ Project Structure (Backend)

```text
src/main/java/com/yassine/donationplatform
├── controller
│   ├── publicapi
│   └── admin
├── security
│   ├── jwt
│   └── admin
├── service
│   ├── donation
│   ├── receipt
│   └── auth
├── repository
└── entity
