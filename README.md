# 🏠 HostelCare — Campus Hostel Complaint & Facility Management System

HostelCare is a full-stack, enterprise-grade hostel management and complaint resolution platform built for universities and educational institutions. It seamlessly connects **Students**, **Hostel Staff**, and the **Chief Warden** into a unified, transparent workflow powered by automated AI issue triage.

---

## 🌟 Key Highlights & Features

- 🎓 **Three Specialized Portals:**
  - **Student Portal:** Report hostel complaints with photo references, track real-time resolution progress via ticket IDs (`HC-XXXXXX`), receive push updates, and rate completed maintenance work.
  - **Single Chief Warden Portal:** Campus-wide administrative jurisdiction across all 20 hostels (**BH-1 to BH-10** and **GH-1 to GH-10**). Monitor campus-wide analytics, assign staff members to open complaints, and oversee facility maintenance.
  - **Staff Portal:** Field technicians and maintenance personnel view assigned work orders, update task statuses (Assigned → In Progress → Resolved), and provide resolution remarks.
- 🤖 **Resilient AI Triage Pipeline:**
  - Instant category classification, priority assignment (Urgent, High, Medium, Low), turnaround estimation, and automated executive summarization.
  - **High-availability multi-model failover:** Primary provider **Google Gemini** (`gemini-1.5-flash` / `gemini-pro`) with automated fallback to **OpenRouter** (`deepseek-chat` / `meta-llama`).
  - **Idempotent persistence:** Completed AI analyses are stored persistently in the canonical database—avoiding redundant API calls unless explicitly retried by an administrator.
- 🏛️ **Campus Jurisdiction & Gender-Aware Scoping:**
  - Gender-filtered hostel assignment during student registration (Boys: `BH-1` through `BH-10`; Girls: `GH-1` through `GH-10`).
  - Flexible manual block identifiers accommodating diverse campus architectures.
  - Unified Chief Warden architecture governing all campus accommodations under a single administrative umbrella.
- 🛡️ **Adaptive UI & Safe Complaint Lifecycle Actions:**
  - Instant ticket ID copying to Android clipboard and in-place complaint details refresh without re-running AI assessment.
  - Safe student complaint withdrawal workflow restricting cancellations to unassigned, submitted requests while preserving full timeline history and AI triage metadata.
  - Streamlined portal header layouts with redundant profile icons removed in favor of persistent bottom navigation.
- 🗄️ **Single Canonical Database (`HostelCareDB`):**
  - All roles, complaints, and operational data are housed exclusively in `HostelCareDB` on MongoDB Atlas.
  - Strictly retired legacy test databases for total consistency across development, staging, and production.

---

## 🏗️ Architecture & Tech Stack

```text
┌─────────────────────────────────────────────────────────────┐
│                   HostelCare Android App                    │
│      (Kotlin, Jetpack Compose, Material 3, Coroutines)       │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTPS / REST API
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   HostelCare Node.js API                    │
│             (Express, Mongoose, JWT, Bcrypt)                │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
               ▼                               ▼
┌─────────────────────────────┐ ┌─────────────────────────────┐
│      MongoDB Atlas          │ │      AI Triage Engine       │
│      (HostelCareDB)         │ │  Gemini 1.5 ──► OpenRouter  │
└─────────────────────────────┘ └─────────────────────────────┘
```

| Component | Technologies & Frameworks | Description |
|---|---|---|
| **Android Client** | Kotlin, Jetpack Compose, Navigation Compose, Material 3 | Modern declarative Android UI with MVVM architecture, StateFlow, and Coroutines |
| **Networking & API** | Retrofit 2, OkHttp 3, Gson, Coil | Resilient HTTP networking with auth token interceptors and asynchronous image caching |
| **Backend Runtime** | Node.js (v18+), Express.js | High-throughput asynchronous REST API server |
| **Database & ODM** | MongoDB Atlas, Mongoose | Cloud-native document storage using canonical database `HostelCareDB` |
| **Authentication** | JWT (JSON Web Tokens), Bcrypt.js | Stateless authentication with cryptographic password hashing and role-based access control (RBAC) |
| **AI Assessment** | Google Gemini SDK, OpenRouter REST API | Multi-provider fallback engine for complaint categorization, severity scoring, and summary generation |
| **Cloud Hosting** | Render Web Services | Auto-deploying production backend with SSL/TLS termination |

---

## 📁 Repository Structure

```text
HostelCare/
├── app/                                # Android Application
│   ├── src/main/
│   │   ├── java/com/hostelcare/app/
│   │   │   ├── data/                   # Models, Retrofit API services, Repositories
│   │   │   ├── ui/
│   │   │   │   ├── components/         # Shared Jetpack Compose components
│   │   │   │   ├── navigation/         # NavHost and App Routes
│   │   │   │   ├── screens/
│   │   │   │   │   ├── auth/           # Login, Register, Role selector
│   │   │   │   │   ├── student/        # Complaint form, Student dashboard, Tracking
│   │   │   │   │   ├── warden/         # Chief Warden dashboard, Staff mgmt, Analytics
│   │   │   │   │   └── staff/          # Staff work orders, Task completion
│   │   │   │   └── theme/              # Color palettes, typography, theme definition
│   │   │   └── MainActivity.kt         # Entry Activity with splash screen
│   │   └── res/                        # Drawables, mipmaps, values
│   └── build.gradle.kts                # Android build configuration & dependencies
├── backend/                            # Node.js Express Backend
│   ├── middleware/
│   │   └── auth.js                     # JWT validation and role authorization
│   ├── models/
│   │   ├── User.js                     # Unified user schema (Student, Warden, Staff)
│   │   ├── Complaint.js                # Complaint schema with AI metadata & history
│   │   └── Notification.js             # User alert notifications
│   ├── routes/
│   │   ├── auth.js                     # Register, Login, Verification
│   │   ├── profile.js                  # User profile retrieval and updates
│   │   ├── complaints.js               # Complaint lifecycle, filtering, AI analysis
│   │   ├── warden.js                   # Chief Warden campus overview & oversight
│   │   └── staff.js                    # Staff provisioning, tasks, work orders
│   ├── services/
│   │   └── aiService.js                # Gemini + OpenRouter failover logic
│   ├── tests/                          # Automated backend test suites
│   ├── server.js                       # Express application bootstrap
│   └── package.json
└── README.md
```

---

## 🔌 Core API Endpoints

### 🔐 Authentication & Profile

| Method | Endpoint | Authorization | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register a new Student account (with gender, hostel, block validation) |
| `POST` | `/api/auth/login` | Public | Authenticate user and receive JWT bearer token + profile summary |
| `GET` | `/api/profile` | Bearer Token | Fetch the authenticated user's profile details |
| `PUT` | `/api/profile` | Bearer Token | Update contact and profile preferences |

### 📝 Complaints & Lifecycle

| Method | Endpoint | Authorization | Description |
|---|---|---|---|
| `POST` | `/api/complaints` | Student | Submit a new complaint (triggers automated AI assessment) |
| `GET` | `/api/complaints` | Authenticated | List complaints scoped to role (Student: own; Staff: assigned; Chief Warden: campus-wide) |
| `GET` | `/api/complaints/:id` | Authenticated | Retrieve detailed complaint information, timeline, and AI insights |
| `POST` | `/api/complaints/:id/withdraw` | Student | Safely withdraw an unassigned complaint in submitted status |
| `PUT` | `/api/complaints/:id/status` | Staff / Warden | Transition status (`SUBMITTED` → `IN_PROGRESS` → `RESOLVED`) |
| `POST` | `/api/complaints/:id/assign` | Chief Warden | Assign an open complaint to a designated staff member |
| `POST` | `/api/complaints/:id/retry-ai`| Chief Warden | Force re-evaluation of complaint by AI assessment engine |

### 👷 Staff & Campus Administration

| Method | Endpoint | Authorization | Description |
|---|---|---|---|
| `GET` | `/api/staff` | Chief Warden | Retrieve list of all maintenance staff across hostels |
| `POST` | `/api/staff` | Chief Warden | Provision a new staff member for a specific department and hostel |
| `GET` | `/api/warden/overview` | Chief Warden | Campus-wide complaint statistics across all 20 hostels |

---

## ⚙️ Environment Configuration

Create a `.env` file inside the `backend/` directory:

```env
# Server
PORT=5000
NODE_ENV=development

# Database (Canonical)
MONGO_URI=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/HostelCareDB?retryWrites=true&w=majority

# Security
JWT_SECRET=your_super_secret_jwt_signing_key_here

# AI Assessment Providers
GEMINI_API_KEY=your_google_gemini_api_key
OPENROUTER_API_KEY=your_openrouter_api_key
```

> [!IMPORTANT]
> Never commit `.env` or sensitive credentials to version control. The repository `.gitignore` automatically prevents `.env` and credential files from being tracked.

---

## 🚀 Getting Started

### 1. Backend Server

```bash
cd backend
npm install
npm run dev      # Starts development server with nodemon on port 5000
```

To run test suites:

```bash
# Run role & authorization regression tests
node tests/run_tests.js

# Run AI engine failover tests
node tests/test_ai_failover.js
```

### 2. Android Application

1. Open the project root in **Android Studio Ladybug (or newer)**.
2. Ensure JDK 17 or JDK 21 is configured as the Gradle JDK.
3. Synchronize Gradle files and build the project:
   ```bash
   ./gradlew testDebugUnitTest    # Run unit tests
   ./gradlew assembleDebug        # Build debug APK
   ```
4. Connect a physical Android device (via USB debugging) or start an Android Emulator.
5. Deploy and run the `app` configuration.

---

## 🏛️ Campus Hostels Reference

HostelCare supports 20 institutional residence halls across campus:

| Boys' Hostels (BH) | Girls' Hostels (GH) |
|:---:|:---:|
| `BH-1` · `BH-2` · `BH-3` · `BH-4` · `BH-5` | `GH-1` · `GH-2` · `GH-3` · `GH-4` · `GH-5` |
| `BH-6` · `BH-7` · `BH-8` · `BH-9` · `BH-10` | `GH-6` · `GH-7` · `GH-8` · `GH-9` · `GH-10` |

---

## 📄 License

This project is developed for institutional hostel facility management and academic research. All rights reserved.
