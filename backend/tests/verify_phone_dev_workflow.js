const assert = require("assert");
const http = require("http");
const mongoose = require("mongoose");
const jwt = require("jsonwebtoken");
const path = require("path");
require("dotenv").config({ path: path.join(__dirname, "../.env") });

const BASE_URL = "http://127.0.0.1:5000";

function makeRequest(method, endpoint, body = null, token = null) {
  return new Promise((resolve, reject) => {
    const url = new URL(endpoint, BASE_URL);
    const options = {
      hostname: url.hostname,
      port: url.port,
      path: url.pathname + url.search,
      method: method,
      headers: {
        "Content-Type": "application/json"
      }
    };
    if (token) {
      options.headers["Authorization"] = `Bearer ${token}`;
    }

    const req = http.request(options, (res) => {
      let data = "";
      res.on("data", (chunk) => (data += chunk));
      res.on("end", () => {
        let json = null;
        try {
          json = JSON.parse(data);
        } catch (e) {
          json = data;
        }
        resolve({ status: res.statusCode, data: json });
      });
    });

    req.on("error", reject);

    if (body) {
      req.write(JSON.stringify(body));
    }
    req.end();
  });
}

async function runVerification() {
  console.log("===============================================================");
  console.log("  HostelCare — Live Development Environment & Phone Workflow  ");
  console.log("===============================================================\n");

  const results = [];
  function record(name, passed, detail = "") {
    results.push({ name, passed, detail });
    if (passed) {
      console.log(`[PASS] ${name}`);
      if (detail) console.log(`       -> ${detail}`);
    } else {
      console.error(`[FAIL] ${name}`);
      if (detail) console.error(`       -> ${detail}`);
    }
  }

  // 1. Confirm local backend connects to HostelCareDevDB
  console.log("--- 1. Backend & Database Configuration ---");
  try {
    const rootRes = await makeRequest("GET", "/");
    assert.strictEqual(rootRes.status, 200, "Backend root should return 200 OK");
    assert.strictEqual(rootRes.data.message, "HostelCare backend is running");

    await mongoose.connect(process.env.MONGO_URI, { dbName: "HostelCareDevDB" });
    assert.strictEqual(mongoose.connection.name, "HostelCareDevDB", "Mongoose must connect to HostelCareDevDB");
    record("Local backend is running and connects to HostelCareDevDB", true, `DB Name: ${mongoose.connection.name}`);
  } catch (err) {
    record("Local backend is running and connects to HostelCareDevDB", false, err.message);
    process.exit(1);
  }

  // 2. Confirm Android Debug Build Configuration
  console.log("\n--- 2. Android Debug Build Configuration ---");
  try {
    const fs = require("fs");
    const gradleContent = fs.readFileSync(path.join(__dirname, "../../app/build.gradle.kts"), "utf8");
    assert(gradleContent.includes('buildConfigField("String", "BASE_URL"'), "debug BASE_URL must be defined");
    assert(!gradleContent.match(/debug\s*\{[^}]*hostelcare-backend-1r6q\.onrender\.com/), "debug must not use Render URL");

    const retrofitContent = fs.readFileSync(path.join(__dirname, "../../app/src/main/java/com/hostelcare/app/data/remote/RetrofitClient.kt"), "utf8");
    assert(retrofitContent.includes("isEmulator()"), "RetrofitClient dynamically routes physical devices vs emulator");

    const apkPath = path.join(__dirname, "../../app/build/outputs/apk/debug/app-debug.apk");
    assert(fs.existsSync(apkPath), "app-debug.apk must exist");
    const apkStats = fs.statSync(apkPath);
    record("Android debug build targets local development backend (NOT Render)", true, `APK size: ${(apkStats.size / (1024 * 1024)).toFixed(2)} MB`);
  } catch (err) {
    record("Android debug build targets local development backend (NOT Render)", false, err.message);
  }

  // 3. Test Student Registration with user-entered hostel name
  console.log("\n--- 3. Student Registration with User-Entered Hostel Name ---");
  let newStudentEmail = `phone.test.${Date.now()}@gmail.com`;
  let newStudentToken = null;
  let newStudentId = null;
  try {
    const regPayload = {
      name: "Pranav Verma",
      studentId: "12308899",
      email: newStudentEmail,
      phone: "+91 9876543211",
      collegeName: "Apex University",
      course: "B.Tech Computer Science",
      admissionYear: "2023",
      hostelName: "Campus Hostel",
      hostelBlock: "B",
      roomNumber: "314",
      password: "StudentPass123!",
      confirmPassword: "StudentPass123!",
      acceptedTerms: true,
      role: "student"
    };

    const regRes = await makeRequest("POST", "/api/auth/register", regPayload);
    assert.strictEqual(regRes.status, 201, `Registration returned status ${regRes.status}`);
    assert(regRes.data.user, "Response must include created user");
    assert.strictEqual(regRes.data.user.hostelName, "Campus Hostel");

    // Verify in MongoDB Atlas HostelCareDevDB directly
    const savedUser = await mongoose.connection.collection("users").findOne({ email: newStudentEmail });
    assert(savedUser, "User must be found in HostelCareDevDB");
    assert.strictEqual(savedUser.hostelName, "Campus Hostel", "hostelName in MongoDB must match entered value");
    assert.strictEqual(savedUser.studentId, "12308899");
    assert.strictEqual(savedUser.role, "student");
    newStudentId = savedUser._id;

    // Login with the new student
    const loginRes = await makeRequest("POST", "/api/auth/login", {
      email: newStudentEmail,
      password: "StudentPass123!",
      portal: "student"
    });
    assert.strictEqual(loginRes.status, 200);
    newStudentToken = loginRes.data.token;

    record("Student registration with user-entered hostelName persisted in HostelCareDevDB", true, `Email: ${newStudentEmail}, Hostel: ${savedUser.hostelName}`);
  } catch (err) {
    record("Student registration with user-entered hostelName persisted in HostelCareDevDB", false, err.message);
  }

  // 4. Test Editing Hostel Name for Existing Development Student (sahilku707@gmail.com)
  console.log("\n--- 4. Edit Profile for Student Lacking Hostel Name ---");
  let sahilToken = null;
  let sahilUserId = null;
  try {
    const existingStudent = await mongoose.connection.collection("users").findOne({ email: "sahilku707@gmail.com" });
    assert(existingStudent, "sahilku707@gmail.com must exist in HostelCareDevDB");
    sahilUserId = existingStudent._id;
    console.log(`       Initial state: email=${existingStudent.email}, hostelName=${existingStudent.hostelName || "NONE"}`);

    // Generate JWT token for sahilku707@gmail.com
    sahilToken = jwt.sign(
      { userId: existingStudent._id.toString(), role: "student" },
      process.env.JWT_SECRET,
      { expiresIn: "7d" }
    );

    // Call PUT /api/profile
    const updateRes = await makeRequest("PUT", "/api/profile", {
      name: "Sahil Kumar Chaudhary",
      email: "sahilku707@gmail.com",
      hostelName: "Campus Hostel",
      hostelBlock: "B",
      roomNumber: "927",
      phone: "+91 9876543200"
    }, sahilToken);

    assert.strictEqual(updateRes.status, 200, `Profile update returned ${updateRes.status}: ${JSON.stringify(updateRes.data)}`);
    assert.strictEqual(updateRes.data.user.hostelName, "Campus Hostel");

    // Verify in MongoDB Atlas HostelCareDevDB directly
    const updatedInDb = await mongoose.connection.collection("users").findOne({ _id: existingStudent._id });
    assert.strictEqual(updatedInDb.hostelName, "Campus Hostel", "hostelName in DB must be updated to Campus Hostel");
    record("Editing hostelName for existing student lacking hostelName updates MongoDB", true, `Updated sahilku707@gmail.com to hostelName: ${updatedInDb.hostelName}`);
  } catch (err) {
    record("Editing hostelName for existing student lacking hostelName updates MongoDB", false, err.message);
  }

  // 5. Submit Complaint and verify hostelName & student reference from profile
  console.log("\n--- 5. Submit Complaint & Location Verification ---");
  let createdComplaintTicketId = null;
  let createdComplaintMongoId = null;
  try {
    const complaintRes = await makeRequest("POST", "/api/complaints", {
      title: "Broken Window Latch in Room 927",
      description: "Window latch is loose and rattles in strong wind.",
      category: "furniture",
      priority: "medium",
      hostelBlock: "B",
      roomNumber: "927"
    }, sahilToken);

    assert.strictEqual(complaintRes.status, 201, `Complaint submission returned ${complaintRes.status}: ${JSON.stringify(complaintRes.data)}`);
    assert(complaintRes.data.complaint, "Response must include created complaint");
    createdComplaintTicketId = complaintRes.data.complaint.ticketId;
    createdComplaintMongoId = complaintRes.data.complaint._id;

    // Verify directly in MongoDB Atlas HostelCareDevDB
    const compInDb = await mongoose.connection.collection("complaints").findOne({ _id: new mongoose.Types.ObjectId(createdComplaintMongoId) });
    assert(compInDb, "Complaint must be found in HostelCareDevDB");
    assert.strictEqual(compInDb.hostelName, "Campus Hostel", "Complaint hostelName must match student's saved profile");
    assert.strictEqual(compInDb.student.toString(), sahilUserId.toString(), "Complaint student reference must match authenticated student ID");
    assert.strictEqual(compInDb.hostelBlock, "B");
    assert.strictEqual(compInDb.roomNumber, "927");

    record("Complaint submission populates hostelName and student from authenticated profile", true, `Ticket: ${createdComplaintTicketId}, Hostel: ${compInDb.hostelName}, StudentRef: ${compInDb.student}`);
  } catch (err) {
    record("Complaint submission populates hostelName and student from authenticated profile", false, err.message);
  }

  // 6. Log in as Development Warden and verify dashboard, list, and complaint details
  console.log("\n--- 6. Development Warden Verification ---");
  try {
    const wardenUser = await mongoose.connection.collection("users").findOne({ email: "warden@hostelcare.com" });
    assert(wardenUser, "warden@hostelcare.com must exist in HostelCareDevDB");
    assert.strictEqual(wardenUser.role, "warden");
    assert.strictEqual(wardenUser.hostelName, "Campus Hostel");

    const wardenToken = jwt.sign(
      { userId: wardenUser._id.toString(), role: "warden" },
      process.env.JWT_SECRET,
      { expiresIn: "7d" }
    );

    // 6a. GET /api/warden/dashboard
    const dashRes = await makeRequest("GET", "/api/warden/dashboard", null, wardenToken);
    assert.strictEqual(dashRes.status, 200, `Warden dashboard returned ${dashRes.status}`);
    assert(dashRes.data.success, "Dashboard success should be true");
    assert.strictEqual(dashRes.data.warden.hostelName, "Campus Hostel");
    assert(dashRes.data.stats.total >= 1, `Total complaints count (${dashRes.data.stats.total}) must be >= 1`);
    console.log(`       Dashboard stats: Total=${dashRes.data.stats.total}, Pending=${dashRes.data.stats.pending}, Resolved=${dashRes.data.stats.resolved}`);

    // 6b. GET /api/warden/complaints
    const listRes = await makeRequest("GET", "/api/warden/complaints", null, wardenToken);
    assert.strictEqual(listRes.status, 200, `Warden complaints list returned ${listRes.status}`);
    const foundInList = listRes.data.complaints.find(c => c.ticketId === createdComplaintTicketId);
    assert(foundInList, `Complaint ${createdComplaintTicketId} must be visible in warden complaint list`);

    // 6c. GET /api/warden/complaints/:id
    const detailRes = await makeRequest("GET", `/api/warden/complaints/${createdComplaintMongoId}`, null, wardenToken);
    assert.strictEqual(detailRes.status, 200, `Warden complaint detail returned ${detailRes.status}`);
    const detail = detailRes.data.complaint;
    assert.strictEqual(detail.ticketId, createdComplaintTicketId, "Ticket ID must match");
    assert.strictEqual(detail.hostelName, "Campus Hostel", "Hostel name must match");
    assert.strictEqual(detail.hostelBlock, "B", "Block must match");
    assert.strictEqual(detail.roomNumber, "927", "Room number must match");
    assert.strictEqual(detail.student.studentId, "12305507", "Student ID must match 12305507");
    assert.strictEqual(detail.student.name, "Sahil Kumar Chaudhary", "Student name must match");

    record("Warden dashboard, list, and complaint details display all fields correctly", true, `Ticket: ${detail.ticketId}, StudentId: ${detail.student.studentId}, Hostel: ${detail.hostelName}, Block: ${detail.hostelBlock}, Room: ${detail.roomNumber}`);
  } catch (err) {
    record("Warden dashboard, list, and complaint details display all fields correctly", false, err.message);
  }

  // 7. Verify Warden assigned to different hostel or block cannot access complaint
  console.log("\n--- 7. Unauthorized Warden Scoping Rejection ---");
  try {
    // 7a. Warden from a foreign hostel
    const foreignWardenToken = jwt.sign(
      { userId: new mongoose.Types.ObjectId().toString(), role: "warden" },
      process.env.JWT_SECRET,
      { expiresIn: "7d" }
    );
    // Insert temporary foreign warden in DB
    const foreignWardenId = new mongoose.Types.ObjectId();
    await mongoose.connection.collection("users").insertOne({
      _id: foreignWardenId,
      name: "Foreign Warden",
      email: "foreign.warden@hostelcare.com",
      role: "warden",
      hostelName: "Other College Hostel",
      hostelBlock: "all"
    });

    const foreignToken = jwt.sign(
      { userId: foreignWardenId.toString(), role: "warden" },
      process.env.JWT_SECRET,
      { expiresIn: "7d" }
    );

    const foreignAccessRes = await makeRequest("GET", `/api/warden/complaints/${createdComplaintMongoId}`, null, foreignToken);
    assert.strictEqual(foreignAccessRes.status, 403, `Foreign hostel warden access must return 403, got ${foreignAccessRes.status}`);
    console.log(`       Foreign warden access rejected with 403 Forbidden: ${foreignAccessRes.data.message}`);

    // Clean up temporary foreign warden
    await mongoose.connection.collection("users").deleteOne({ _id: foreignWardenId });

    // 7b. Accessing legacy complaint with no hostelName (HC-076299)
    const legacyComp = await mongoose.connection.collection("complaints").findOne({ ticketId: "HC-076299" });
    if (legacyComp) {
      const wardenUser = await mongoose.connection.collection("users").findOne({ email: "warden@hostelcare.com" });
      const wardenToken = jwt.sign(
        { userId: wardenUser._id.toString(), role: "warden" },
        process.env.JWT_SECRET,
        { expiresIn: "7d" }
      );
      const legacyAccessRes = await makeRequest("GET", `/api/warden/complaints/${legacyComp._id}`, null, wardenToken);
      assert.strictEqual(legacyAccessRes.status, 403, `Unassigned complaint access must return 403, got ${legacyAccessRes.status}`);
      console.log(`       Unassigned complaint (HC-076299) rejected with 403 Forbidden: ${legacyAccessRes.data.message}`);
    }

    record("Warden from different hostel or unassigned complaint denied with 403 Forbidden", true, "Authorization strictly enforces scoped hostel access");
  } catch (err) {
    record("Warden from different hostel or unassigned complaint denied with 403 Forbidden", false, err.message);
  }

  // Clean up test student created in test 3
  if (newStudentId) {
    await mongoose.connection.collection("users").deleteOne({ _id: newStudentId });
  }

  await mongoose.disconnect();

  console.log("\n===============================================================");
  const allPassed = results.every(r => r.passed);
  console.log(`Summary: ${results.filter(r => r.passed).length} / ${results.length} PASSED`);
  console.log("===============================================================");

  if (!allPassed) {
    process.exit(1);
  }
}

runVerification().catch(err => {
  console.error("Verification execution error:", err);
  process.exit(1);
});
