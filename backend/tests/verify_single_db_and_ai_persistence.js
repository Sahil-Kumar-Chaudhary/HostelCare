/**
 * Non-destructive Single Database & AI Idempotency Integration Test
 * Targets canonical HostelCareDB with isolated, self-cleaning test documents.
 */

const path = require("path");
require("dotenv").config({ path: path.resolve(__dirname, "../.env") });

const assert = require("assert");
const mongoose = require("mongoose");
const bcrypt = require("bcryptjs");
const User = require("../models/User");
const Complaint = require("../models/Complaint");
const Notification = require("../models/Notification");
const aiRouter = require("../routes/ai");
const aiProviderManager = require("../services/aiProviderManager");

async function runSingleDbAndIdempotencyVerification() {
  console.log("==================================================================");
  console.log("   HostelCare Single Database & AI Idempotency Live Test Suite    ");
  console.log("==================================================================\n");

  const uri = process.env.MONGO_URI;
  if (!uri) throw new Error("MONGO_URI is missing");

  const targetDbName = process.env.MONGODB_DB_NAME || "HostelCareDB";
  console.log(`Connecting to database target: ${targetDbName}...`);
  await mongoose.connect(uri, { dbName: targetDbName });
  assert.strictEqual(mongoose.connection.name, "HostelCareDB", "Target must be strictly HostelCareDB");
  console.log(`[PASS] Connected securely to canonical application database: ${mongoose.connection.name}`);

  // 1. Verify cluster database list (HostelCareDevDB retired)
  const adminDb = mongoose.connection.db.admin();
  const dbsList = await adminDb.listDatabases();
  const currentDbNames = dbsList.databases.map(d => d.name);
  console.log(`Current cluster databases: ${currentDbNames.join(", ")}`);
  assert(currentDbNames.includes("HostelCareDB"), "HostelCareDB must exist");
  assert(!currentDbNames.includes("HostelCareDevDB"), "HostelCareDevDB must NOT exist (retired)");
  console.log("[PASS] HostelCareDevDB is confirmed retired and absent from cluster.");

  // 2. Verify fresh Chief Warden account
  const warden = await User.findOne({ email: "warden@hostelcare.com" });
  assert(warden, "Chief Warden must exist in HostelCareDB");
  assert.strictEqual(warden.role, "warden");
  assert.strictEqual(warden.hostelName, "BH1");
  assert.strictEqual(warden.hostelBlock, "B");
  console.log(`[PASS] Fresh Chief Warden verified: ${warden.name} (${warden.email}), Hostel: ${warden.hostelName}, Block: ${warden.hostelBlock}`);

  const testId = `TEST_${Date.now()}`;
  let testStudent = null;
  let testComplaint = null;
  let legacyComplaint = null;

  try {
    // 3. Create isolated test student
    const hashedPassword = await bcrypt.hash("TestStudent123!", 10);
    testStudent = await User.create({
      name: `Test Student ${testId}`,
      email: `student_${testId}@hostelcare.test`,
      password: hashedPassword,
      studentId: `STU_${testId.slice(-6)}`,
      role: "student",
      phone: "9876543210",
      hostelName: "BH1",
      hostelBlock: "B",
      roomNumber: "201"
    });
    console.log(`[PASS] Created isolated test student: ${testStudent.email}`);

    // 4. Create isolated test complaint
    testComplaint = await Complaint.create({
      title: `Leaking Washroom Tap [${testId}]`,
      description: "Water tap is dripping continuously and won't turn off completely.",
      category: "plumbing",
      priority: "medium",
      student: testStudent._id,
      hostelName: "BH1",
      hostelBlock: "B",
      roomNumber: "201",
      status: "submitted",
      aiStatus: "not_started",
      timeline: [
        {
          status: "submitted",
          note: "Test complaint submitted",
          updatedBy: testStudent._id
        }
      ]
    });
    console.log(`[PASS] Created isolated test complaint: ${testComplaint.ticketId} (ID: ${testComplaint._id})`);
    assert.strictEqual(testComplaint.aiStatus, "not_started");
    assert.strictEqual(testComplaint.status, "submitted");

    // 5. Route handler reference
    const routeLayer = aiRouter.stack.find(s => s.route && s.route.path === "/analyze-complaint" && s.route.methods.post);
    const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

    let providerCalls = 0;
    const origAnalyze = aiProviderManager.analyzeComplaint;
    aiProviderManager.analyzeComplaint = async (title, desc, cat, photo) => {
      providerCalls++;
      return {
        success: true,
        analysis: {
          category: "plumbing",
          priority: "high",
          summary: "Continuous dripping water valve in room washroom."
        },
        provider: "gemini",
        failedOver: false
      };
    };

    const mockReq = {
      user: { userId: testStudent._id.toString(), role: "student" },
      body: { complaintId: testComplaint._id.toString() }
    };

    let responseData = null;
    let statusCode = 0;
    const mockRes = {
      status: (code) => { statusCode = code; return { json: (d) => { responseData = d; } }; },
      json: (d) => { responseData = d; }
    };

    // 6. Test First Analysis Execution
    console.log("\n[Test Flow 1] First AI analysis call...");
    await handler(mockReq, mockRes);
    assert.strictEqual(statusCode, 200);
    assert.strictEqual(providerCalls, 1, "Provider must be called on first execution");
    assert.strictEqual(responseData.complaint.aiStatus, "completed");
    assert.strictEqual(responseData.complaint.priority, "high");
    assert.strictEqual(responseData.complaint.aiSummary, "Continuous dripping water valve in room washroom.");
    console.log("  -> First analysis succeeded and persisted to HostelCareDB.");

    // Refresh from DB
    const refreshed1 = await Complaint.findById(testComplaint._id);
    assert.strictEqual(refreshed1.aiStatus, "completed");
    assert.strictEqual(refreshed1.aiSummary, "Continuous dripping water valve in room washroom.");
    assert.strictEqual(refreshed1.status, "submitted", "Ticket lifecycle status preserved");

    // 7. Test Reopening / Repeated Request (Idempotency)
    console.log("\n[Test Flow 2] Reopening complaint details (second call without forceRetry)...");
    await handler(mockReq, mockRes);
    assert.strictEqual(statusCode, 200);
    assert.strictEqual(responseData.cached, true, "Must return cached assessment");
    assert.strictEqual(providerCalls, 1, "AI provider MUST NOT be called again on repeated screen open!");
    console.log("  -> Idempotent cache returned successfully. Zero additional provider calls.");

    // 8. Test Explicit Retry with forceRetry = true
    console.log("\n[Test Flow 3] Explicit Retry (forceRetry: true)...");
    mockReq.body.forceRetry = true;
    await handler(mockReq, mockRes);
    assert.strictEqual(statusCode, 200);
    assert.strictEqual(providerCalls, 2, "forceRetry must invoke provider");
    console.log("  -> Explicit retry re-analyzed and persisted.");

    // 9. Test Backward Compatibility with legacy record lacking aiStatus
    console.log("\n[Test Flow 4] Backward compatibility for records without aiStatus...");
    legacyComplaint = await Complaint.create({
      title: `Legacy Chair Leg Loose [${testId}]`,
      description: "Chair leg is wobbling and loose.",
      category: "furniture",
      priority: "low",
      student: testStudent._id,
      hostelName: "BH1",
      hostelBlock: "B",
      roomNumber: "201",
      aiSummary: "Wobbly chair leg needing tightening.",
      status: "submitted"
    });
    // Strip aiStatus to simulate pre-migration documents
    await Complaint.updateOne({ _id: legacyComplaint._id }, { $unset: { aiStatus: "" } });

    mockReq.body = { complaintId: legacyComplaint._id.toString() };
    await handler(mockReq, mockRes);
    assert.strictEqual(statusCode, 200);
    assert.strictEqual(responseData.cached, true, "Legacy completed record served from cache");
    assert.strictEqual(providerCalls, 2, "No provider call for legacy record with aiSummary");
    console.log("  -> Legacy record without aiStatus successfully served from cache.");

    // Clean up legacy test complaint
    await Complaint.deleteOne({ _id: legacyComplaint._id });
    legacyComplaint = null;

    aiProviderManager.analyzeComplaint = origAnalyze;

  } finally {
    // Guaranteed cleanup of ONLY the test records
    console.log("\nCleaning up test records...");
    if (legacyComplaint && legacyComplaint._id) {
      await Complaint.deleteOne({ _id: legacyComplaint._id });
      console.log(`  -> Deleted legacy test complaint ${legacyComplaint._id}`);
    }
    if (testComplaint && testComplaint._id) {
      await Complaint.deleteOne({ _id: testComplaint._id });
      console.log(`  -> Deleted test complaint ${testComplaint._id}`);
    }
    if (testStudent && testStudent._id) {
      await User.deleteOne({ _id: testStudent._id });
      console.log(`  -> Deleted test student ${testStudent._id}`);
    }
    await Notification.deleteMany({ recipient: { $in: [testStudent ? testStudent._id : null] } });

    await mongoose.disconnect();
    console.log("Disconnected from MongoDB.");
  }

  console.log("\n==================================================================");
  console.log("   Single Database & AI Idempotency Live Test PASSED 100%         ");
  console.log("==================================================================\n");
}

if (require.main === module) {
  runSingleDbAndIdempotencyVerification().catch((err) => {
    console.error("Test failed:", err);
    process.exit(1);
  });
}

module.exports = runSingleDbAndIdempotencyVerification;
