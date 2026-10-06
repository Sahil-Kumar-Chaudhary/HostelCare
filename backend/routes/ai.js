const express = require('express');
const auth = require('../middleware/auth');
const geminiService = require('../services/geminiService');

const router = express.Router();

router.post('/analyze-complaint', auth, async (req, res) => {
    try {
        const { title, description, category, image } = req.body;

        if (!title || typeof title !== 'string' || title.trim().length === 0) {
            return res.status(400).json({ message: "Valid title is required" });
        }
        if (title.length > 150) {
            return res.status(400).json({ message: "Title is too long (max 150 characters)" });
        }

        if (!description || typeof description !== 'string' || description.trim().length === 0) {
            return res.status(400).json({ message: "Valid description is required" });
        }
        if (description.length > 1000) {
            return res.status(400).json({ message: "Description is too long (max 1000 characters)" });
        }
        
        if (category) {
            const allowedCategories = ["plumbing", "electrical", "internet", "cleaning", "furniture", "other"];
            if (!allowedCategories.includes(category.toLowerCase())) {
                return res.status(400).json({ message: "Invalid category" });
            }
        }

        const analysisResult = await geminiService.analyzeComplaint(title, description, category, image);

        if (!analysisResult.success) {
            return res.status(503).json({ message: "AI analysis is currently unavailable. Please try again." });
        }

        const allowedPriorities = ["low", "medium", "high", "emergency"];
        const aiCategory = analysisResult.analysis.category.toLowerCase();
        const aiPriority = analysisResult.analysis.priority.toLowerCase();

        // Safe fallback validation
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