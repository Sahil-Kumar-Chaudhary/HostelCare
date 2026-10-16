/**
 * HostelCare Live OpenRouter Integration & Failover Verification Script
 * Validates Tests A, B, C, D, and E using real OpenRouter API calls and HostelCareDevDB.
 */

const path = require("path");
require("dotenv").config({ path: path.resolve(__dirname, "../.env") });

const assert = require("assert");
const mongoose = require("mongoose");
const geminiService = require("../services/geminiService");
const openrouterService = require("../services/openrouterService");
const aiProviderManager = require("../services/aiProviderManager");
const Complaint = require("../models/Complaint");
const User = require("../models/User");

async function runLiveFailoverVerification() {
    console.log("==================================================");
    console.log("  Live OpenRouter & Automatic Failover Audit     ");
    console.log("==================================================\n");

    // Pre-flight DB Safety Check
    const targetDb = process.env.MONGODB_DB_NAME || "HostelCareDevDB";
    console.log(`[Safety Check] Configured Database: ${targetDb}`);
    if (targetDb !== "HostelCareDevDB") {
        console.error("FATAL: Target database is NOT HostelCareDevDB. Aborting.");
        process.exit(1);
    }

    let passed = 0;
    let failed = 0;

    async function step(name, fn) {
        try {
            aiProviderManager.resetCooldowns();
            await fn();
            console.log(`[PASS] ${name}`);
            passed++;
        } catch (err) {
            console.error(`[FAIL] ${name}`);
            console.error(`   -> ${err.stack || err.message}`);
            failed++;
        }
    }

    // ----------------------------------------------------
    // TEST A: Gemini succeeds -> OpenRouter not called
    // ----------------------------------------------------
    await step("Test A — Gemini succeeds (OpenRouter not invoked)", async () => {
        const origGemini = geminiService.analyzeComplaint;
        const origOpenRouter = openrouterService.analyzeComplaint;
        let openRouterCalled = false;

        try {
            geminiService.analyzeComplaint = async () => ({
                success: true,
                analysis: {
                    category: "plumbing",
                    priority: "high",
                    summary: "Major pipe leak in corridor"
                },
                provider: "gemini"
            });

            openrouterService.analyzeComplaint = async () => {
                openRouterCalled = true;
                return { success: true };
            };

            const result = await aiProviderManager.analyzeComplaint(
                "Pipe leak",
                "Water flooding corridor",
                "plumbing"
            );

            assert.strictEqual(result.success, true);
            assert.strictEqual(result.provider, "gemini");
            assert.strictEqual(result.failedOver, false);
            assert.strictEqual(openRouterCalled, false, "OpenRouter must not be called when Gemini succeeds");
            assert.strictEqual(aiProviderManager.isCoolingDown("gemini"), false);
        } finally {
            geminiService.analyzeComplaint = origGemini;
            openrouterService.analyzeComplaint = origOpenRouter;
        }
    });

    // ----------------------------------------------------
    // TEST B: Gemini 503 -> Real OpenRouter live request
    // ----------------------------------------------------
    await step("Test B — Gemini 503 triggers real OpenRouter live API request", async () => {
        const origGemini = geminiService.analyzeComplaint;

        try {
            // Inject 503 into Gemini
            geminiService.analyzeComplaint = async () => ({
                success: false,
                error: "This model is currently experiencing high demand. Spikes in demand are usually temporary.",
                errorCategory: "transient_error",
                status: 503
            });

            // openrouterService.analyzeComplaint is the REAL live function
            const startTime = Date.now();
            const result = await aiProviderManager.analyzeComplaint(
                "Leaking tap",
                "A bathroom tap is leaking continuously.",
                "plumbing"
            );
            const elapsed = Date.now() - startTime;

            console.log(`       -> Live fallback completed in ${elapsed}ms`);
            console.log(`       -> Provider: ${result.provider}, FailedOver: ${result.failedOver}`);
            console.log(`       -> Analysis: ${JSON.stringify(result.analysis)}`);

            assert.strictEqual(result.success, true, "Failover to real OpenRouter must succeed");
            assert.strictEqual(result.provider, "openrouter");
            assert.strictEqual(result.failedOver, true);
            assert(result.analysis && result.analysis.category, "Must return validated category");
            assert(result.analysis.priority, "Must return validated priority");
            assert(result.analysis.summary, "Must return validated summary");
            assert.strictEqual(aiProviderManager.isCoolingDown("gemini"), true, "Gemini must be in cooldown after 503");
        } finally {
            geminiService.analyzeComplaint = origGemini;
        }
    });

    // ----------------------------------------------------
    // TEST C: Gemini 429 -> Rate limit handling & cooldown
    // ----------------------------------------------------
    await step("Test C — Gemini 429 quota exhaustion handles rate limiting and cooldown", async () => {
        const origGemini = geminiService.analyzeComplaint;

        try {
            // Inject 429 quota exhausted into Gemini
            geminiService.analyzeComplaint = async () => ({
                success: false,
                error: "Resource has been exhausted (quota exceeded)",
                errorCategory: "quota_exceeded",
                status: 429,
                retryAfterMs: 60000
            });

            const result = await aiProviderManager.analyzeComplaint(
                "Broken switch",
                "Electrical sparks from room main switch",
                "electrical"
            );

            assert.strictEqual(result.success, true);
            assert.strictEqual(result.provider, "openrouter");
            assert.strictEqual(result.failedOver, true);
            assert.strictEqual(aiProviderManager.isCoolingDown("gemini"), true);

            // While in cooldown, verify Gemini is bypassed directly
            let geminiCalledWhileCooling = false;
            geminiService.analyzeComplaint = async () => {
                geminiCalledWhileCooling = true;
                return { success: false };
            };

            const secondResult = await aiProviderManager.analyzeComplaint(
                "Door broken",
                "Hostel door lock damaged",
                "furniture"
            );

            assert.strictEqual(secondResult.success, true);
            assert.strictEqual(secondResult.provider, "openrouter");
            assert.strictEqual(geminiCalledWhileCooling, false, "Gemini should be bypassed while in cooldown");
        } finally {
            geminiService.analyzeComplaint = origGemini;
        }
    });

    // ----------------------------------------------------
    // TEST D: Both providers fail -> Recoverable error
    // ----------------------------------------------------
    await step("Test D — Both providers fail returns recoverable 503 without crashing", async () => {
        const origGemini = geminiService.analyzeComplaint;
        const origOpenRouter = openrouterService.analyzeComplaint;

        try {
            geminiService.analyzeComplaint = async () => ({
                success: false,
                error: "Gemini 503 unavailable",
                errorCategory: "transient_error",
                status: 503
            });

            openrouterService.analyzeComplaint = async () => ({
                success: false,
                error: "OpenRouter rate limited",
                errorCategory: "quota_exceeded",
                status: 429
            });

            const result = await aiProviderManager.analyzeComplaint(
                "Corridor light out",
                "Bulb fused",
                "electrical"
            );

            assert.strictEqual(result.success, false);
            assert.strictEqual(result.status, 503);
            assert(result.error.includes("unavailable"), "Should return user-friendly error");
            assert(result.details, "Should include details of failures");
        } finally {
            geminiService.analyzeComplaint = origGemini;
            openrouterService.analyzeComplaint = origOpenRouter;
        }
    });

    // ----------------------------------------------------
    // TEST E: Existing complaint retry in HostelCareDevDB
    // ----------------------------------------------------
    await step("Test E — Complaint persistence & retry without duplicate creation in HostelCareDevDB", async () => {
        await mongoose.connect(process.env.MONGO_URI, { dbName: targetDb });
        assert.strictEqual(mongoose.connection.name, "HostelCareDevDB");

        // 1. Create a dummy student if needed or find existing
        let testStudent = await User.findOne({ role: "student" });
        if (!testStudent) {
            testStudent = await User.create({
                name: "Test Verification Student",
                studentId: "TEST-STU-999",
                email: "test.failover.student@gmail.com",
                password: "$2a$10$e8wF4r719G0.hashed.password.here",
                hostelName: "BH1",
                hostelBlock: "B",
                roomNumber: "101",
                role: "student"
            });
        }

        const initialComplaintCount = await Complaint.countDocuments();

        // 2. Create a test complaint (submission independent of AI)
        const testComplaint = await Complaint.create({
            title: "Live tap dripping",
            description: "Water tap dripping slowly in washroom",
            category: "plumbing",
            priority: "low",
            status: "submitted",
            hostelName: "BH1",
            hostelBlock: "B",
            roomNumber: "101",
            student: testStudent._id,
            ticketId: "HC-VERIFY-" + Date.now().toString().slice(-6)
        });

        const countAfterCreation = await Complaint.countDocuments();
        assert.strictEqual(countAfterCreation, initialComplaintCount + 1, "Exactly one complaint created");

        try {
            // 3. Test AI analysis on the existing complaint using routes/ai
            const express = require("express");
            const aiRouter = require("../routes/ai");
            const routeLayer = aiRouter.stack.find(s => s.route && s.route.path === "/analyze-complaint" && s.route.methods.post);
            const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

            const mockReq = {
                user: { userId: testStudent._id.toString(), role: "student" },
                body: { complaintId: testComplaint._id.toString() }
            };

            let resCode = 0;
            let resData = null;
            const mockRes = {
                status: (c) => { resCode = c; return { json: (d) => { resData = d; } }; },
                json: (d) => { resCode = 200; resData = d; }
            };

            // Inject 503 into Gemini to force failover to real OpenRouter on the DB complaint
            const origGemini = geminiService.analyzeComplaint;
            geminiService.analyzeComplaint = async () => ({
                success: false,
                error: "High demand spike",
                errorCategory: "transient_error",
                status: 503
            });

            try {
                await handler(mockReq, mockRes);
            } finally {
                geminiService.analyzeComplaint = origGemini;
            }

            assert.strictEqual(resCode, 200, "AI analysis on existing complaint must return 200");
            assert.strictEqual(resData.provider, "openrouter");
            assert.strictEqual(resData.failedOver, true);
            assert.strictEqual(resData.complaint._id.toString(), testComplaint._id.toString());
            assert.strictEqual(resData.complaint.ticketId, testComplaint.ticketId);
            assert.strictEqual(resData.complaint.status, "submitted", "Status must remain submitted");
            assert(resData.complaint.aiSummary, "aiSummary must be populated");

            // 4. Verify DB count: NO DUPLICATE CREATED
            const countAfterAnalysis = await Complaint.countDocuments();
            assert.strictEqual(countAfterAnalysis, countAfterCreation, "Complaint count must not increase after AI analysis");

            // 5. Test AI retry when both providers fail: Status and complaint preserved
            const origOR = openrouterService.analyzeComplaint;
            geminiService.analyzeComplaint = async () => ({ success: false, status: 503 });
            openrouterService.analyzeComplaint = async () => ({ success: false, status: 503 });

            let retryCode = 0;
            let retryData = null;
            const mockResRetry = {
                status: (c) => { retryCode = c; return { json: (d) => { retryData = d; } }; },
                json: (d) => { retryCode = 200; retryData = d; }
            };

            try {
                await handler(mockReq, mockResRetry);
            } finally {
                geminiService.analyzeComplaint = origGemini;
                openrouterService.analyzeComplaint = origOR;
            }

            assert.strictEqual(retryCode, 503, "Should return 503 when providers fail");

            // Verify complaint is STILL in DB with unchanged status and ticket ID
            const fetchedComplaint = await Complaint.findById(testComplaint._id);
            assert(fetchedComplaint, "Complaint must still exist after failed AI retry");
            assert.strictEqual(fetchedComplaint.status, "submitted");
            assert.strictEqual(fetchedComplaint.ticketId, testComplaint.ticketId);

            const finalCount = await Complaint.countDocuments();
            assert.strictEqual(finalCount, countAfterCreation, "Total complaint count must remain unchanged");

            console.log("       -> Complaint ID preserved:", testComplaint._id.toString());
            console.log("       -> Ticket ID preserved:", testComplaint.ticketId);
            console.log("       -> Complaint status preserved:", fetchedComplaint.status);
            console.log("       -> Zero duplicate complaints created.");

        } finally {
            // Clean up the test complaint
            await Complaint.deleteOne({ _id: testComplaint._id });
            await mongoose.disconnect();
        }
    });

    console.log("\n==================================================");
    console.log(`Live Failover Verification: ${passed} PASSED, ${failed} FAILED`);
    console.log("==================================================\n");

    if (failed > 0) {
        process.exit(1);
    }
}

if (require.main === module) {
    runLiveFailoverVerification().catch(err => {
        console.error("Live Verification Uncaught Error:", err);
        process.exit(1);
    });
}

module.exports = runLiveFailoverVerification;
