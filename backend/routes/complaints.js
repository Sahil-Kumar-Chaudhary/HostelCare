const express = require("express");
const Complaint = require("../models/Complaint");
const auth = require("../middleware/auth");

const router = express.Router();

router.post("/", auth, async (req, res) => {
  try {
    const {
      title,
      description,
      category,
      priority,
      hostelBlock,
      roomNumber,
      photoUrl,
      aiSummary
    } = req.body;

    if (
      !title ||
      !description ||
      !category ||
      !hostelBlock ||
      !roomNumber
    ) {
      return res.status(400).json({
        message: "Please fill all required fields"
      });
    }

    const complaint = await Complaint.create({
      student: req.user.userId,
      title: title.trim(),
      description: description.trim(),
      category: category.toLowerCase(),
      priority: priority
        ? priority.toLowerCase()
        : "medium",
      hostelBlock: hostelBlock.trim(),
      roomNumber: roomNumber.trim(),
      photoUrl: photoUrl || "",
      aiSummary: aiSummary || "",
      status: "submitted",
      timeline: [
        {
          status: "submitted",
          note: "Complaint submitted by student",
          updatedBy: req.user.userId
        }
      ]
    });

    res.status(201).json({
      message: "Complaint created successfully",
      complaint
    });
  } catch (error) {
    console.error("Create complaint error:", error.message);

    res.status(500).json({
      message: "Server error"
    });
  }
});

router.get("/", auth, async (req, res) => {
  try {
    const complaints = await Complaint.find({ student: req.user.userId }).sort({ createdAt: -1 });
    res.json({ complaints });
  } catch (error) {
    console.error("Get complaints error:", error.message);
    res.status(500).json({ message: "Server error" });
  }
});

router.get("/:id", auth, async (req, res) => {
  try {
    const complaint = await Complaint.findById(req.params.id);
    if (!complaint) {
      return res.status(404).json({ message: "Complaint not found" });
    }
    
    // Ensure the student can only access their own complaint
    if (complaint.student.toString() !== req.user.userId) {
      return res.status(403).json({ message: "Not authorized to view this complaint" });
    }

    res.json({ complaint });
  } catch (error) {
    console.error("Get complaint by ID error:", error.message);
    if (error.kind === 'ObjectId') {
      return res.status(404).json({ message: "Complaint not found" });
    }
    res.status(500).json({ message: "Server error" });
  }
});

module.exports = router;