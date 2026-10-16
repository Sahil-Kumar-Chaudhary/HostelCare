const express = require('express');
const auth = require('../middleware/auth');
const aiProviderManager = require('../services/aiProviderManager');
const Complaint = require('../models/Complaint');

const router = express.Router();

// Diagnostic endpoint to inspect AI providers health and cooldowns
router.get('/status', auth, (req, res) => {
    try {
        const status = aiProviderManager.getProviderStatus();
        res.status(200).json({
            success: true,
            providers: status
        });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
});

router.post('/analyze-complaint', auth, async (req, res) => {
    try {
        const { complaintId, title, description, category, image } = req.body;

        if (complaintId) {
            // Flow: analyze an existing complaint by complaintId
            const complaint = await Complaint.findById(complaintId);
            if (!complaint) {
                return res.status(404).json({ message: "Complaint not found" });
            }

            // Verify ownership or staff/admin/warden role
            if (complaint.student.toString() !== req.user.userId && req.user.role !== 'admin' && req.user.role !== 'staff' && req.user.role !== 'warden') {
                return res.status(403).json({ message: "Not authorized to analyze this complaint" });
            }

            // Phase B3: Return stored assessment immediately if already completed and valid
            const hasExistingSummary = Boolean(complaint.aiSummary && complaint.aiSummary.trim().length > 0);
            const isCompleted = complaint.aiStatus === 'completed' || (hasExistingSummary && complaint.aiStatus !== 'failed');
            if (isCompleted && !req.body.forceRetry) {
                if (complaint.aiStatus !== 'completed' && typeof complaint.save === 'function') {
                    complaint.aiStatus = 'completed';
                    await complaint.save();
                }
                return res.status(200).json({
                    message: "Complaint analyzed successfully",
                    cached: true,
                    analysis: {
                        category: complaint.category,
                        priority: complaint.priority,
                        summary: complaint.aiSummary
                    },
                    provider: complaint.aiProvider || "persisted",
                    failedOver: false,
                    complaint
                });
            }

            // Phase B3: Avoid duplicate simultaneous provider calls if already in progress
            const isProcessingRecent = complaint.aiStatus === 'processing' && complaint.updatedAt && (Date.now() - new Date(complaint.updatedAt).getTime() < 30000);
            if (isProcessingRecent && !req.body.forceRetry) {
                return res.status(200).json({
                    message: "AI analysis is currently in progress",
                    inProgress: true,
                    complaint
                });
            }

            // Atomic claim to prevent concurrent duplicate processing
            const claimQuery = { _id: complaintId };
            if (!req.body.forceRetry) {
                claimQuery.$or = [
                    { aiStatus: { $in: ['not_started', 'failed', null] } },
                    { aiSummary: { $in: ['', null] } },
                    { updatedAt: { $lt: new Date(Date.now() - 30000) } }
                ];
            }

            const claimedComplaint = await Complaint.findOneAndUpdate(
                claimQuery,
                {
                    $set: {
                        aiStatus: 'processing',
                        aiError: ''
                    }
                },
                { returnDocument: 'after' }
            );

            if (!claimedComplaint) {
                return res.status(200).json({
                    message: "AI analysis is currently in progress",
                    inProgress: true,
                    complaint
                });
            }

            const analysisResult = await aiProviderManager.analyzeComplaint(
                claimedComplaint.title,
                claimedComplaint.description,
                claimedComplaint.category,
                claimedComplaint.photoUrl
            );

            if (!analysisResult.success) {
                claimedComplaint.aiStatus = 'failed';
                claimedComplaint.aiError = analysisResult.error || "AI analysis failed";
                await claimedComplaint.save();

                return res.status(503).json({
                    message: "AI analysis is currently unavailable. Please try again.",
                    aiStatus: "failed",
                    error: claimedComplaint.aiError,
                    complaint: claimedComplaint
                });
            }

            claimedComplaint.category = analysisResult.analysis.category;
            claimedComplaint.priority = analysisResult.analysis.priority;
            claimedComplaint.aiSummary = analysisResult.analysis.summary;
            claimedComplaint.aiStatus = 'completed';
            claimedComplaint.aiProvider = analysisResult.provider || '';
            claimedComplaint.aiAssessedAt = new Date();
            claimedComplaint.aiError = '';
            await claimedComplaint.save();

            return res.status(200).json({
                message: "Complaint analyzed successfully",
                analysis: analysisResult.analysis,
                provider: analysisResult.provider,
                failedOver: analysisResult.failedOver || false,
                complaint: claimedComplaint
            });
        }

        // Backward compatibility (draft flow without prior complaint creation)
        if (!title || typeof title !== 'string' || title.trim().length === 0) {
            return res.status(400).json({ message: "Valid title is required" });
        }
        if (title.length > 150) {
            return res.status(400).json({ message: "Title is too long" });
        }
        if (!description || typeof description !== 'string' || description.trim().length === 0) {
            return res.status(400).json({ message: "Valid description is required" });
        }
        if (description.length > 1000) {
            return res.status(400).json({ message: "Description is too long" });
        }

        const analysisResult = await aiProviderManager.analyzeComplaint(
            title,
            description,
            category,
            image
        );

        if (!analysisResult.success) {
            return res.status(503).json({ message: "AI analysis is currently unavailable. Please try again." });
        }

        res.status(200).json({
            message: "Complaint analyzed successfully",
            analysis: analysisResult.analysis,
            provider: analysisResult.provider,
            failedOver: analysisResult.failedOver || false
        });

    } catch (error) {
        console.error("AI Analysis Route Error:", error);
        res.status(500).json({ message: "Server error during analysis" });
    }
});

module.exports = router;