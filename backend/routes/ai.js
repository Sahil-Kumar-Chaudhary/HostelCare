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

            // Verify ownership or staff/admin role
            if (complaint.student.toString() !== req.user.userId && req.user.role !== 'admin' && req.user.role !== 'staff' && req.user.role !== 'warden') {
                return res.status(403).json({ message: "Not authorized to analyze this complaint" });
            }

            const analysisResult = await aiProviderManager.analyzeComplaint(
                complaint.title,
                complaint.description,
                complaint.category,
                complaint.photoUrl
            );

            if (!analysisResult.success) {
                return res.status(503).json({ message: "AI analysis is currently unavailable. Please try again." });
            }

            complaint.category = analysisResult.analysis.category;
            complaint.priority = analysisResult.analysis.priority;
            complaint.aiSummary = analysisResult.analysis.summary;
            await complaint.save();

            return res.status(200).json({
                message: "Complaint analyzed successfully",
                analysis: analysisResult.analysis,
                provider: analysisResult.provider,
                failedOver: analysisResult.failedOver || false,
                complaint
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