const express = require('express');
const auth = require('../middleware/auth');
const geminiService = require('../services/geminiService');
const Complaint = require('../models/Complaint');

const router = express.Router();

router.post('/analyze-complaint', auth, async (req, res) => {
    try {
        const { complaintId, title, description, category, image } = req.body;

        if (complaintId) {
            // New flow: analyze an existing complaint
            const complaint = await Complaint.findById(complaintId);
            if (!complaint) {
                return res.status(404).json({ message: "Complaint not found" });
            }

            // Optional: verify ownership or admin role
            if (complaint.student.toString() !== req.user.userId && req.user.role !== 'admin' && req.user.role !== 'staff') {
                return res.status(403).json({ message: "Not authorized to analyze this complaint" });
            }

            const analysisResult = await geminiService.analyzeComplaint(complaint.title, complaint.description, complaint.category, complaint.photoUrl);

            if (!analysisResult.success) {
                return res.status(503).json({ message: "AI analysis is currently unavailable. Please try again." });
            }

            const allowedPriorities = ["low", "medium", "high", "emergency"];
            const allowedCategories = ["plumbing", "electrical", "internet", "cleaning", "furniture", "other"];
            
            const aiCategory = analysisResult.analysis.category.toLowerCase();
            const aiPriority = analysisResult.analysis.priority.toLowerCase();

            if (!allowedCategories.includes(aiCategory) || !allowedPriorities.includes(aiPriority) || !analysisResult.analysis.summary) {
                return res.status(502).json({ message: "Invalid AI response structure" });
            }

            complaint.category = aiCategory;
            complaint.priority = aiPriority;
            complaint.aiSummary = analysisResult.analysis.summary;
            await complaint.save();

            return res.status(200).json({
                message: "Complaint analyzed successfully",
                analysis: {
                    category: aiCategory,
                    priority: aiPriority,
                    summary: analysisResult.analysis.summary
                },
                complaint
            });
        }

        // Backward compatibility (old flow)
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

        const analysisResult = await geminiService.analyzeComplaint(title, description, category, image);

        if (!analysisResult.success) {
            return res.status(503).json({ message: "AI analysis is currently unavailable. Please try again." });
        }

        const allowedPriorities = ["low", "medium", "high", "emergency"];
        const aiCategory = analysisResult.analysis.category.toLowerCase();
        const aiPriority = analysisResult.analysis.priority.toLowerCase();

        if (!["plumbing", "electrical", "internet", "cleaning", "furniture", "other"].includes(aiCategory) ||
            !allowedPriorities.includes(aiPriority) ||
            !analysisResult.analysis.summary) {
            return res.status(502).json({ message: "Invalid AI response structure" });
        }

        res.status(200).json({
            message: "Complaint analyzed successfully",
            analysis: {
                category: aiCategory,
                priority: aiPriority,
                summary: analysisResult.analysis.summary
            }
        });

    } catch (error) {
        console.error("AI Analysis Route Error:", error);
        res.status(500).json({ message: "Server error during analysis" });
    }
});

module.exports = router;