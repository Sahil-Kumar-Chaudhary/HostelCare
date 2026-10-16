/**
 * HostelCare AI Failover & Multimodal Test Suite
 * Tests all 13 failover, schema validation, cooldown, and edge-case scenarios.
 */

const path = require("path");
require("dotenv").config({ path: path.resolve(__dirname, "../.env") });

const assert = require("assert");
const mongoose = require("mongoose");
const aiValidation = require("../services/aiValidation");
const geminiService = require("../services/geminiService");
const openrouterService = require("../services/openrouterService");
const aiProviderManager = require("../services/aiProviderManager");
const Complaint = require("../models/Complaint");
const User = require("../models/User");

async function runAiFailoverTests() {
    console.log("==================================================");
    console.log("   HostelCare AI Provider Failover Test Suite     ");
    console.log("==================================================\n");

    let passed = 0;
    let failed = 0;

    async function test(name, fn) {
        try {
            aiProviderManager.resetCooldowns();
            await fn();
            console.log(`PASS: [${passed + 1}] ${name}`);
            passed++;
        } catch (err) {
            console.error(`FAIL: ${name}`);
            console.error(`   -> ${err.stack || err.message}`);
            failed++;
        }
    }

    // SCENARIO 1: Gemini succeeds (normal operation)
    await test("Scenario 1: Primary provider (Gemini) succeeds, OpenRouter is not invoked", async () => {
        const origGemini = geminiService.analyzeComplaint;
        const origOpenRouter = openrouterService.analyzeComplaint;
        const origGeminiConfigured = geminiService.isConfigured;
        const origORConfigured = openrouterService.isConfigured;
        let openRouterCalled = false;

        try {
            geminiService.isConfigured = () => true;
            openrouterService.isConfigured = () => true;

            geminiService.analyzeComplaint = async () => ({
                success: true,
                analysis: {
                    category: "plumbing",
                    priority: "high",
                    summary: "Severe water pipe leak in bathroom"
                },
                provider: "gemini"
            });

            openrouterService.analyzeComplaint = async () => {
                openRouterCalled = true;
                return { success: true };
            };

            const result = await aiProviderManager.analyzeComplaint("Pipe Leak", "Water flooding bathroom floor", "plumbing");

            assert.strictEqual(result.success, true);
            assert.strictEqual(result.provider, "gemini");
            assert.strictEqual(result.failedOver, false);
            assert.strictEqual(result.analysis.category, "plumbing");
            assert.strictEqual(result.analysis.priority, "high");
            assert.strictEqual(openRouterCalled, false, "OpenRouter must not be invoked when Gemini succeeds");
            assert.strictEqual(aiProviderManager.isCoolingDown("gemini"), false);
        } finally {
            geminiService.analyzeComplaint = origGemini;
            openrouterService.analyzeComplaint = origOpenRouter;
            geminiService.isConfigured = origGeminiConfigured;
            openrouterService.isConfigured = origORConfigured;
        }
    });

    // SCENARIO 2: Gemini 429 quota exhaustion -> failover to OpenRouter
    await test("Scenario 2: Gemini HTTP 429 quota exhaustion triggers automatic failover to OpenRouter", async () => {
        const origGemini = geminiService.analyzeComplaint;
        const origOpenRouter = openrouterService.analyzeComplaint;
        const origGeminiConfigured = geminiService.isConfigured;
        const origORConfigured = openrouterService.isConfigured;

        try {
            geminiService.isConfigured = () => true;
            openrouterService.isConfigured = () => true;

            geminiService.analyzeComplaint = async () => ({
                success: false,
                error: "Resource has been exhausted (e.g. check quota)",
                errorCategory: "quota_exceeded",
                status: 429,
                retryAfterMs: 60000
            });

            let openRouterInvoked = false;
            openrouterService.analyzeComplaint = async () => {
                openRouterInvoked = true;
                return {
                    success: true,
                    analysis: {
                        category: "electrical",
                        priority: "medium",
                        summary: "Tube light flickering continuously in study room"
                    },
                    provider: "openrouter"
                };
            };

            const result = await aiProviderManager.analyzeComplaint("Light flickering", "Room light blinking rapidly", "electrical");

            assert.strictEqual(result.success, true);
            assert.strictEqual(result.provider, "openrouter");
            assert.strictEqual(result.failedOver, true);
            assert.strictEqual(openRouterInvoked, true, "OpenRouter must be invoked upon Gemini 429");
            assert.strictEqual(result.analysis.category, "electrical");
            assert.strictEqual(aiProviderManager.isCoolingDown("gemini"), true, "Gemini must be placed in cooldown");
        } finally {
            geminiService.analyzeComplaint = origGemini;
            openrouterService.analyzeComplaint = origOpenRouter;
            geminiService.isConfigured = origGeminiConfigured;
            openrouterService.isConfigured = origORConfigured;
        }
    });

    // SCENARIO 3: Gemini 503 high demand -> failover to OpenRouter
    await test("Scenario 3: Gemini HTTP 503 high demand triggers transparent failover to OpenRouter", async () => {
        const origGemini = geminiService.analyzeComplaint;
        const origOpenRouter = openrouterService.analyzeComplaint;
        const origGeminiConfigured = geminiService.isConfigured;
        const origORConfigured = openrouterService.isConfigured;

        try {
            geminiService.isConfigured = () => true;
            openrouterService.isConfigured = () => true;

            geminiService.analyzeComplaint = async () => ({
                success: false,
                error: "This model is currently experiencing high demand. Spikes in demand are usually temporary.",
                errorCategory: "transient_error",
                status: 503
            });

            openrouterService.analyzeComplaint = async () => ({
                success: true,
                analysis: {
                    category: "cleaning",
                    priority: "low",
                    summary: "Corridor trash bin overflowing"
                },
                provider: "openrouter"
            });

            const result = await aiProviderManager.analyzeComplaint("Trash full", "Dustbin not cleared for 2 days", "cleaning");

            assert.strictEqual(result.success, true);
            assert.strictEqual(result.provider, "openrouter");
            assert.strictEqual(result.failedOver, true);
            assert.strictEqual(result.analysis.priority, "low");
            assert.strictEqual(aiProviderManager.isCoolingDown("gemini"), true);
        } finally {
            geminiService.analyzeComplaint = origGemini;
            openrouterService.analyzeComplaint = origOpenRouter;
            geminiService.isConfigured = origGeminiConfigured;
            openrouterService.isConfigured = origORConfigured;
        }
    });

    // SCENARIO 4: Gemini timeout / network error -> failover to OpenRouter
    await test("Scenario 4: Gemini timeout or network error triggers failover to OpenRouter", async () => {
        const origGemini = geminiService.analyzeComplaint;
        const origOpenRouter = openrouterService.analyzeComplaint;
        const origGeminiConfigured = geminiService.isConfigured;
        const origORConfigured = openrouterService.isConfigured;

        try {
            geminiService.isConfigured = () => true;
            openrouterService.isConfigured = () => true;

            geminiService.analyzeComplaint = async () => ({
                success: false,
                error: "Gemini request timed out after 15000ms",
                errorCategory: "timeout",
                status: 504
            });

            openrouterService.analyzeComplaint = async () => ({
                success: true,
                analysis: {
                    category: "internet",
                    priority: "high",
                    summary: "Wi-Fi access point offline in Block B"
                },
                provider: "openrouter"
            });

            const result = await aiProviderManager.analyzeComplaint("No WiFi", "Hostel wifi has no internet connection", "internet");

            assert.strictEqual(result.success, true);
            assert.strictEqual(result.provider, "openrouter");
            assert.strictEqual(result.failedOver, true);
            assert.strictEqual(aiProviderManager.isCoolingDown("gemini"), true);
        } finally {
            geminiService.analyzeComplaint = origGemini;
            openrouterService.analyzeComplaint = origOpenRouter;
            geminiService.isConfigured = origGeminiConfigured;
            openrouterService.isConfigured = origORConfigured;
        }
    });

    // SCENARIO 5: Both providers fail -> clean graceful error
    await test("Scenario 5: Both providers fail returns graceful 503 response without server crash", async () => {
        const origGemini = geminiService.analyzeComplaint;
        const origOpenRouter = openrouterService.analyzeComplaint;
        const origGeminiConfigured = geminiService.isConfigured;
        const origORConfigured = openrouterService.isConfigured;

        try {
            geminiService.isConfigured = () => true;
            openrouterService.isConfigured = () => true;

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

            const result = await aiProviderManager.analyzeComplaint("Door lock broken", "Key stuck inside latch", "furniture");

            assert.strictEqual(result.success, false);
            assert.strictEqual(result.status, 503);
            assert(result.error.includes("unavailable"), "Should provide user-friendly error message");
            assert(result.details, "Should contain diagnostic failure details");
            assert.strictEqual(aiProviderManager.isCoolingDown("gemini"), true);
            assert.strictEqual(aiProviderManager.isCoolingDown("openrouter"), true);
        } finally {
            geminiService.analyzeComplaint = origGemini;
            openrouterService.analyzeComplaint = origOpenRouter;
            geminiService.isConfigured = origGeminiConfigured;
            openrouterService.isConfigured = origORConfigured;
        }
    });

    // SCENARIO 6: Gemini configured, OpenRouter missing
    await test("Scenario 6: Gemini configured and OpenRouter missing works when Gemini is healthy", async () => {
        const origGeminiConfigured = geminiService.isConfigured;
        const origORConfigured = openrouterService.isConfigured;
        const origGemini = geminiService.analyzeComplaint;

        try {
            geminiService.isConfigured = () => true;
            openrouterService.isConfigured = () => false;

            geminiService.analyzeComplaint = async () => ({
                success: true,
                analysis: {
                    category: "furniture",
                    priority: "low",
                    summary: "Study chair wheel detached"
                },
                provider: "gemini"
            });

            const result = await aiProviderManager.analyzeComplaint("Chair broken", "Wheel came off", "furniture");
            assert.strictEqual(result.success, true);
            assert.strictEqual(result.provider, "gemini");
            assert.strictEqual(result.failedOver, false);
        } finally {
            geminiService.isConfigured = origGeminiConfigured;
            openrouterService.isConfigured = origORConfigured;
            geminiService.analyzeComplaint = origGemini;
        }
    });

    // SCENARIO 7: OpenRouter configured, Gemini missing
    await test("Scenario 7: OpenRouter configured and Gemini unconfigured routes directly to OpenRouter", async () => {
        const origGeminiConfigured = geminiService.isConfigured;
        const origORConfigured = openrouterService.isConfigured;
        const origOpenRouter = openrouterService.analyzeComplaint;
        let geminiAttempted = false;

        try {
            geminiService.isConfigured = () => false;
            openrouterService.isConfigured = () => true;

            const origGemini = geminiService.analyzeComplaint;
            geminiService.analyzeComplaint = async () => {
                geminiAttempted = true;
                return { success: false };
            };

            openrouterService.analyzeComplaint = async () => ({
                success: true,
                analysis: {
                    category: "cleaning",
                    priority: "medium",
                    summary: "Washroom drain blocked with debris"
                },
                provider: "openrouter"
            });

            const result = await aiProviderManager.analyzeComplaint("Drain blocked", "Water not draining out", "cleaning");

            assert.strictEqual(result.success, true);
            assert.strictEqual(result.provider, "openrouter");
            assert.strictEqual(geminiAttempted, false, "Gemini must not be attempted when unconfigured");
        } finally {
            geminiService.isConfigured = origGeminiConfigured;
            openrouterService.isConfigured = origORConfigured;
            openrouterService.analyzeComplaint = origOpenRouter;
        }
    });

    // SCENARIO 8: Invalid API key / unknown model handling
    await test("Scenario 8: Invalid API key or model error enters bounded cooldown and triggers failover", async () => {
        const origGemini = geminiService.analyzeComplaint;
        const origOpenRouter = openrouterService.analyzeComplaint;
        const origGeminiConfigured = geminiService.isConfigured;
        const origORConfigured = openrouterService.isConfigured;

        try {
            geminiService.isConfigured = () => true;
            openrouterService.isConfigured = () => true;

            geminiService.analyzeComplaint = async () => ({
                success: false,
                error: "API key not valid. Please pass a valid API key.",
                errorCategory: "auth_error",
                status: 400
            });

            openrouterService.analyzeComplaint = async () => ({
                success: true,
                analysis: {
                    category: "other",
                    priority: "low",
                    summary: "Notice board glass cracked"
                },
                provider: "openrouter"
            });

            const result = await aiProviderManager.analyzeComplaint("Board broken", "Notice board glass cracked", "other");

            assert.strictEqual(result.success, true);
            assert.strictEqual(result.provider, "openrouter");
            assert.strictEqual(result.failedOver, true);
            assert.strictEqual(aiProviderManager.isCoolingDown("gemini"), true);
        } finally {
            geminiService.analyzeComplaint = origGemini;
            openrouterService.analyzeComplaint = origOpenRouter;
            geminiService.isConfigured = origGeminiConfigured;
            openrouterService.isConfigured = origORConfigured;
        }
    });

    // SCENARIO 9: AI output strictly adheres to schema
    await test("Scenario 9: Strict schema enforcement validates allowed categories, priorities, and summary limits", () => {
        // Valid case
        const valid = aiValidation.validateAiOutput({
            category: "PLUMBING",
            priority: "HIGH",
            summary: "Pipe leakage under sink"
        });
        assert.strictEqual(valid.valid, true);
        assert.strictEqual(valid.data.category, "plumbing");
        assert.strictEqual(valid.data.priority, "high");

        // Invalid category
        const invalidCat = aiValidation.validateAiOutput({
            category: "food_delivery",
            priority: "high",
            summary: "Late food"
        });
        assert.strictEqual(invalidCat.valid, false);
        assert(invalidCat.error.includes("Invalid category"));

        // Invalid priority
        const invalidPri = aiValidation.validateAiOutput({
            category: "plumbing",
            priority: "super_urgent",
            summary: "Pipe leak"
        });
        assert.strictEqual(invalidPri.valid, false);
        assert(invalidPri.error.includes("Invalid priority"));

        // Missing summary
        const missingSum = aiValidation.validateAiOutput({
            category: "plumbing",
            priority: "high",
            summary: "   "
        });
        assert.strictEqual(missingSum.valid, false);

        // Summary exceeding 500 chars
        const longSum = aiValidation.validateAiOutput({
            category: "plumbing",
            priority: "high",
            summary: "a".repeat(501)
        });
        assert.strictEqual(longSum.valid, false);
        assert(longSum.error.includes("exceeds 500 character limit"));
    });

    // SCENARIO 10: Multimodal photo analysis
    await test("Scenario 10: Multimodal image URL / base64 payload is accepted and processed", async () => {
        const origOpenRouter = openrouterService.analyzeComplaint;
        const origORConfigured = openrouterService.isConfigured;
        const origGeminiConfigured = geminiService.isConfigured;

        try {
            geminiService.isConfigured = () => false;
            openrouterService.isConfigured = () => true;

            let capturedImage = null;
            openrouterService.analyzeComplaint = async (title, desc, cat, img) => {
                capturedImage = img;
                return {
                    success: true,
                    analysis: {
                        category: "plumbing",
                        priority: "high",
                        summary: "Verified pipe fracture from photo"
                    },
                    provider: "openrouter"
                };
            };

            const samplePhoto = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/";
            const result = await aiProviderManager.analyzeComplaint("Broken pipe", "See photo", "plumbing", samplePhoto);

            assert.strictEqual(result.success, true);
            assert.strictEqual(capturedImage, samplePhoto, "Photo URL must be forwarded to the provider");
        } finally {
            geminiService.isConfigured = origGeminiConfigured;
            openrouterService.isConfigured = origORConfigured;
            openrouterService.analyzeComplaint = origOpenRouter;
        }
    });

    // SCENARIO 11: Cooldown recovery
    await test("Scenario 11: Cooldown expiry enables provider probing, and success resets cooldown", async () => {
        const origGemini = geminiService.analyzeComplaint;
        const origGeminiConfigured = geminiService.isConfigured;
        const origORConfigured = openrouterService.isConfigured;

        try {
            geminiService.isConfigured = () => true;
            openrouterService.isConfigured = () => true;

            // Force gemini into cooldown of 50ms
            aiProviderManager.forceCooldown("gemini", 50, "Test transient failure");
            assert.strictEqual(aiProviderManager.isCoolingDown("gemini"), true);

            // Wait 60ms for cooldown to expire
            await new Promise(resolve => setTimeout(resolve, 60));
            assert.strictEqual(aiProviderManager.isCoolingDown("gemini"), false, "Cooldown must expire");

            // Successful recovery
            geminiService.analyzeComplaint = async () => ({
                success: true,
                analysis: {
                    category: "electrical",
                    priority: "low",
                    summary: "Switch socket loose"
                },
                provider: "gemini"
            });

            const result = await aiProviderManager.analyzeComplaint("Socket loose", "Switch loose", "electrical");
            assert.strictEqual(result.success, true);
            assert.strictEqual(result.provider, "gemini");

            const status = aiProviderManager.getProviderStatus();
            assert.strictEqual(status.gemini.consecutiveFailures, 0, "Consecutive failures must reset to 0 upon success");
        } finally {
            geminiService.analyzeComplaint = origGemini;
            geminiService.isConfigured = origGeminiConfigured;
            openrouterService.isConfigured = origORConfigured;
        }
    });

    // SCENARIO 12: Complaint independence & ticketId preservation
    await test("Scenario 12: Complaint creation independent of AI, and AI retry reuses complaintId without duplication", async () => {
        const origFindById = Complaint.findById;
        const origAI = aiProviderManager.analyzeComplaint;

        const origFindOneAndUpdate = Complaint.findOneAndUpdate;

        try {
            let saveCount = 0;
            let providerCallCount = 0;
            const mockComplaint = {
                _id: new mongoose.Types.ObjectId(),
                ticketId: "HC-987654",
                title: "Water heater not functioning",
                description: "Geyser not heating water in washroom",
                category: "electrical",
                priority: "medium",
                status: "submitted",
                student: new mongoose.Types.ObjectId("507f1f77bcf86cd799439011"),
                hostelBlock: "B",
                roomNumber: "302",
                aiSummary: null,
                aiStatus: "not_started",
                save: async function() {
                    saveCount++;
                    return this;
                }
            };

            Complaint.findById = async (id) => {
                if (id && id.toString() === mockComplaint._id.toString()) return mockComplaint;
                return null;
            };

            Complaint.findOneAndUpdate = async (query, update) => {
                if (query._id && query._id.toString() === mockComplaint._id.toString()) {
                    if (update.$set) {
                        Object.assign(mockComplaint, update.$set);
                    }
                    return mockComplaint;
                }
                return null;
            };

            aiProviderManager.analyzeComplaint = async () => {
                providerCallCount++;
                return {
                    success: true,
                    analysis: {
                        category: "electrical",
                        priority: "high",
                        summary: "Geyser heating coil burnt out"
                    },
                    provider: "openrouter",
                    failedOver: true
                };
            };

            // Mock route call
            const aiRouter = require("../routes/ai");
            const routeLayer = aiRouter.stack.find(s => s.route && s.route.path === "/analyze-complaint" && s.route.methods.post);
            const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

            const mockReq = {
                user: { userId: "507f1f77bcf86cd799439011", role: "student" },
                body: { complaintId: mockComplaint._id.toString() }
            };

            let responseData = null;
            let statusCode = 0;
            const mockRes = {
                status: (code) => { statusCode = code; return { json: (d) => { responseData = d; } }; },
                json: (d) => { responseData = d; }
            };

            // First AI analysis execution
            await handler(mockReq, mockRes);

            assert.strictEqual(statusCode, 200);
            assert.strictEqual(responseData.complaint._id.toString(), mockComplaint._id.toString(), "Complaint ID must be preserved");
            assert.strictEqual(responseData.complaint.ticketId, "HC-987654", "Ticket ID must remain intact");
            assert.strictEqual(responseData.complaint.status, "submitted", "Status must remain submitted without altering workflow");
            assert.strictEqual(responseData.complaint.aiSummary, "Geyser heating coil burnt out");
            assert.strictEqual(responseData.complaint.aiStatus, "completed");
            assert.strictEqual(responseData.complaint.priority, "high");
            assert.strictEqual(providerCallCount, 1, "Provider should be called on first execution");
            assert.strictEqual(saveCount, 1);

            // Reopening complaint (second run without forceRetry) -> MUST return cached result without provider call!
            await handler(mockReq, mockRes);
            assert.strictEqual(statusCode, 200);
            assert.strictEqual(responseData.cached, true, "Reopening complaint must return cached assessment");
            assert.strictEqual(providerCallCount, 1, "AI provider must NOT be called on reopen");
            assert.strictEqual(saveCount, 1, "Save count must not increase on cached read");

            // Explicit retry with forceRetry = true -> re-invokes provider and updates complaint
            mockReq.body.forceRetry = true;
            await handler(mockReq, mockRes);
            assert.strictEqual(statusCode, 200);
            assert.strictEqual(providerCallCount, 2, "forceRetry must re-invoke provider");
            assert.strictEqual(saveCount, 2, "Second run updates the existing complaint without creating new complaints");
            assert.strictEqual(responseData.complaint._id.toString(), mockComplaint._id.toString());
        } finally {
            Complaint.findById = origFindById;
            Complaint.findOneAndUpdate = origFindOneAndUpdate;
            aiProviderManager.analyzeComplaint = origAI;
        }
    });

    // SCENARIO 13: Provider status diagnostic endpoint
    await test("Scenario 13: Provider status diagnostic endpoint reports healthy metrics and cooldown tracking", () => {
        aiProviderManager.resetCooldowns();
        const status = aiProviderManager.getProviderStatus();

        assert(status.gemini, "Status must include Gemini");
        assert(status.openrouter, "Status must include OpenRouter");
        assert.strictEqual(typeof status.gemini.configured, "boolean");
        assert.strictEqual(typeof status.openrouter.configured, "boolean");
        assert.strictEqual(status.gemini.coolingDown, false);
        assert.strictEqual(status.openrouter.coolingDown, false);
        assert.strictEqual(typeof status.gemini.totalRequests, "number");
    });

    // SCENARIO 14: Concurrent analysis requests are safely locked
    await test("Scenario 14: Concurrent analysis request returns inProgress without duplicate provider execution", async () => {
        const origFindById = Complaint.findById;
        const origFindOneAndUpdate = Complaint.findOneAndUpdate;
        const origAI = aiProviderManager.analyzeComplaint;

        try {
            let providerCalls = 0;
            const inProgressComplaint = {
                _id: new mongoose.Types.ObjectId(),
                ticketId: "HC-112233",
                title: "Water pump noise",
                description: "Continuous vibrating noise",
                category: "plumbing",
                priority: "medium",
                status: "submitted",
                student: new mongoose.Types.ObjectId("507f1f77bcf86cd799439011"),
                hostelBlock: "A",
                roomNumber: "101",
                aiSummary: null,
                aiStatus: "processing",
                updatedAt: new Date()
            };

            Complaint.findById = async () => inProgressComplaint;
            aiProviderManager.analyzeComplaint = async () => {
                providerCalls++;
                return { success: true, analysis: { category: "plumbing", priority: "medium", summary: "Pump noise" } };
            };

            const aiRouter = require("../routes/ai");
            const routeLayer = aiRouter.stack.find(s => s.route && s.route.path === "/analyze-complaint" && s.route.methods.post);
            const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

            const mockReq = {
                user: { userId: "507f1f77bcf86cd799439011", role: "student" },
                body: { complaintId: inProgressComplaint._id.toString() }
            };

            let responseData = null;
            let statusCode = 0;
            const mockRes = {
                status: (code) => { statusCode = code; return { json: (d) => { responseData = d; } }; },
                json: (d) => { responseData = d; }
            };

            await handler(mockReq, mockRes);

            assert.strictEqual(statusCode, 200);
            assert.strictEqual(responseData.inProgress, true, "Must report inProgress when concurrent request is active");
            assert.strictEqual(providerCalls, 0, "No duplicate provider call should be made");
        } finally {
            Complaint.findById = origFindById;
            Complaint.findOneAndUpdate = origFindOneAndUpdate;
            aiProviderManager.analyzeComplaint = origAI;
        }
    });

    // SCENARIO 15: Backward-compatibility for documents lacking aiStatus
    await test("Scenario 15: Legacy complaint with aiSummary but missing aiStatus is returned from cache", async () => {
        const origFindById = Complaint.findById;
        const origAI = aiProviderManager.analyzeComplaint;

        try {
            let providerCalls = 0;
            const legacyComplaint = {
                _id: new mongoose.Types.ObjectId(),
                ticketId: "HC-777666",
                title: "Broken wardrobe hinge",
                description: "Hinge screws came loose",
                category: "furniture",
                priority: "low",
                status: "submitted",
                student: new mongoose.Types.ObjectId("507f1f77bcf86cd799439011"),
                hostelBlock: "C",
                roomNumber: "205",
                aiSummary: "Loose screws on wardrobe hinge.",
                // aiStatus deliberately undefined to test backward compatibility
            };

            Complaint.findById = async () => legacyComplaint;
            aiProviderManager.analyzeComplaint = async () => {
                providerCalls++;
                return { success: true };
            };

            const aiRouter = require("../routes/ai");
            const routeLayer = aiRouter.stack.find(s => s.route && s.route.path === "/analyze-complaint" && s.route.methods.post);
            const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

            const mockReq = {
                user: { userId: "507f1f77bcf86cd799439011", role: "student" },
                body: { complaintId: legacyComplaint._id.toString() }
            };

            let responseData = null;
            let statusCode = 0;
            const mockRes = {
                status: (code) => { statusCode = code; return { json: (d) => { responseData = d; } }; },
                json: (d) => { responseData = d; }
            };

            await handler(mockReq, mockRes);

            assert.strictEqual(statusCode, 200);
            assert.strictEqual(responseData.cached, true, "Legacy complaint must return cached result");
            assert.strictEqual(providerCalls, 0, "No provider call should be made for legacy completed complaint");
            assert.strictEqual(responseData.analysis.summary, "Loose screws on wardrobe hinge.");
        } finally {
            Complaint.findById = origFindById;
            aiProviderManager.analyzeComplaint = origAI;
        }
    });

    // SCENARIO 16: Provider failure preserves ticket and marks aiStatus = 'failed'
    await test("Scenario 16: Both AI providers failing marks aiStatus = 'failed' and preserves ticket and complaint status", async () => {
        const origFindById = Complaint.findById;
        const origFindOneAndUpdate = Complaint.findOneAndUpdate;
        const origAI = aiProviderManager.analyzeComplaint;

        try {
            let saved = false;
            const failingComplaint = {
                _id: new mongoose.Types.ObjectId(),
                ticketId: "HC-554433",
                title: "Corridor light flicker",
                description: "Light in hallway is blinking rapidly",
                category: "electrical",
                priority: "medium",
                status: "submitted",
                student: new mongoose.Types.ObjectId("507f1f77bcf86cd799439011"),
                hostelBlock: "A",
                roomNumber: "110",
                aiSummary: null,
                aiStatus: "not_started",
                save: async function() {
                    saved = true;
                    return this;
                }
            };

            Complaint.findById = async () => failingComplaint;
            Complaint.findOneAndUpdate = async (q, u) => {
                if (u.$set) Object.assign(failingComplaint, u.$set);
                return failingComplaint;
            };

            aiProviderManager.analyzeComplaint = async () => ({
                success: false,
                error: "All AI providers failed"
            });

            const aiRouter = require("../routes/ai");
            const routeLayer = aiRouter.stack.find(s => s.route && s.route.path === "/analyze-complaint" && s.route.methods.post);
            const handler = routeLayer.route.stack[routeLayer.route.stack.length - 1].handle;

            const mockReq = {
                user: { userId: "507f1f77bcf86cd799439011", role: "student" },
                body: { complaintId: failingComplaint._id.toString() }
            };

            let responseData = null;
            let statusCode = 0;
            const mockRes = {
                status: (code) => { statusCode = code; return { json: (d) => { responseData = d; } }; },
                json: (d) => { responseData = d; }
            };

            await handler(mockReq, mockRes);

            assert.strictEqual(statusCode, 503);
            assert.strictEqual(saved, true);
            assert.strictEqual(failingComplaint.aiStatus, "failed", "aiStatus must be set to 'failed'");
            assert.strictEqual(failingComplaint.status, "submitted", "Main ticket lifecycle status must remain intact");
            assert.strictEqual(failingComplaint.ticketId, "HC-554433", "Ticket ID must remain intact");
        } finally {
            Complaint.findById = origFindById;
            Complaint.findOneAndUpdate = origFindOneAndUpdate;
            aiProviderManager.analyzeComplaint = origAI;
        }
    });

    console.log("\n==================================================");
    console.log(`AI Failover Results: ${passed} PASSED, ${failed} FAILED`);
    console.log("==================================================\n");

    if (failed > 0) {
        process.exit(1);
    }
}

if (require.main === module) {
    runAiFailoverTests().catch(err => {
        console.error("Test Suite Uncaught Error:", err);
        process.exit(1);
    });
}

module.exports = runAiFailoverTests;
