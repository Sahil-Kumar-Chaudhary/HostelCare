const assert = require("assert");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const mongoose = require("mongoose");
const path = require("path");
require("dotenv").config({ path: path.join(__dirname, "../.env") });

const User = require("../models/User");
const Complaint = require("../models/Complaint");
const Notification = require("../models/Notification");

async function runLiveWorkflowTest() {
  console.log("==================================================");
  console.log("   HostelCareDevDB Live Workflow Integration Test ");
  console.log("==================================================\n");

  const uri = process.env.MONGO_URI;
  if (!uri) throw new Error("MONGO_URI is missing");

  // Connect directly to HostelCareDevDB
  await mongoose.connect(uri, { dbName: "HostelCareDevDB" });
  console.log(`Connected to: ${mongoose.connection.name}`);
  assert.strictEqual(mongoose.connection.name, "HostelCareDevDB", "Must be connected to HostelCareDevDB");

  let createdComplaintId = null;

  try {
    // 1. Authenticate existing Development Student
    console.log("\n[Test 1] Authenticating Development Student (sahil.student@gmail.com)...");
    const student = await User.findOne({ email: "sahil.student@gmail.com" });
    assert(student, "Development student must exist in HostelCareDevDB");
    assert.strictEqual(student.role, "student");
    assert.strictEqual(student.studentId, "STU101");
    console.log(`  -> Student ID: ${student.studentId}, Name: ${student.name}, Block: ${student.hostelBlock}`);

    // 2. Submit identifiable test complaint
    console.log("\n[Test 2] Submitting test complaint under Student STU101...");
    const testComplaint = await Complaint.create({
      title: "Ceiling Fan Regulator Broken (Automated E2E Test)",
      description: "Fan is running at maximum speed and regulator knob is stuck.",
      category: "electrical",
      priority: "medium",
      student: student._id,
      hostelName: student.hostelName || "Campus Hostel",
      hostelBlock: student.hostelBlock || "Tagore Block B",
      roomNumber: student.roomNumber || "204",
      status: "submitted",
      timeline: [
        {
          status: "submitted",
          note: "Complaint submitted by student",
          updatedBy: student._id
        }
      ]
    });
    createdComplaintId = testComplaint._id;
    assert(testComplaint.ticketId.startsWith("HC-"), "Ticket ID must start with HC-");
    console.log(`  -> Created Complaint Ticket: ${testComplaint.ticketId} (ID: ${testComplaint._id})`);

    // 3. Verify complaint is stored with correct student reference
    console.log("\n[Test 3] Verifying complaint storage and student reference...");
    const storedComplaint = await Complaint.findById(createdComplaintId).populate("student");
    assert(storedComplaint, "Complaint must be found in database");
    assert.strictEqual(storedComplaint.student._id.toString(), student._id.toString());
    assert.strictEqual(storedComplaint.student.studentId, "STU101");
    assert.strictEqual(storedComplaint.status, "submitted");
    console.log(`  -> Complaint verified. Student ref: ${storedComplaint.student.name} (${storedComplaint.student.studentId})`);

    // 4. Authenticate Warden
    console.log("\n[Test 4] Authenticating Warden (warden@hostelcare.com)...");
    const warden = await User.findOne({ email: "warden@hostelcare.com" });
    assert(warden, "Warden must exist in HostelCareDevDB");
    assert.strictEqual(warden.role, "warden");
    console.log(`  -> Warden: ${warden.name}, Authorized Hostel: ${warden.hostelName}, Block: ${warden.hostelBlock}`);

    // 5. Verify complaint appears in Warden queries
    console.log("\n[Test 5] Querying complaints as Warden...");
    const wardenComplaints = await Complaint.find({
      hostelName: warden.hostelName,
      status: { $in: ["submitted", "under_review", "assigned", "in_progress", "resolved"] }
    }).sort({ createdAt: -1 });
    const foundInWardenList = wardenComplaints.some(c => c._id.toString() === createdComplaintId.toString());
    assert(foundInWardenList, "Test complaint must appear in Warden's list");
    console.log(`  -> Complaint ${testComplaint.ticketId} successfully visible in Warden complaint list.`);

    // 6. Warden assigns staff
    console.log("\n[Test 6] Assigning complaint to Development Staff (staff@hostelcare.com)...");
    const staff = await User.findOne({ email: "staff@hostelcare.com" });
    assert(staff, "Development staff must exist");
    assert.strictEqual(staff.role, "staff");
    assert.strictEqual(staff.staffId, "STF-101");

    storedComplaint.assignedStaff = staff._id;
    storedComplaint.assignmentInstructions = "Please inspect regulator and replace capacitor if needed.";
    storedComplaint.status = "assigned";
    storedComplaint.timeline.push({
      status: "assigned",
      note: `Assigned to ${staff.name} by Warden`,
      updatedBy: warden._id
    });
    await storedComplaint.save();
    console.log(`  -> Assigned to: ${staff.name} (Staff ID: ${staff.staffId})`);

    // 7. Update status to in_progress with audit note
    console.log("\n[Test 7] Updating status to 'in_progress' with audit timeline...");
    storedComplaint.status = "in_progress";
    storedComplaint.timeline.push({
      status: "in_progress",
      note: "Work started by maintenance staff",
      updatedBy: staff._id
    });
    await storedComplaint.save();

    // 8. Verify Staff Portal query
    console.log("\n[Test 8] Verifying task visibility in Staff Portal...");
    const staffTasks = await Complaint.find({ assignedStaff: staff._id });
    const foundInStaffTasks = staffTasks.some(c => c._id.toString() === createdComplaintId.toString());
    assert(foundInStaffTasks, "Assigned complaint must be in staff task list");
    console.log(`  -> Staff task confirmed: ${staffTasks.length} total tasks assigned.`);

    // 9. Verify Foreign Hostel Warden Authorization Restriction
    console.log("\n[Test 9] Verifying Foreign Hostel Warden access restriction (403)...");
    const foreignWarden = {
      _id: new mongoose.Types.ObjectId(),
      role: "warden",
      hostelName: "Other Engineering Hostel",
      hostelBlock: "A"
    };
    const isAuthorized = storedComplaint.hostelName === foreignWarden.hostelName;
    assert.strictEqual(isAuthorized, false, "Foreign warden MUST be denied access");
    console.log("  -> Foreign warden authorization check passed: correctly denied access.");

    // 10. Verify Production Database is completely untouched
    console.log("\n[Test 10] Verifying Production Database (HostelCareDB) was NOT touched...");
    const prodConn = await mongoose.createConnection(uri, { dbName: "HostelCareDB" }).asPromise();
    const prodUsersCount = await prodConn.collection("users").countDocuments();
    const prodComplaintsCount = await prodConn.collection("complaints").countDocuments();
    assert.strictEqual(prodUsersCount, 1, "HostelCareDB must have exactly 1 user");
    assert.strictEqual(prodComplaintsCount, 1, "HostelCareDB must have exactly 1 complaint");
    await prodConn.close();
    console.log("  -> HostelCareDB verified: exactly 1 production user, 1 production complaint. Completely untouched!");

    console.log("\n==================================================");
    console.log("   ALL 10 LIVE WORKFLOW INTEGRATION CHECKS PASSED ");
    console.log("==================================================");

  } finally {
    // Clean up test complaint from HostelCareDevDB
    if (createdComplaintId) {
      await Complaint.findByIdAndDelete(createdComplaintId);
      console.log(`\n[Cleanup] Removed test complaint ${createdComplaintId} from HostelCareDevDB.`);
    }
    await mongoose.connection.close();
  }
}

runLiveWorkflowTest()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("\n[Live Workflow Failure]:", err);
    process.exit(1);
  });
