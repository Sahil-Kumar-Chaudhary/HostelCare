# 🏠 HostelCare Backend

Backend API for **HostelCare**, a student hostel complaint management Android application. 📱

HostelCare is designed to make hostel issue reporting and resolution easier by connecting students with hostel staff through a secure backend. 🤝 The backend handles authentication, student profiles, and persistent data using **Node.js, Express, MongoDB Atlas, and JWT authentication**.

> **🚧 Project status:** Authentication and profile APIs are currently implemented. Complaint, notification, feedback, and AI services can be added as the project progresses.

## ✨ Features

- 📝 Student registration
- 🔐 Secure password hashing with bcrypt
- 🔑 Student login
- 🛡️ JWT-based authentication
- 🔒 Protected API routes
- 👤 Get logged-in student profile
- ✏️ Update student profile
- ☁️ MongoDB Atlas persistence
- 🌐 CORS support
- ⚙️ Environment-based configuration
- 🚀 Ready for deployment on Render

## 🧰 Tech Stack

| Technology | Purpose |
|---|---|
| Node.js | Backend runtime |
| Express.js | REST API framework |
| MongoDB Atlas | Cloud database |
| Mongoose | MongoDB ODM |
| JWT | Authentication |
| bcryptjs | Password hashing |
| dotenv | Environment variables |
| CORS | Cross-origin API access |

## 📁 Project Structure

```text
backend/
├── middleware/
│   └── auth.js
├── models/
│   └── User.js
├── routes/
│   ├── auth.js
│   └── profile.js
├── .env
├── .gitignore
├── package.json
├── package-lock.json
└── server.js
```

## 🔌 API Endpoints

### 🔐 Authentication

#### 📝 Register

```http
POST /api/auth/register
```

Example request:

```json
{
  "name": "Test Student",
  "studentId": "TEST1001",
  "email": "teststudent@example.com",
  "password": "Test@12345",
  "hostelBlock": "Block B",
  "roomNumber": "B-204",
  "phone": "9876543210"
}
```

#### 🔑 Login

```http
POST /api/auth/login
```

Example request:

```json
{
  "email": "teststudent@example.com",
  "password": "Test@12345"
}
```

The successful response returns a JWT token and basic user information.

### 👤 Profile

The following endpoints require a JWT token in the request header:

```http
Authorization: Bearer <token>
```

#### 📄 Get Profile

```http
GET /api/profile
```

#### ✏️ Update Profile

```http
PUT /api/profile
```

Example request:

```json
{
  "name": "Test Student Updated",
  "email": "teststudent@example.com",
  "hostelBlock": "Block A",
  "roomNumber": "A-101",
  "phone": "9999999999"
}
```

### 🛡️ Protected Route

A development test route is currently available:

```http
GET /api/protected
```

It requires a valid JWT token.

## 🛠️ Local Setup

### 1️⃣ Clone the repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd HostelCare-backend
```

### 2️⃣ Install dependencies

```bash
npm install
```

### 3️⃣ Create environment variables

Create a `.env` file in the backend root:

```env
MONGO_URI=your_mongodb_atlas_connection_string
PORT=5000
JWT_SECRET=your_long_random_secret
```

⚠️ Never commit `.env` to GitHub.

### 4️⃣ Start the server

Development:

```bash
npm run dev
```

Production-style local start:

```bash
npm start
```

The API will run on: 🌐

```text
http://localhost:5000
```

## 📱 Android Emulator

When the Android app is running on the Android Emulator and the backend is running on the development PC, use:

```text
http://10.0.2.2:5000/
```

Do not use `localhost` from inside the Android Emulator to reach the PC backend.

## 🗄️ Database

HostelCare uses MongoDB Atlas for cloud database storage.

The current user model stores:

- 👤 Name
- 🆔 Student ID
- 📧 Email
- 🔐 Hashed password
- 🏢 Hostel / Block
- 🚪 Room number
- 📞 Phone
- 🖼️ Profile photo reference
- 👥 User role
- 🕒 Created/updated timestamps

Passwords are hashed before being stored. 🔐

## 🔒 Security

The backend follows several basic security practices:

- 🔐 Passwords are hashed with bcryptjs.
- 🛡️ JWTs are used for protected routes.
- 🔒 Protected routes verify the `Authorization: Bearer <token>` header.
- 🚫 Passwords are excluded from profile responses.
- 🔑 MongoDB credentials and JWT secrets are stored in environment variables.
- 🚫 `.env` and `node_modules` are excluded from Git.

## 🗺️ Planned Modules

```text
Authentication
    ├── 📝 Register
    └── 🔑 Login

Student Profile
    ├── 👤 View Profile
    └── ✏️ Update Profile

Complaints
    ├── 📝 Create Complaint
    ├── 📋 View Complaints
    ├── 🔎 Complaint Details
    ├── 📊 Status Tracking
    └── 👷 Staff Assignment

AI Assistance
    ├── 🧠 Category Detection
    ├── 🚨 Priority Detection
    └── 📝 Complaint Summary

Notifications
    ├── 🔔 Complaint Updates
    └── ✅ Resolution Notifications

Feedback
    └── ⭐ Resolution Rating / Feedback
```

## 🚀 Deployment

The backend is designed to be deployed as a Node.js Web Service on **Render** and connected to **MongoDB Atlas**. ☁️

For Render, use:

```text
Build Command: npm install
Start Command: npm start
```

Set the following environment variables in Render:

```text
MONGO_URI
JWT_SECRET
```

The `PORT` should be provided by the hosting platform through the environment.

## 💻 Development

Useful commands:

```bash
npm install
npm run dev
npm start
```

Before deployment, verify that:

- ✅ `.env` is not committed
- ✅ the server starts successfully
- ✅ MongoDB Atlas connection works
- ✅ authentication endpoints work
- ✅ protected routes reject missing/invalid tokens

## 🎯 Project Goal

HostelCare aims to provide a simple and reliable way for students to report hostel problems, track their complaints, and receive updates while helping hostel staff manage and resolve issues efficiently.

## 📜 License

This project is developed for academic/project purposes.
