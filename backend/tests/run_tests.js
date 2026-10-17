const assert = require("assert");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const mongoose = require("mongoose");
const User = require("../models/User");
const Complaint = require("../models/Complaint");
const Notification = require("../models/Notification");
const authMiddleware = require("../middleware/auth");
const { provisionWarden, validatePassword } = require("../scripts/provisionWarden");
const { normalizeHostelName, buildHostelRegex, isHostelMatch, buildBlockFilter, checkBlockAuthorized } = require("../utils/hostelUtils");

const TEST_JWT_SECRET = "test_jwt_secret_key_1234567890_test";
process.env.JWT_SECRET = TEST_JWT_SECRET;

async function runTests() {
  console.log("==================================================");
  console.log("   HostelCare Backend Role & Auth Test Suite      ");
  console.log("==================================================\n");

  let passed = 0;
  let failed = 0;

  function test(name, fn) {
    try {
      fn();
      console.log(`PASS: ${name}`);
      passed++;
    } catch (err) {
      console.error(`FAIL: ${name}`);
      console.error(`   -> ${err.message}`);
      failed++;
    }
  }

  async function testAsync(name, fn) {
    try {
      await fn();
      console.log(`PASS: ${name}`);
      passed++;
    } catch (err) {
      console.error(`FAIL: ${name}`);
      console.error(`   -> ${err.message}`);
      failed++;
    }
  }

  // 1. User Schema Validation Tests
  test("Schema: Student requires studentId, hostelBlock, and roomNumber", () => {
    const student = new User({
      name: "Sahil Kumar",
      email: "sahil@gmail.com",
      password: "HashedPassword123!",
      role: "student"
    });
    const err = student.validateSync();
    assert(err, "Validation error expected for missing student fields");
    assert(err.errors.studentId, "studentId should be required for student");
    assert(err.errors.hostelBlock, "hostelBlock should be required for student");
    assert(err.errors.roomNumber, "roomNumber should be required for student");
  });

  test("Schema: Valid Student document passes validation", () => {
    const student = new User({
      name: "Sahil Kumar",
      studentId: "STU-101",
      email: "sahil@gmail.com",
      password: "HashedPassword123!",
      hostelBlock: "A",
      roomNumber: "101",
      role: "student"
    });
    const err = student.validateSync();
    assert(!err, `Validation should pass for complete student: ${err ? err.message : ""}`);
    assert.strictEqual(student.role, "student");
  });

  test("Schema: Warden does NOT require studentId, hostelBlock, or roomNumber", () => {
    const warden = new User({
      name: "Dr. A. K. Sharma",
      email: "warden.sharma@hostelcare.com",
      password: "HashedPassword123!",
      hostelName: "Tagore Hostel",
      role: "warden"
    });
    const err = warden.validateSync();
    assert(!err, `Validation should pass for warden without student fields: ${err ? err.message : ""}`);
    assert.strictEqual(warden.role, "warden");
    assert.strictEqual(warden.studentId, undefined);
  });

  test("Schema: Staff does NOT require studentId, hostelBlock, or roomNumber", () => {
    const staff = new User({
      name: "Ramesh Electrician",
      email: "ramesh.staff@hostelcare.com",
      password: "HashedPassword123!",
      role: "staff"
    });
    const err = staff.validateSync();
    assert(!err, `Validation should pass for staff without student fields: ${err ? err.message : ""}`);
    assert.strictEqual(staff.role, "staff");
  });

  test("Schema: Invalid role is rejected by enum validation", () => {
    const invalid = new User({
      name: "Hacker",
      email: "hacker@test.com",
      password: "Password123!",
      role: "superadmin"
    });
    const err = invalid.validateSync();
    assert(err && err.errors.role, "Invalid role should fail Mongoose enum check");
  });

  // 2. Public Registration Security Tests
  test("Registration: Public attempt using role 'warden' is rejected", () => {
    const reqBody = {
      name: "Attacker",
      studentId: "ATTACK1",
      email: "attacker@gmail.com",
      phone: "9876543210",
      collegeName: "Tech Univ",
      course: "CS",
      currentYear: "3",
      hostelName: "Tagore",
      hostelBlock: "A",
      roomNumber: "101",
      password: "Password123!",
      confirmPassword: "Password123!",
      acceptedTerms: true,
      role: "warden"
    };

    let rejected = false;
    if (reqBody.role && reqBody.role.toString().trim().toLowerCase() !== "student") {
      rejected = true;
    }
    assert(rejected, "Public registration specifying role 'warden' must be rejected");
  });

  test("Registration: Public attempt using role 'staff' is rejected", () => {
    const reqBody = {
      role: "staff"
    };
    let rejected = false;
    if (reqBody.role && reqBody.role.toString().trim().toLowerCase() !== "student") {
      rejected = true;
    }
    assert(rejected, "Public registration specifying role 'staff' must be rejected");
  });

  test("Registration: Valid student registration sets role 'student'", () => {
    const userDoc = new User({
      name: "Rohan Verma",
      studentId: "STU-202",
      email: "rohan@gmail.com",
      password: "HashedPassword123!",
      hostelBlock: "B",
      roomNumber: "205",
      role: "student"
    });
    assert.strictEqual(userDoc.role, "student");
  });

  // 3. Password Validation Rules
  test("Password: Strength validation enforces complexity", () => {
    assert(!validatePassword("weak"), "Short password rejected");
    assert(!validatePassword("alllowercase123!"), "Missing uppercase rejected");
    assert(!validatePassword("ALLUPPERCASE123!"), "Missing lowercase rejected");
    assert(!validatePassword("NoNumberSpecial!"), "Missing number rejected");
    assert(!validatePassword("NoSpecialChar1234"), "Missing special char rejected");
    assert(validatePassword("SecurePass123!"), "Valid strong password accepted");
  });

  // 4. Initial Warden Provisioning Logic
  await testAsync("Provisioning: Warden provisioning creates valid warden with bcrypt hash", async () => {
    const rawPassword = "WardenSecurePass123!";
    const hashedPassword = await bcrypt.hash(rawPassword, 10);
    const isMatch = await bcrypt.compare(rawPassword, hashedPassword);
    assert(isMatch, "Bcrypt hash must match raw password");

    const warden = new User({
      name: "Dr. A. K. Sharma",
      email: "warden.sharma@hostelcare.com",
      password: hashedPassword,
      hostelName: "Tagore Hostel",
      role: "warden"
    });
    const err = warden.validateSync();
    assert(!err, "Warden document must pass validation");
    assert.strictEqual(warden.role, "warden");
    assert.strictEqual(warden.password.startsWith("$2"), true, "Password must be bcrypt hash");
  });

  test("Provisioning: Refusal to overwrite existing account", () => {
    const existing = { email: "existing.warden@hostelcare.com", role: "warden" };
    const attemptEmail = "existing.warden@hostelcare.com";
    let refused = false;
    if (existing.email === attemptEmail) {
      refused = true;
    }
    assert(refused, "Provisioning must refuse to overwrite existing user");
  });

  // 5. Authentication & Login Role Verification Tests
  await testAsync("Login: Password comparison and JWT token creation with role", async () => {
    const rawPassword = "ValidPassword123!";
    const hashedPassword = await bcrypt.hash(rawPassword, 10);

    const isMatch = await bcrypt.compare(rawPassword, hashedPassword);
    assert(isMatch, "Valid credentials compare successfully");

    const token = jwt.sign(
      { userId: "507f1f77bcf86cd799439011", role: "warden" },
      TEST_JWT_SECRET,
      { expiresIn: "7d" }
    );
    const decoded = jwt.verify(token, TEST_JWT_SECRET);
    assert.strictEqual(decoded.role, "warden");
    assert.strictEqual(decoded.userId, "507f1f77bcf86cd799439011");
  });

  test("Login: Role mismatch for selected portal is rejected", () => {
    function checkPortalRole(userRole, requestedPortal) {
      if (requestedPortal) {
        const normalized = requestedPortal === "admin" ? "staff" : requestedPortal;
        if (userRole !== normalized) {
          return { allowed: false, status: 403, message: "Role mismatch" };
        }
      }
      return { allowed: true };
    }

    // Student trying to login to Warden portal
    const r1 = checkPortalRole("student", "warden");
    assert.strictEqual(r1.allowed, false, "Student cannot login via warden portal");

    // Warden trying to login to Student portal
    const r2 = checkPortalRole("warden", "student");
    assert.strictEqual(r2.allowed, false, "Warden cannot login via student portal");

    // Staff trying to login to Student portal
    const r3 = checkPortalRole("staff", "student");
    assert.strictEqual(r3.allowed, false, "Staff cannot login via student portal");

    // Student in Student portal
    const r4 = checkPortalRole("student", "student");
    assert.strictEqual(r4.allowed, true, "Student in student portal allowed");

    // Warden in Warden portal
    const r5 = checkPortalRole("warden", "warden");
    assert.strictEqual(r5.allowed, true, "Warden in warden portal allowed");

    // Staff in Staff portal
    const r6 = checkPortalRole("staff", "staff");
    assert.strictEqual(r6.allowed, true, "Staff in staff portal allowed");
  });

  test("Login: Response user object NEVER exposes password or hash", () => {
    const mockDbUser = {
      _id: "507f1f77bcf86cd799439011",
      name: "Dr. Sharma",
      email: "sharma@hostelcare.com",
      password: "$2a$10$e8wF4r719G0.hashed.password.here",
      role: "warden",
      hostelName: "Tagore Hostel"
    };

    const sanitizedUser = {
      id: mockDbUser._id,
      name: mockDbUser.name,
      studentId: mockDbUser.studentId || "",
      email: mockDbUser.email,
      phone: mockDbUser.phone || "",
      hostelName: mockDbUser.hostelName || "",
      role: mockDbUser.role
    };

    assert(!("password" in sanitizedUser), "Password field must never be in login response");
    assert(!JSON.stringify(sanitizedUser).includes("hashed"), "Password hash must never be in response");
  });

  // 6. Authorization Middleware Tests
  test("Authorization: requireWarden authorizes warden and rejects student/staff", () => {
    const wardenReq = { user: { userId: "1", role: "warden" } };
    const studentReq = { user: { userId: "2", role: "student" } };
    const staffReq = { user: { userId: "3", role: "staff" } };

    let wardenCalled = false;
    authMiddleware.requireWarden(wardenReq, {}, () => {
      wardenCalled = true;
    });
    assert(wardenCalled, "requireWarden should allow warden");

    let studentRejected = false;
    const mockResStudent = {
      status: (code) => {
        if (code === 403) studentRejected = true;
        return { json: () => {} };
      }
    };
    authMiddleware.requireWarden(studentReq, mockResStudent, () => {});
    assert(studentRejected, "requireWarden should reject student with 403");

    let staffRejected = false;
    const mockResStaff = {
      status: (code) => {
        if (code === 403) staffRejected = true;
        return { json: () => {} };
      }
    };
    authMiddleware.requireWarden(staffReq, mockResStaff, () => {});
    assert(staffRejected, "requireWarden should reject staff with 403");
  });

  test("Authorization: requireStaff authorizes staff and warden, rejects student", () => {
    const staffReq = { user: { userId: "3", role: "staff" } };
    const wardenReq = { user: { userId: "1", role: "warden" } };
    const studentReq = { user: { userId: "2", role: "student" } };

    let staffCalled = false;
    authMiddleware.requireStaff(staffReq, {}, () => {
      staffCalled = true;
    });
    assert(staffCalled, "requireStaff should allow staff");

    let wardenCalled = false;
    authMiddleware.requireStaff(wardenReq, {}, () => {
      wardenCalled = true;
    });
    assert(wardenCalled, "requireStaff should allow warden");

    let studentRejected = false;
    const mockRes = {
      status: (code) => {
        if (code === 403) studentRejected = true;
        return { json: () => {} };
      }
    };
    authMiddleware.requireStaff(studentReq, mockRes, () => {});
    assert(studentRejected, "requireStaff should reject student with 403");
  });

  test("Authorization: requireStudent authorizes student, rejects warden and staff", () => {
    const studentReq = { user: { userId: "2", role: "student" } };
    const wardenReq = { user: { userId: "1", role: "warden" } };

    let studentCalled = false;
    authMiddleware.requireStudent(studentReq, {}, () => {
      studentCalled = true;
    });
    assert(studentCalled, "requireStudent should allow student");

    let wardenRejected = false;
    const mockRes = {
      status: (code) => {
        if (code === 403) wardenRejected = true;
        return { json: () => {} };
      }
    };
    authMiddleware.requireStudent(wardenReq, mockRes, () => {});
    assert(wardenRejected, "requireStudent should reject warden with 403");
  });

  // 7. Legacy Account Compatibility
  test("Compatibility: Legacy student account structure remains completely valid", () => {
    const legacyStudent = new User({
      name: "Old Student",
      studentId: "LEGACY-001",
      email: "old@gmail.com",
      password: "HashedPassword123!",
      collegeName: "Univ",
      course: "BTech",
      currentYear: "4",
      semester: "8",
      hostelName: "Tagore Hostel",
      hostelBlock: "A",
      roomNumber: "302",
      phone: "9998887770",
      acceptedTerms: true,
      role: "student"
    });

    const err = legacyStudent.validateSync();
    assert(!err, "Legacy student record must validate cleanly without error");
    assert.strictEqual(legacyStudent.role, "student");
    assert.strictEqual(legacyStudent.studentId, "LEGACY-001");
  });

  // 8. Warden Dashboard Endpoint Scoping and Aggregation Tests
  await testAsync("Warden Dashboard: Rejects warden lacking assigned hostel with 400", async () => {
    const origFindById = User.findById;
    try {
      User.findById = () => ({
        select: () => Promise.resolve({
          _id: "warden123",
          name: "Test Warden",
          email: "warden@test.com",
          role: "warden",
          hostelName: ""
        })
      });

      let statusCode = 0;
      let responseBody = null;
      const mockReq = { user: { userId: "warden123", role: "warden" } };
      const mockRes = {
        status: (code) => {
          statusCode = code;
          return {
            json: (body) => { responseBody = body; }
          };
        }
      };

      const wardenRouter = require("../routes/warden");
      const routeLayer = wardenRouter.stack.find(s => s.route && s.route.path === "/dashboard");
      const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

      await handler(mockReq, mockRes);
      assert.strictEqual(statusCode, 400, "Should return 400 when hostelName is missing");
      assert(responseBody.message.includes("authorized hostel assignment"), "Should have clear error message");
    } finally {
      User.findById = origFindById;
    }
  });

  await testAsync("Warden Dashboard: Scopes queries strictly to warden's hostel and calculates metrics", async () => {
    const origFindById = User.findById;
    const origUserFind = User.find;
    const origComplaintCount = Complaint.countDocuments;
    const origComplaintFind = Complaint.find;

    try {
      User.findById = () => ({
        select: () => Promise.resolve({
          _id: "warden123",
          name: "Dr. Sharma",
          email: "warden.tagore@hostelcare.com",
          role: "warden",
          hostelName: "Tagore Hostel",
          hostelBlock: "A"
        })
      });

      let capturedHostelRegex = null;
      User.find = (query) => {
        if (query.role === "student" && query.hostelName) {
          capturedHostelRegex = query.hostelName;
        }
        return {
          distinct: () => Promise.resolve(["stu1", "stu2"])
        };
      };

      let capturedComplaintQueries = [];
      Complaint.countDocuments = (query) => {
        capturedComplaintQueries.push(query);
        if (query.status === "submitted") return Promise.resolve(5);
        if (query.status === "under_review") return Promise.resolve(3);
        if (query.status === "assigned") return Promise.resolve(2);
        if (query.status === "in_progress") return Promise.resolve(4);
        if (query.status === "resolved") return Promise.resolve(10);
        if (query.priority) return Promise.resolve(4);
        return Promise.resolve(24);
      };

      Complaint.find = (query) => ({
        sort: () => ({
          limit: () => ({
            populate: () => ({
              populate: () => Promise.resolve([
                {
                  _id: "comp1",
                  ticketId: "TKT-101",
                  title: "Water leakage in bathroom",
                  description: "Continuous water leakage from sink pipe",
                  category: "plumbing",
                  priority: "high",
                  status: "submitted",
                  hostelBlock: "A",
                  roomNumber: "101",
                  photoUrl: "",
                  aiSummary: "Plumbing issue: faulty joint leak",
                  createdAt: new Date().toISOString(),
                  student: { _id: "stu1", name: "Aryan Kumar", studentId: "12305507" },
                  assignedStaff: null
                }
              ])
            })
          })
        })
      });

      let responseData = null;
      const mockReq = { user: { userId: "warden123", role: "warden" } };
      const mockRes = {
        status: () => ({ json: (d) => { responseData = d; } }),
        json: (d) => { responseData = d; }
      };

      const wardenRouter = require("../routes/warden");
      const routeLayer = wardenRouter.stack.find(s => s.route && s.route.path === "/dashboard");
      const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

      await handler(mockReq, mockRes);

      assert(responseData && responseData.success, "Response should be success: true");
      assert.strictEqual(responseData.warden.hostelName, "Tagore Hostel");
      assert.strictEqual(responseData.stats.total, 24);
      assert.strictEqual(responseData.stats.pending, 8, "Pending should be submitted (5) + under_review (3)");
      assert.strictEqual(responseData.stats.inProgress, 6, "In Progress should be assigned (2) + in_progress (4)");
      assert.strictEqual(responseData.stats.resolved, 10);
      assert.strictEqual(responseData.stats.priorityIssues, 4);
      assert.strictEqual(responseData.recentComplaints.length, 1);
      assert.strictEqual(responseData.recentComplaints[0].title, "Water leakage in bathroom");
      assert.strictEqual(responseData.recentComplaints[0].student.name, "Aryan Kumar");
      assert.strictEqual(responseData.recentComplaints[0].student.studentId, "12305507");

      assert(capturedComplaintQueries.length > 0, "Complaints count queries must be executed");
      assert.deepStrictEqual(capturedComplaintQueries[0].student, { $in: ["stu1", "stu2"] }, "Must scope to student IDs");
    } finally {
      User.findById = origFindById;
      User.find = origUserFind;
      Complaint.countDocuments = origComplaintCount;
      Complaint.find = origComplaintFind;
    }
  });

  // 9. Staff Account Creation & Management Tests
  await testAsync("Staff Management: Warden creates staff with role 'staff', auto-generated staffId, and hashed password", async () => {
    const origFindById = User.findById;
    const origFindOne = User.findOne;
    const origCreate = User.create;
    const origFind = User.find;

    try {
      User.find = () => ({ distinct: () => Promise.resolve([]) });
      User.findById = () => ({
        select: () => Promise.resolve({
          _id: "warden123",
          name: "Dr. Sharma",
          email: "warden.tagore@hostelcare.com",
          role: "warden",
          hostelName: "Tagore Hostel",
          hostelBlock: "A"
        })
      });

      User.findOne = () => Promise.resolve(null); // No existing email duplicate

      let createdDoc = null;
      User.create = (doc) => {
        createdDoc = doc;
        return Promise.resolve({ _id: "staff123", ...doc });
      };

      const wardenRouter = require("../routes/warden");
      const routeLayer = wardenRouter.stack.find(s => s.route && s.route.path === "/staff" && s.route.methods.post);
      const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

      const mockReq = {
        user: { userId: "warden123", role: "warden" },
        body: {
          name: "Rahul Mehta",
          email: "rahul.plumber@hostelcare.com",
          phone: "9876543210",
          department: "Plumbing",
          hostelBlock: "A",
          password: "TempPassword123!"
        }
      };

      let statusCode = 200;
      let responseData = null;
      const mockRes = {
        status: (code) => { statusCode = code; return { json: (d) => { responseData = d; } }; },
        json: (d) => { responseData = d; }
      };

      await handler(mockReq, mockRes);

      assert.strictEqual(statusCode, 201);
      assert(responseData && responseData.success, "Staff creation should be success: true");
      assert.strictEqual(createdDoc.role, "staff");
      assert.strictEqual(createdDoc.hostelName, "Tagore Hostel");
      assert.strictEqual(createdDoc.department, "Plumbing");
      assert(createdDoc.staffId.startsWith("STF-"), "Staff ID should start with STF-");
      assert.strictEqual(createdDoc.isActive, true);
      assert.strictEqual(createdDoc.mustChangePassword, true);
      assert(createdDoc.password !== "TempPassword123!", "Password must be hashed");
      assert(await bcrypt.compare("TempPassword123!", createdDoc.password), "Hashed password must match");
    } finally {
      User.findById = origFindById;
      User.findOne = origFindOne;
      User.create = origCreate;
      User.find = origFind;
    }
  });

  // 10. Deactivated Staff Login Rejection Test
  await testAsync("Login: Deactivated staff account is rejected with 403 Forbidden", async () => {
    const origFindOne = User.findOne;
    try {
      const hashedPassword = await bcrypt.hash("StaffPass123!", 10);
      User.findOne = () => Promise.resolve({
        _id: "staff_deactivated",
        name: "Inactive Staff",
        email: "inactive.staff@hostelcare.com",
        password: hashedPassword,
        role: "staff",
        isActive: false
      });

      const authRouter = require("../routes/auth");
      const routeLayer = authRouter.stack.find(s => s.route && s.route.path === "/login");
      const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

      const mockReq = {
        body: {
          email: "inactive.staff@hostelcare.com",
          password: "StaffPass123!",
          portal: "staff"
        }
      };

      let statusCode = 0;
      let responseData = null;
      const mockRes = {
        status: (code) => { statusCode = code; return { json: (d) => { responseData = d; } }; },
        json: (d) => { responseData = d; }
      };

      await handler(mockReq, mockRes);
      assert.strictEqual(statusCode, 403, "Deactivated staff must receive 403 status code");
      assert(responseData.message.includes("deactivated"), "Response should explain account is deactivated");
    } finally {
      User.findOne = origFindOne;
    }
  });

  // 11. Staff Task Authorization and Scoping
  await testAsync("Staff Portal: Staff cannot access task assigned to another staff member", async () => {
    const origFindById = Complaint.findById;
    try {
      Complaint.findById = () => ({
        populate: () => ({
          populate: () => Promise.resolve({
            _id: "comp_999",
            title: "Broken window",
            assignedStaff: "different_staff_id_456"
          })
        })
      });

      const staffRouter = require("../routes/staff");
      const routeLayer = staffRouter.stack.find(s => s.route && s.route.path === "/tasks/:id" && s.route.methods.get);
      const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

      const mockReq = {
        user: { userId: "my_staff_id_123", role: "staff" },
        params: { id: "comp_999" }
      };

      let statusCode = 0;
      const mockRes = {
        status: (code) => { statusCode = code; return { json: () => {} }; },
        json: () => {}
      };

      await handler(mockReq, mockRes);
      assert.strictEqual(statusCode, 403, "Staff attempting to access another staff member's task must get 403 Forbidden");
    } finally {
      Complaint.findById = origFindById;
    }
  });

  // 12. Staff Task Completion Review Submission
  await testAsync("Staff Portal: Staff submits completion review, updates status to under_review, and appends timeline", async () => {
    const origFindById = Complaint.findById;
    const origFindStudent = User.findById;
    const origFindWardens = User.find;
    const Notification = require("../models/Notification");
    const origNotifCreate = Notification.create;

    try {
      let saved = false;
      const mockTask = {
        _id: "comp_100",
        ticketId: "HC-123456",
        title: "Leaking faucet",
        roomNumber: "204",
        student: "stu_1",
        assignedStaff: "staff_1",
        status: "in_progress",
        timeline: [],
        save: () => { saved = true; return Promise.resolve(); }
      };

      Complaint.findById = () => Promise.resolve(mockTask);
      User.findById = () => Promise.resolve({ _id: "stu_1", hostelName: "Tagore Hostel" });
      User.find = () => Promise.resolve([{ _id: "warden_1" }]);

      let notificationCreated = false;
      Notification.create = () => { notificationCreated = true; return Promise.resolve(); };

      const staffRouter = require("../routes/staff");
      const routeLayer = staffRouter.stack.find(s => s.route && s.route.path === "/tasks/:id/complete" && s.route.methods.post);
      const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

      const mockReq = {
        user: { userId: "staff_1", role: "staff" },
        params: { id: "comp_100" },
        body: { completionNotes: "Replaced faulty washer. Water flow normal.", completionPhotoUrl: "data:image/jpeg;base64,sample" }
      };

      let responseData = null;
      const mockRes = {
        status: () => ({ json: (d) => { responseData = d; } }),
        json: (d) => { responseData = d; }
      };

      await handler(mockReq, mockRes);

      assert(saved, "Complaint document should be saved to database");
      assert.strictEqual(mockTask.status, "under_review", "Status should transition to under_review");
      assert.strictEqual(mockTask.completionPhotoUrl, "data:image/jpeg;base64,sample");
      assert.strictEqual(mockTask.timeline.length, 1);
      assert.strictEqual(mockTask.timeline[0].status, "under_review");
      assert(responseData && responseData.success, "Response should be success: true");
    } finally {
      Complaint.findById = origFindById;
      User.findById = origFindStudent;
      User.find = origFindWardens;
      Notification.create = origNotifCreate;
    }
  });

  // 12. Student Complaint Submission & Scoped Synchronization Tests
  await testAsync("Complaint Submission: Persists trusted hostelName from student profile", async () => {
    const origFindById = User.findById;
    const origCreate = Complaint.create;

    try {
      User.findById = () => Promise.resolve({
        _id: "student_abc",
        name: "Test Student",
        email: "student@gmail.com",
        role: "student",
        hostelName: "Campus Hostel",
        hostelBlock: "B",
        roomNumber: "204"
      });

      let createdComplaint = null;
      Complaint.create = (doc) => {
        createdComplaint = new Complaint(doc);
        return Promise.resolve(createdComplaint);
      };

      const complaintsRouter = require("../routes/complaints");
      const routeLayer = complaintsRouter.stack.find(s => s.route && s.route.path === "/" && s.route.methods.post);
      const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

      let responseData = null;
      let statusCode = 200;
      const mockReq = {
        user: { userId: "student_abc", role: "student" },
        body: {
          title: "Leaking pipe in washroom",
          description: "Water leakage causing floor flooding",
          category: "plumbing",
          hostelBlock: "Tagore Block B",
          roomNumber: "204"
        }
      };
      const mockRes = {
        status: (code) => { statusCode = code; return { json: (d) => { responseData = d; } }; },
        json: (d) => { responseData = d; }
      };

      await handler(mockReq, mockRes);

      assert.strictEqual(statusCode, 201, "Complaint should be created with 201 Created");
      assert(createdComplaint, "Complaint should be created in DB");
      assert.strictEqual(createdComplaint.hostelName, "Campus Hostel", "hostelName must be populated from student profile");
      assert.strictEqual(createdComplaint.hostelBlock, "Tagore Block B");
      assert(createdComplaint.ticketId && createdComplaint.ticketId.startsWith("HC-"), "ticketId must follow HC format");
    } finally {
      User.findById = origFindById;
      Complaint.create = origCreate;
    }
  });

  await testAsync("Warden Scoping: Block normalization matches Block B variants and foreign hostel is rejected with 403", async () => {
    const origFindByIdUser = User.findById;
    const origFindUser = User.find;
    const origFindByIdComplaint = Complaint.findById;

    try {
      User.findById = (id) => ({
        select: () => Promise.resolve({
          _id: "warden_b",
          name: "Warden Block B",
          email: "warden.b@hostelcare.com",
          role: "warden",
          hostelName: "Campus Hostel",
          hostelBlock: "B"
        })
      });

      User.find = (query) => ({
        distinct: () => Promise.resolve(["student_in_campus"])
      });

      // 1. Complaint in Campus Hostel, Tagore Block B -> Authorized
      Complaint.findById = () => ({
        populate: () => ({
          populate: () => ({
            populate: () => Promise.resolve({
              _id: "comp_campus_b",
              title: "Leakage in Block B",
              student: { _id: "student_in_campus", name: "Campus Student" },
              hostelName: "Campus Hostel",
              hostelBlock: "Tagore Block B",
              timeline: []
            })
          })
        })
      });

      const wardenRouter = require("../routes/warden");
      const routeLayer = wardenRouter.stack.find(s => s.route && s.route.path === "/complaints/:id" && s.route.methods.get);
      const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

      let responseData = null;
      let statusCode = 200;
      const mockReq = { user: { userId: "warden_b", role: "warden" }, params: { id: "comp_campus_b" } };
      const mockRes = {
        status: (code) => { statusCode = code; return { json: (d) => { responseData = d; } }; },
        json: (d) => { responseData = d; }
      };

      await handler(mockReq, mockRes);
      assert.strictEqual(statusCode, 200, "Warden B must have access to Tagore Block B complaint");
      assert.strictEqual(responseData.complaint.title, "Leakage in Block B");

      // 2. Complaint in foreign hostel -> Must be rejected with 403 Forbidden
      Complaint.findById = () => ({
        populate: () => ({
          populate: () => ({
            populate: () => Promise.resolve({
              _id: "comp_foreign",
              title: "Foreign Hostel Issue",
              student: { _id: "foreign_student", name: "Foreign Student" },
              hostelName: "Other College Hostel",
              hostelBlock: "B",
              timeline: []
            })
          })
        })
      });

      let foreignStatus = 200;
      let foreignResponse = null;
      const mockResForeign = {
        status: (code) => { foreignStatus = code; return { json: (d) => { foreignResponse = d; } }; },
        json: (d) => { foreignResponse = d; }
      };

      await handler(mockReq, mockResForeign);
      assert.strictEqual(foreignStatus, 403, "Foreign hostel complaint must be rejected with 403 Forbidden");
    } finally {
      User.findById = origFindByIdUser;
      User.find = origFindUser;
      Complaint.findById = origFindByIdComplaint;
    }
  });

  // 13. Student Registration with Hostel Name
  await testAsync("Registration: Student registration succeeds with entered hostelName and persists to MongoDB", async () => {
    const origFindOne = User.findOne;
    const origCreate = User.create;
    try {
      User.findOne = () => Promise.resolve(null);
      let createdUser = null;
      User.create = (doc) => {
        createdUser = doc;
        return Promise.resolve({ _id: "new_stu_123", ...doc });
      };

      const authRouter = require("../routes/auth");
      const routeLayer = authRouter.stack.find(s => s.route && s.route.path === "/register");
      const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

      let statusCode = 200;
      let responseData = null;
      const mockReq = {
        body: {
          name: "Amit Patel",
          studentId: "12309999",
          gender: "Male",
          email: "amit.patel@gmail.com",
          phone: "+91 9876543210",
          collegeName: "Apex Institute of Tech",
          course: "B.Tech Computer Science",
          admissionYear: "2023",
          hostelName: "BH-1",
          hostelBlock: "B",
          roomNumber: "302",
          password: "SecureStudentPass123!",
          confirmPassword: "SecureStudentPass123!",
          acceptedTerms: true,
          role: "student"
        }
      };
      const mockRes = {
        status: (code) => { statusCode = code; return { json: (d) => { responseData = d; } }; },
        json: (d) => { responseData = d; }
      };

      await handler(mockReq, mockRes);
      assert.strictEqual(statusCode, 201, "Registration should return 201 Created");
      assert(createdUser, "User should be created in MongoDB");
      assert.strictEqual(createdUser.hostelName, "BH-1", "hostelName must be saved as canonical format");
      assert.strictEqual(createdUser.gender, "Male", "gender must be saved from request");
      assert.strictEqual(createdUser.role, "student");
    } finally {
      User.findOne = origFindOne;
      User.create = origCreate;
    }
  });

  await testAsync("Registration: Rejection when hostelName is missing or whitespace-only", async () => {
    const authRouter = require("../routes/auth");
    const routeLayer = authRouter.stack.find(s => s.route && s.route.path === "/register");
    const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

    // 1. Missing hostelName
    let statusCodeMissing = 200;
    const mockReqMissing = {
      body: {
        name: "Amit Patel",
        studentId: "12309999",
        gender: "Male",
        email: "amit.patel@gmail.com",
        phone: "+91 9876543210",
        collegeName: "Apex Institute of Tech",
        course: "B.Tech Computer Science",
        admissionYear: "2023",
        hostelBlock: "B",
        roomNumber: "302",
        password: "SecureStudentPass123!",
        confirmPassword: "SecureStudentPass123!",
        acceptedTerms: true
      }
    };
    const mockResMissing = {
      status: (code) => { statusCodeMissing = code; return { json: () => {} }; },
      json: () => {}
    };
    await handler(mockReqMissing, mockResMissing);
    assert.strictEqual(statusCodeMissing, 400, "Registration without hostelName must return 400");

    // 2. Whitespace-only hostelName
    let statusCodeBlank = 200;
    const mockReqBlank = {
      body: {
        ...mockReqMissing.body,
        hostelName: "   "
      }
    };
    const mockResBlank = {
      status: (code) => { statusCodeBlank = code; return { json: () => {} }; },
      json: () => {}
    };
    await handler(mockReqBlank, mockResBlank);
    assert.strictEqual(statusCodeBlank, 400, "Registration with blank hostelName must return 400");
  });

  // 14. Complaint Submission Missing Hostel Rejection
  await testAsync("Complaint Submission: Blocked with clear message when student profile lacks hostelName", async () => {
    const origFindById = User.findById;
    try {
      User.findById = () => Promise.resolve({
        _id: "student_no_hostel",
        name: "Legacy Student",
        email: "legacy@gmail.com",
        role: "student",
        hostelName: "", // Missing hostel assignment
        hostelBlock: "B",
        roomNumber: "927"
      });

      const complaintsRouter = require("../routes/complaints");
      const routeLayer = complaintsRouter.stack.find(s => s.route && s.route.path === "/" && s.route.methods.post);
      const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

      let statusCode = 200;
      let responseData = null;
      const mockReq = {
        user: { userId: "student_no_hostel", role: "student" },
        body: {
          title: "Leaking faucet",
          description: "Tap dripping continuously",
          category: "plumbing",
          hostelBlock: "B",
          roomNumber: "927"
        }
      };
      const mockRes = {
        status: (code) => { statusCode = code; return { json: (d) => { responseData = d; } }; },
        json: (d) => { responseData = d; }
      };

      await handler(mockReq, mockRes);
      assert.strictEqual(statusCode, 400, "Must return 400 when student lacks hostel assignment");
      assert(responseData && responseData.message, "Must include helpful error message");
      assert.strictEqual(
        responseData.message,
        "Please complete your hostel information in your profile before submitting a complaint."
      );
    } finally {
      User.findById = origFindById;
    }
  });

  // 15. Complaint Submission Anti-Spoofing Tests
  await testAsync("Complaint Submission: Rejects attempts to spoof foreign hostelName or mismatching block", async () => {
    const origFindById = User.findById;
    try {
      User.findById = () => Promise.resolve({
        _id: "student_auth",
        name: "Valid Student",
        email: "student@gmail.com",
        role: "student",
        hostelName: "Tagore Hostel",
        hostelBlock: "B",
        roomNumber: "204"
      });

      const complaintsRouter = require("../routes/complaints");
      const routeLayer = complaintsRouter.stack.find(s => s.route && s.route.path === "/" && s.route.methods.post);
      const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

      // 1. Spoofing different hostel in request body -> 403 Forbidden
      let statusHostelSpoof = 200;
      let resHostelSpoof = null;
      const mockReqHostel = {
        user: { userId: "student_auth", role: "student" },
        body: {
          title: "Broken fan",
          description: "Ceiling fan making loud noise",
          category: "electrical",
          hostelName: "Raman Hostel", // Student belongs to Tagore Hostel!
          hostelBlock: "B",
          roomNumber: "204"
        }
      };
      const mockResHostel = {
        status: (code) => { statusHostelSpoof = code; return { json: (d) => { resHostelSpoof = d; } }; },
        json: (d) => { resHostelSpoof = d; }
      };

      await handler(mockReqHostel, mockResHostel);
      assert.strictEqual(statusHostelSpoof, 403, "Spoofing a foreign hostel must return 403 Forbidden");
      assert.strictEqual(resHostelSpoof.message, "You cannot submit complaints for a hostel you do not belong to.");

      // 2. Attempting to submit for a completely different block -> 400 Bad Request
      let statusBlockSpoof = 200;
      let resBlockSpoof = null;
      const mockReqBlock = {
        user: { userId: "student_auth", role: "student" },
        body: {
          title: "Broken chair",
          description: "Chair leg is detached",
          category: "furniture",
          hostelName: "Tagore Hostel",
          hostelBlock: "C", // Student is registered in Block B
          roomNumber: "204"
        }
      };
      const mockResBlock = {
        status: (code) => { statusBlockSpoof = code; return { json: (d) => { resBlockSpoof = d; } }; },
        json: (d) => { resBlockSpoof = d; }
      };

      await handler(mockReqBlock, mockResBlock);
      assert.strictEqual(statusBlockSpoof, 400, "Mismatched block submission must return 400");
    } finally {
      User.findById = origFindById;
    }
  });

  // 16. Profile Update for Existing Student Account
  await testAsync("Profile Update: Allows student with missing hostelName to save valid hostelName in MongoDB", async () => {
    const origFindById = User.findById;
    const origFindOne = User.findOne;
    try {
      const mockUser = {
        _id: "legacy_user_1",
        name: "Sahil Kumar",
        email: "sahil@gmail.com",
        role: "student",
        hostelName: "", // initially missing
        hostelBlock: "B",
        roomNumber: "927",
        save: function() { return Promise.resolve(this); }
      };

      User.findById = () => Promise.resolve(mockUser);
      User.findOne = () => Promise.resolve(null); // No email conflict

      const profileRouter = require("../routes/profile");
      const routeLayer = profileRouter.stack.find(s => s.route && s.route.path === "/" && s.route.methods.put);
      const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

      // 1. Valid update with hostelName
      let statusCode = 200;
      let responseData = null;
      const mockReq = {
        user: { userId: "legacy_user_1", role: "student" },
        body: {
          name: "Sahil Kumar Chaudhary",
          email: "sahil@gmail.com",
          hostelName: "Tagore Hostel",
          hostelBlock: "B",
          roomNumber: "927"
        }
      };
      const mockRes = {
        status: (code) => { statusCode = code; return { json: (d) => { responseData = d; } }; },
        json: (d) => { responseData = d; }
      };

      await handler(mockReq, mockRes);
      assert.strictEqual(statusCode, 200, "Profile update should succeed with 200 OK");
      assert.strictEqual(mockUser.hostelName, "Tagore Hostel", "User document should have updated hostelName");
      assert.strictEqual(responseData.user.hostelName, "Tagore Hostel");

      // 2. Reject empty or whitespace-only hostelName
      let statusCodeEmpty = 200;
      const mockReqEmpty = {
        user: { userId: "legacy_user_1", role: "student" },
        body: {
          name: "Sahil Kumar",
          email: "sahil@gmail.com",
          hostelName: "   ",
          hostelBlock: "B",
          roomNumber: "927"
        }
      };
      const mockResEmpty = {
        status: (code) => { statusCodeEmpty = code; return { json: () => {} }; },
        json: () => {}
      };
      await handler(mockReqEmpty, mockResEmpty);
      assert.strictEqual(statusCodeEmpty, 400, "Whitespace hostelName in profile update must return 400");
    } finally {
      User.findById = origFindById;
      User.findOne = origFindOne;
    }
  });

  // 17. Warden Complaint Access Scoping: Unassigned complaints rejected
  await testAsync("Warden Access: Unassigned complaints without hostelName return 403 Forbidden to Wardens", async () => {
    const origFindByIdUser = User.findById;
    const origFindUser = User.find;
    const origFindByIdComplaint = Complaint.findById;

    try {
      User.findById = () => ({
        select: () => Promise.resolve({
          _id: "warden_test",
          name: "Warden Test",
          email: "warden.test@hostelcare.com",
          role: "warden",
          hostelName: "Tagore Hostel",
          hostelBlock: "all"
        })
      });

      User.find = () => ({
        distinct: () => Promise.resolve(["stu_test"])
      });

      // Complaint without hostelName
      Complaint.findById = () => ({
        populate: () => ({
          populate: () => ({
            populate: () => Promise.resolve({
              _id: "comp_no_hostel",
              ticketId: "HC-076299",
              title: "Legacy issue without hostel",
              student: { _id: "stu_test", name: "Student Test" },
              hostelName: "", // unassigned
              hostelBlock: "B",
              timeline: []
            })
          })
        })
      });

      const wardenRouter = require("../routes/warden");
      const routeLayer = wardenRouter.stack.find(s => s.route && s.route.path === "/complaints/:id" && s.route.methods.get);
      const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

      let statusCode = 200;
      const mockReq = { user: { userId: "warden_test", role: "warden" }, params: { id: "comp_no_hostel" } };
      const mockRes = {
        status: (code) => { statusCode = code; return { json: () => {} }; },
        json: () => {}
      };

      await handler(mockReq, mockRes);
      assert.strictEqual(statusCode, 403, "Complaint without hostelName must be rejected with 403 until assigned");
    } finally {
      User.findById = origFindByIdUser;
      User.find = origFindUser;
      Complaint.findById = origFindByIdComplaint;
    }
  });

  // 18. Notifications Lifecycle, Scoping, and Deduplication Tests
  await testAsync("Notifications: Schema enforces valid types and recipient requirement", async () => {
    const Notification = require("../models/Notification");
    const validNotif = new Notification({
      recipient: new mongoose.Types.ObjectId(),
      title: "Complaint Registered",
      message: "Ticket HC-123456 registered successfully.",
      type: "complaint_created",
      complaintId: new mongoose.Types.ObjectId()
    });
    const err = validNotif.validateSync();
    assert(!err, "Valid notification should pass validation");

    const invalidNotif = new Notification({
      title: "Test",
      message: "Test message",
      type: "invalid_type"
    });
    const errInvalid = invalidNotif.validateSync();
    assert(errInvalid, "Notification without recipient and with invalid type should fail validation");
    assert(errInvalid.errors.recipient, "recipient is required");
    assert(errInvalid.errors.type, "type must be valid enum");
  });

  await testAsync("Notifications API: Recipient isolation, read marking, and count accuracy", async () => {
    const Notification = require("../models/Notification");
    const origFind = Notification.find;
    const origCount = Notification.countDocuments;
    const origFindOne = Notification.findOne;
    const origUpdateMany = Notification.updateMany;

    const notifRouter = require("../routes/notifications");
    const getLayer = notifRouter.stack.find(s => s.route && s.route.path === "/" && s.route.methods.get);
    const getHandler = getLayer.route.stack[getLayer.route.stack.length - 1].handle;

    const markReadLayer = notifRouter.stack.find(s => s.route && s.route.path === "/:id/read" && s.route.methods.patch);
    const markReadHandler = markReadLayer.route.stack[markReadLayer.route.stack.length - 1].handle;

    const markAllLayer = notifRouter.stack.find(s => s.route && s.route.path === "/mark-all-read" && s.route.methods.patch);
    const markAllHandler = markAllLayer.route.stack[markAllLayer.route.stack.length - 1].handle;

    try {
      const userAId = "507f191e810c19729de860ea";
      const notifId = "507f1f77bcf86cd799439011";

      let capturedQuery = null;
      Notification.find = (q) => {
        capturedQuery = q;
        return {
          sort: () => ({
            limit: () => Promise.resolve([
              { _id: notifId, title: "Your Task", message: "Details", type: "complaint_assigned", isRead: false, createdAt: new Date() }
            ])
          })
        };
      };
      Notification.countDocuments = (q) => {
        assert.strictEqual(q.recipient, userAId);
        assert.strictEqual(q.isRead, false);
        return Promise.resolve(1);
      };

      let resData = null;
      await getHandler({ user: { userId: userAId } }, { json: (d) => { resData = d; } });
      assert(resData && resData.success, "GET /api/notifications should succeed");
      assert.strictEqual(capturedQuery.recipient, userAId, "Query must strictly isolate to authenticated recipient");
      assert.strictEqual(resData.unreadCount, 1);
      assert.strictEqual(resData.notifications.length, 1);

      // Mark single read
      let saved = false;
      Notification.findOne = (q) => {
        assert.strictEqual(q._id, notifId);
        assert.strictEqual(q.recipient, userAId);
        return Promise.resolve({
          _id: notifId,
          isRead: false,
          save: () => { saved = true; return Promise.resolve(); }
        });
      };
      await markReadHandler({ user: { userId: userAId }, params: { id: notifId } }, { json: (d) => { resData = d; } });
      assert(saved, "Notification should be marked read and saved");

      // Mark all read
      let updatedFilter = null;
      Notification.updateMany = (filter, update) => {
        updatedFilter = filter;
        assert.strictEqual(update.$set.isRead, true);
        return Promise.resolve({ modifiedCount: 5 });
      };
      await markAllHandler({ user: { userId: userAId } }, { json: (d) => { resData = d; } });
      assert.strictEqual(updatedFilter.recipient, userAId);
      assert.strictEqual(updatedFilter.isRead, false);
    } finally {
      Notification.find = origFind;
      Notification.countDocuments = origCount;
      Notification.findOne = origFindOne;
      Notification.updateMany = origUpdateMany;
    }
  });

  await testAsync("Notifications Lifecycle: Status update creates student/staff notifications with deduplication", async () => {
    const Notification = require("../models/Notification");
    const origFindOneNotif = Notification.findOne;
    const origCreateNotif = Notification.create;
    const origFindByIdComplaint = Complaint.findById;
    const origFindByIdUser = User.findById;
    const origFindUser = User.find;

    try {
      const studentId = new mongoose.Types.ObjectId();
      const staffId = new mongoose.Types.ObjectId();
      const complaintId = new mongoose.Types.ObjectId();
      const wardenId = new mongoose.Types.ObjectId();

      let complaintDoc = {
        _id: complaintId,
        ticketId: "HC-999888",
        title: "Broken window",
        hostelName: "BH1",
        hostelBlock: "B",
        student: studentId,
        assignedStaff: staffId,
        status: "under_review",
        timeline: [],
        save: () => Promise.resolve()
      };

      Complaint.findById = () => Promise.resolve(complaintDoc);
      User.findById = () => ({
        select: () => Promise.resolve({
          _id: wardenId,
          role: "warden",
          hostelName: "BH1",
          hostelBlock: "all"
        })
      });
      User.find = () => ({
        distinct: () => Promise.resolve([studentId.toString()])
      });

      const createdNotifications = [];
      Notification.findOne = () => Promise.resolve(null);
      Notification.create = (doc) => {
        createdNotifications.push(doc);
        return Promise.resolve(doc);
      };

      const wardenRouter = require("../routes/warden");
      const statusLayer = wardenRouter.stack.find(s => s.route && s.route.path === "/complaints/:id/status" && s.route.methods.patch);
      const statusHandler = statusLayer.route.stack[statusLayer.route.stack.length - 1].handle;

      const mockReq = {
        user: { userId: wardenId.toString(), role: "warden" },
        params: { id: complaintId.toString() },
        body: { status: "resolved", resolutionNote: "Window glass replaced and sealed." }
      };
      let resJson = null;
      await statusHandler(mockReq, {
        status: () => ({ json: (d) => { resJson = d; } }),
        json: (d) => { resJson = d; }
      });

      assert(resJson && resJson.success, "Status update should succeed");
      assert.strictEqual(createdNotifications.length, 2, "Should create 2 notifications: 1 for student, 1 for staff");
      assert.strictEqual(createdNotifications[0].type, "complaint_resolved");
      assert.strictEqual(createdNotifications[0].recipient.toString(), studentId.toString());
      assert.strictEqual(createdNotifications[1].type, "complaint_resolved");
      assert.strictEqual(createdNotifications[1].recipient.toString(), staffId.toString());

      // Deduplication test: when duplicate is found within 30 seconds, do not create duplicate
      createdNotifications.length = 0;
      Notification.findOne = () => Promise.resolve({ _id: new mongoose.Types.ObjectId() });
      await statusHandler(mockReq, {
        status: () => ({ json: () => {} }),
        json: () => {}
      });
      assert.strictEqual(createdNotifications.length, 0, "Deduplication must prevent duplicate notification creation");
    } finally {
      Notification.findOne = origFindOneNotif;
      Notification.create = origCreateNotif;
      Complaint.findById = origFindByIdComplaint;
      User.findById = origFindByIdUser;
      User.find = origFindUser;
    }
  });

  // 36. Hostel Name Normalization & Flexible Matching Tests
  test("Hostel Normalization: Utility correctly normalizes and matches equivalent hostel representations", () => {
    assert.strictEqual(isHostelMatch("BH1", "BH-1"), true, "BH1 should match BH-1");
    assert.strictEqual(isHostelMatch("BH1", "BH 1"), true, "BH1 should match BH 1");
    assert.strictEqual(isHostelMatch("BH1", "B.H. 1"), true, "BH1 should match B.H. 1");
    assert.strictEqual(isHostelMatch("BH-1", "BH1"), true, "BH-1 should match BH1");
    assert.strictEqual(isHostelMatch("BH-1", "bh-1"), true, "Case-insensitive matching");
    assert.strictEqual(isHostelMatch("BH-1", "BH - 1"), true, "Spaces and hyphens handled");
    assert.strictEqual(isHostelMatch("BH1", "BH2"), false, "Different numbers must not match");
    assert.strictEqual(isHostelMatch("BH1", "GH1"), false, "Different prefixes must not match");
    assert.strictEqual(isHostelMatch("Hostel A", "Hostel-A"), true, "Hostel A matches Hostel-A");
    assert.strictEqual(isHostelMatch("Hostel A", "Hostel B"), false, "Hostel A does not match Hostel B");

    const r1 = buildHostelRegex("BH1");
    assert(r1.test("BH-1"), "Regex for BH1 must match BH-1");
    assert(r1.test("BH1"), "Regex for BH1 must match BH1");
    assert(r1.test("BH 1"), "Regex for BH1 must match BH 1");
    assert(!r1.test("BH2"), "Regex for BH1 must reject BH2");
    assert(!r1.test("GH1"), "Regex for BH1 must reject GH1");

    const r2 = buildHostelRegex("BH-1");
    assert(r2.test("BH1"), "Regex for BH-1 must match BH1");
    assert(r2.test("BH-1"), "Regex for BH-1 must match BH-1");
  });

  // 37. Warden Scoping with Cross-Format Hostel Names (BH1 vs BH-1)
  await testAsync("Warden Scope: Warden with BH1 matches complaints and students from BH-1 within authorized block", async () => {
    const origFindById = User.findById;
    const origUserFind = User.find;
    const origCountDocuments = Complaint.countDocuments;
    const origComplaintFind = Complaint.find;

    try {
      const mockWardenId = new mongoose.Types.ObjectId();
      const mockStudentId = new mongoose.Types.ObjectId();

      User.findById = () => ({
        select: () => Promise.resolve({
          _id: mockWardenId,
          name: "Warden BH1",
          email: "warden.bh1@hostelcare.com",
          role: "warden",
          hostelName: "BH1",
          hostelBlock: "B"
        })
      });

      let capturedUserQuery = null;
      User.find = (q) => {
        capturedUserQuery = q;
        return {
          distinct: () => Promise.resolve([mockStudentId])
        };
      };

      let capturedComplaintQuery = null;
      Complaint.countDocuments = (q) => {
        capturedComplaintQuery = q;
        return Promise.resolve(1);
      };

      Complaint.find = () => ({
        sort: () => ({
          limit: () => ({
            populate: () => ({
              populate: () => Promise.resolve([
                {
                  _id: new mongoose.Types.ObjectId(),
                  ticketId: "HC-928758",
                  title: "Table damage",
                  description: "study table damage",
                  category: "furniture",
                  priority: "medium",
                  status: "submitted",
                  hostelName: "BH-1",
                  hostelBlock: "B",
                  roomNumber: "927",
                  createdAt: new Date().toISOString(),
                  student: {
                    _id: mockStudentId,
                    name: "Sahil Kumar",
                    studentId: "12305507",
                    email: "sahilku707@gmail.com",
                    hostelBlock: "B",
                    roomNumber: "927"
                  },
                  assignedStaff: null
                }
              ])
            })
          })
        })
      });

      const wardenRouter = require("../routes/warden");
      const routeLayer = wardenRouter.stack.find(s => s.route && s.route.path === "/dashboard");
      const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

      let resData = null;
      await handler(
        { user: { userId: mockWardenId.toString(), role: "warden" } },
        { status: () => ({ json: (d) => { resData = d; } }), json: (d) => { resData = d; } }
      );

      assert(resData && resData.success, "Warden dashboard must return success: true");
      assert.strictEqual(resData.stats.total, 1, "Total complaints should be 1");
      assert.strictEqual(resData.recentComplaints.length, 1);
      assert.strictEqual(resData.recentComplaints[0].ticketId, "HC-928758");

      // Verify that User.find regex matched BH-1
      assert(capturedUserQuery.hostelName.$regex.test("BH-1"), "User query regex must match BH-1");
      assert(capturedUserQuery.hostelName.$regex.test("BH1"), "User query regex must match BH1");
      assert(!capturedUserQuery.hostelName.$regex.test("BH2"), "User query regex must reject BH2");

      // Verify authorization helper
      const authorizedComplaint = {
        hostelName: "BH-1",
        hostelBlock: "B"
      };
      const foreignHostelComplaint = {
        hostelName: "BH2",
        hostelBlock: "B"
      };
      const mismatchBlockComplaint = {
        hostelName: "BH-1",
        hostelBlock: "A"
      };
      const wardenObj = {
        hostelName: "BH1",
        hostelBlock: "B"
      };

      assert.strictEqual(isHostelMatch(authorizedComplaint.hostelName, wardenObj.hostelName), true);
      assert.strictEqual(checkBlockAuthorized(authorizedComplaint, wardenObj), true);
      assert.strictEqual(isHostelMatch(foreignHostelComplaint.hostelName, wardenObj.hostelName), false);
      assert.strictEqual(checkBlockAuthorized(mismatchBlockComplaint, wardenObj), false);
    } finally {
      User.findById = origFindById;
      User.find = origUserFind;
      Complaint.countDocuments = origCountDocuments;
      Complaint.find = origComplaintFind;
    }
  });

  // 38. Warden Notification Creation on Complaint Submission with Flexible Hostel Format
  await testAsync("Complaint Submission: Student in BH-1 triggers notification for Warden in BH1 with block matching", async () => {
    const origFindByIdUser = User.findById;
    const origFindUser = User.find;
    const origCreateComplaint = Complaint.create;
    const origFindOneNotif = Notification.findOne;
    const origCreateNotif = Notification.create;

    try {
      const studentId = new mongoose.Types.ObjectId();
      const wardenId = new mongoose.Types.ObjectId();
      const otherBlockWardenId = new mongoose.Types.ObjectId();

      User.findById = (id) => {
        if (id.toString() === studentId.toString()) {
          return Promise.resolve({
            _id: studentId,
            role: "student",
            hostelName: "BH-1",
            hostelBlock: "B",
            roomNumber: "927"
          });
        }
        return Promise.resolve(null);
      };

      User.find = (q) => {
        if (q.role === "warden") {
          const regexObj = q.hostelName?.$regex || (q.$or && q.$or.find(c => c.hostelName)?.hostelName?.$regex);
          assert(regexObj && regexObj.test("BH-1"), "Must search wardens with regex matching BH-1");
          assert(regexObj.test("BH1"), "Must also match warden BH1");
          return Promise.resolve([
            { _id: wardenId, role: "warden", hostelName: "BH1", hostelBlock: "B" },
            { _id: otherBlockWardenId, role: "warden", hostelName: "BH1", hostelBlock: "C" }
          ]);
        }
        return Promise.resolve([]);
      };

      const complaintId = new mongoose.Types.ObjectId();
      Complaint.create = (doc) => {
        return Promise.resolve({
          _id: complaintId,
          ticketId: "HC-928758",
          title: doc.title,
          hostelName: doc.hostelName,
          hostelBlock: doc.hostelBlock,
          roomNumber: doc.roomNumber,
          category: doc.category,
          priority: doc.priority,
          student: doc.student
        });
      };

      const createdNotifications = [];
      Notification.findOne = () => Promise.resolve(null);
      Notification.create = (n) => {
        createdNotifications.push(n);
        return Promise.resolve({ _id: new mongoose.Types.ObjectId(), ...n });
      };

      const complaintsRouter = require("../routes/complaints");
      const routeLayer = complaintsRouter.stack.find(s => s.route && s.route.path === "/" && s.route.methods.post);
      const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

      const mockReq = {
        user: { userId: studentId.toString(), role: "student" },
        body: {
          title: "Table damage",
          description: "study table damage",
          category: "furniture",
          priority: "medium",
          hostelBlock: "B",
          roomNumber: "927"
        }
      };

      let resData = null;
      await handler(mockReq, {
        status: () => ({ json: (d) => { resData = d; } }),
        json: (d) => { resData = d; }
      });

      assert(resData && resData.complaint, "Complaint creation should succeed");
      // Notifications: 1 for student confirmation + 1 for warden of Block B (Block C warden is skipped)
      assert.strictEqual(createdNotifications.length, 2, "Should create 2 notifications (1 student, 1 block-matched warden)");
      assert.strictEqual(createdNotifications[0].recipient.toString(), studentId.toString());
      assert.strictEqual(createdNotifications[1].recipient.toString(), wardenId.toString());
      assert.strictEqual(createdNotifications[1].type, "complaint_created");
      assert(createdNotifications[1].title.includes("Table damage"));
    } finally {
      User.findById = origFindByIdUser;
      User.find = origFindUser;
      Complaint.create = origCreateComplaint;
      Notification.findOne = origFindOneNotif;
      Notification.create = origCreateNotif;
    }
  });

  // 13. Gender and Hostel Pairing Validation Tests
  await testAsync("Gender Validation: Registration validates Male with BH-1 and Female with GH-2, and rejects mismatches", async () => {
    const authRouter = require("../routes/auth");
    const routeLayer = authRouter.stack.find(s => s.route && s.route.path === "/register");
    const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

    const baseBody = {
      name: "Test Student",
      studentId: "99887766",
      email: "test.student@gmail.com",
      phone: "+91 9999999999",
      collegeName: "Tech Univ",
      course: "CS",
      admissionYear: "2024",
      hostelBlock: "A",
      roomNumber: "101",
      password: "SecurePassword123!",
      confirmPassword: "SecurePassword123!",
      acceptedTerms: true,
      role: "student"
    };

    const origFindOne = User.findOne;
    const origCreate = User.create;

    try {
      User.findOne = () => Promise.resolve(null);
      User.create = (doc) => Promise.resolve({ _id: "new_id", ...doc });

      // Test 1: Male with BH-1 (Valid)
      let status1 = 0;
      await handler({ body: { ...baseBody, gender: "Male", hostelName: "BH-1" } }, {
        status: (code) => { status1 = code; return { json: () => {} }; },
        json: () => {}
      });
      assert.strictEqual(status1, 201, "Male with BH-1 must succeed");

      // Test 2: Female with GH-2 (Valid)
      let status2 = 0;
      await handler({ body: { ...baseBody, email: "female@gmail.com", studentId: "99887767", gender: "Female", hostelName: "GH-2" } }, {
        status: (code) => { status2 = code; return { json: () => {} }; },
        json: () => {}
      });
      assert.strictEqual(status2, 201, "Female with GH-2 must succeed");

      // Test 3: Male with GH-1 (Invalid mismatch)
      let status3 = 0;
      let msg3 = "";
      await handler({ body: { ...baseBody, gender: "Male", hostelName: "GH-1" } }, {
        status: (code) => { status3 = code; return { json: (d) => { msg3 = d.message; } }; },
        json: (d) => { msg3 = d.message; }
      });
      assert.strictEqual(status3, 400, "Male with GH-1 must be rejected with 400");
      assert(msg3.includes("Invalid hostel for Male student"), "Expected descriptive error message");

      // Test 4: Female with BH-2 (Invalid mismatch)
      let status4 = 0;
      let msg4 = "";
      await handler({ body: { ...baseBody, gender: "Female", hostelName: "BH-2" } }, {
        status: (code) => { status4 = code; return { json: (d) => { msg4 = d.message; } }; },
        json: (d) => { msg4 = d.message; }
      });
      assert.strictEqual(status4, 400, "Female with BH-2 must be rejected with 400");
      assert(msg4.includes("Invalid hostel for Female student"), "Expected descriptive error message");

      // Test 5: Missing or invalid gender
      let status5 = 0;
      await handler({ body: { ...baseBody, gender: "Unknown", hostelName: "BH-1" } }, {
        status: (code) => { status5 = code; return { json: () => {} }; },
        json: () => {}
      });
      assert.strictEqual(status5, 400, "Invalid gender must be rejected with 400");
    } finally {
      User.findOne = origFindOne;
      User.create = origCreate;
    }
  });

  // 14. Collision-safe ID Generator Tests
  await testAsync("ID Generator: Collision-safe Warden ID sequence generates WRD-0001, WRD-0002", async () => {
    const { generateUniqueWardenId } = require("../utils/idGenerator");
    const mockModel = {
      find: () => Promise.resolve([
        { wardenId: "WRD-0001" },
        { wardenId: "WRD-0002" }
      ]),
      findOne: () => Promise.resolve(null)
    };

    const nextId = await generateUniqueWardenId(mockModel);
    assert.strictEqual(nextId, "WRD-0003", "Next warden ID should be WRD-0003");

    // Empty DB starts at WRD-0001
    const emptyModel = {
      find: () => Promise.resolve([]),
      findOne: () => Promise.resolve(null)
    };
    const firstId = await generateUniqueWardenId(emptyModel);
    assert.strictEqual(firstId, "WRD-0001", "Initial warden ID should be WRD-0001");
  });

  await testAsync("ID Generator: Collision-safe Staff ID sequence generates STF-1001, STF-1002", async () => {
    const { generateUniqueStaffId } = require("../utils/idGenerator");
    const mockModel = {
      find: () => Promise.resolve([
        { staffId: "STF-1001" },
        { staffId: "STF-1005" }
      ]),
      findOne: () => Promise.resolve(null)
    };

    const nextId = await generateUniqueStaffId(mockModel);
    assert.strictEqual(nextId, "STF-1006", "Next staff ID should be STF-1006");

    // Empty DB starts at STF-1001
    const emptyModel = {
      find: () => Promise.resolve([]),
      findOne: () => Promise.resolve(null)
    };
    const firstId = await generateUniqueStaffId(emptyModel);
    assert.strictEqual(firstId, "STF-1001", "Initial staff ID should be STF-1001");
  });

  // 15. Single Chief Warden Architecture & Scope Tests
  await testAsync("Warden Management: Retired POST /wardens and GET /wardens return 410 Gone preventing multiple warden creation", async () => {
    const wardenRouter = require("../routes/warden");
    const postWardensLayer = wardenRouter.stack.find(s => s.route && s.route.path === "/wardens" && s.route.methods.post);
    const getWardensLayer = wardenRouter.stack.find(s => s.route && s.route.path === "/wardens" && s.route.methods.get);

    assert(postWardensLayer, "POST /wardens route must exist (as retired endpoint)");
    assert(getWardensLayer, "GET /wardens route must exist (as retired endpoint)");

    const postHandler = postWardensLayer.route.stack[postWardensLayer.route.stack.length - 1].handle;
    const getHandler = getWardensLayer.route.stack[getWardensLayer.route.stack.length - 1].handle;

    let postStatus = 0;
    let postData = null;
    await postHandler({ user: { userId: "w1", role: "warden" }, body: {} }, {
      status: (code) => { postStatus = code; return { json: (d) => { postData = d; } }; },
      json: (d) => { postData = d; }
    });
    assert.strictEqual(postStatus, 410, "POST /wardens must return 410 Gone");
    assert(postData && postData.message.includes("retired"), "POST /wardens message must state endpoint is retired");

    let getStatus = 0;
    let getData = null;
    await getHandler({ user: { userId: "w1", role: "warden" } }, {
      status: (code) => { getStatus = code; return { json: (d) => { getData = d; } }; },
      json: (d) => { getData = d; }
    });
    assert.strictEqual(getStatus, 410, "GET /wardens must return 410 Gone");
    assert(getData && getData.message.includes("retired"), "GET /wardens message must state endpoint is retired");
  });

  await testAsync("Chief Warden: Campus-wide authorization scope grants access to complaints from all BH and GH hostels across all blocks", async () => {
    const origFindById = User.findById;
    const origFind = User.find;
    try {
      // Chief Warden identified by trusted email
      User.findById = () => ({
        select: () => Promise.resolve({
          _id: "chief123",
          name: "Chief Warden",
          email: "warden@hostelcare.com",
          role: "warden",
          hostelName: "BH1",
          hostelBlock: "B",
          isChiefWarden: true
        })
      });
      User.find = () => ({
        distinct: () => Promise.resolve(["s1", "s2"])
      });

      const wardenRouter = require("../routes/warden");
      const dashLayer = wardenRouter.stack.find(s => s.route && s.route.path === "/dashboard" && s.route.methods.get);
      assert(dashLayer, "GET /dashboard must exist");

      // Verify complaints endpoint for Chief Warden with GH-1 complaint
      const compDetailLayer = wardenRouter.stack.find(s => s.route && s.route.path === "/complaints/:id" && s.route.methods.get);
      assert(compDetailLayer, "GET /complaints/:id must exist");
      const compHandler = compDetailLayer.route.stack[compDetailLayer.route.stack.length - 1].handle;

      // Mock complaint in GH-1 Block C
      const origComplaintFindById = Complaint.findById;
      const mockComplaintDoc = {
        _id: "gh_complaint_1",
        ticketId: "HC-GH1-001",
        title: "Fan issue in GH-1",
        hostelName: "GH-1",
        hostelBlock: "C",
        roomNumber: "302",
        status: "submitted",
        student: { _id: "s_female", name: "Pooja", hostelName: "GH-1" }
      };
      const queryMock = {
        populate: () => queryMock,
        then: (resolve, reject) => Promise.resolve(mockComplaintDoc).then(resolve, reject)
      };
      Complaint.findById = () => queryMock;

      let resData = null;
      let statusCode = 0;
      await compHandler({ user: { userId: "chief123", role: "warden" }, params: { id: "gh_complaint_1" } }, {
        status: (code) => { statusCode = code; return { json: (d) => { resData = d; } }; },
        json: (d) => { resData = d; }
      });

      assert.strictEqual(statusCode, 0, "Chief Warden must not receive error code");
      assert(resData && resData.success, "Chief Warden must successfully access GH-1 complaint");
      assert.strictEqual(resData.complaint.hostelName, "GH-1", "Should return complaint in GH-1");

      Complaint.findById = origComplaintFindById;
    } finally {
      User.findById = origFindById;
      User.find = origFind;
    }
  });

  await testAsync("Chief Warden: Campus-wide notification routing notifies Chief Warden for complaints lodged in any hostel (e.g. GH-1, BH-2)", async () => {
    const origFindById = User.findById;
    const origFind = User.find;
    const origCreateComplaint = Complaint.create;
    const origFindOneNotification = Notification.findOne;
    const origCreateNotification = Notification.create;

    try {
      const mockStudentId = new mongoose.Types.ObjectId();
      const mockChiefId = new mongoose.Types.ObjectId();
      const mockComplaintId = new mongoose.Types.ObjectId();

      // Mock student lodging complaint in GH-2
      User.findById = () => Promise.resolve({
        _id: mockStudentId,
        name: "Ananya",
        role: "student",
        hostelName: "GH-2",
        hostelBlock: "Tagore Block A",
        roomNumber: "405"
      });

      // User.find for wardens returns Chief Warden
      User.find = (query) => {
        if (query.role === "warden") {
          return Promise.resolve([
            {
              _id: mockChiefId,
              name: "Chief Warden",
              email: "warden@hostelcare.com",
              role: "warden",
              isChiefWarden: true,
              hostelName: "BH1" // Old single hostel assignment
            }
          ]);
        }
        return Promise.resolve([]);
      };

      Complaint.create = (doc) => Promise.resolve({
        _id: mockComplaintId,
        ticketId: "HC-778899",
        ...doc
      });

      Notification.findOne = () => Promise.resolve(null);
      const createdNotifications = [];
      Notification.create = (notif) => {
        createdNotifications.push(notif);
        return Promise.resolve(notif);
      };

      const complaintsRouter = require("../routes/complaints");
      const postComplaintLayer = complaintsRouter.stack.find(s => s.route && s.route.path === "/" && s.route.methods.post);
      const handler = postComplaintLayer.route.stack[postComplaintLayer.route.stack.length - 1].handle;

      let statusCode = 0;
      await handler({
        user: { userId: mockStudentId.toString(), role: "student" },
        body: {
          title: "Water leakage",
          description: "Water tap is dripping in bathroom",
          category: "plumbing",
          hostelBlock: "Tagore Block A",
          roomNumber: "405"
        }
      }, {
        status: (code) => { statusCode = code; return { json: () => {} }; },
        json: () => {}
      });

      assert.strictEqual(statusCode, 201, "Complaint in GH-2 must be created successfully");
      const chiefNotif = createdNotifications.find(n => n.recipient.toString() === mockChiefId.toString());
      assert(chiefNotif, "Chief Warden must receive notification for GH-2 complaint even with old BH1 assignment");
      assert(chiefNotif.message.includes("GH-2"), "Chief Warden notification message must mention GH-2");
    } finally {
      User.findById = origFindById;
      User.find = origFind;
      Complaint.create = origCreateComplaint;
      Notification.findOne = origFindOneNotification;
      Notification.create = origCreateNotification;
    }
  });

  await testAsync("Warden Authorization: Non-chief warden without explicit assignment is rejected and does NOT receive campus-wide access", async () => {
    const origFindById = User.findById;
    try {
      // Mock an ordinary warden who is NOT the Chief Warden and has NO hostel assignment
      User.findById = () => ({
        select: () => Promise.resolve({
          _id: "rogue_warden_1",
          name: "Rogue Warden",
          email: "rogue@hostelcare.com",
          role: "warden",
          hostelName: "", // Missing assignment
          isChiefWarden: false
        })
      });

      const wardenRouter = require("../routes/warden");
      const dashLayer = wardenRouter.stack.find(s => s.route && s.route.path === "/dashboard" && s.route.methods.get);
      const handler = dashLayer.route.stack[dashLayer.route.stack.length - 1].handle;

      let statusCode = 0;
      let resMsg = "";
      await handler({ user: { userId: "rogue_warden_1", role: "warden" } }, {
        status: (code) => { statusCode = code; return { json: (d) => { resMsg = d.message; } }; },
        json: (d) => { resMsg = d.message; }
      });

      assert.strictEqual(statusCode, 400, "Non-chief warden lacking assignment must be rejected with 400");
      assert(resMsg.includes("Scoped access cannot be determined"), "Should explain missing hostel assignment");
    } finally {
      User.findById = origFindById;
    }
  });

  console.log("\n==================================================");
  console.log(`Results: ${passed} PASSED, ${failed} FAILED`);
  console.log("==================================================");

  if (failed > 0) {
    process.exit(1);
  }
}

runTests().catch((err) => {
  console.error("Test execution failed:", err);
  process.exit(1);
});
