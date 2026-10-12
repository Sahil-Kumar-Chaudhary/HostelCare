const express = require("express");
const Complaint = require("../models/Complaint");
const User = require("../models/User");
const auth = require("../middleware/auth");

const multer = require("multer");
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 }, // 5 MB
  fileFilter: (req, file, cb) => {
    if (["image/jpeg", "image/png", "image/webp"].includes(file.mimetype)) {
      cb(null, true);
    } else {
      cb(new Error("Only JPEG, PNG, and WebP images are allowed."));
    }
  }
});

const router = express.Router();

router.post(
  "/",
  auth,
  auth.requireStudent,
  (req, res, next) => {
    upload.single("photo")(req, res, (err) => {
      if (err) {
        if (err.code === "LIMIT_FILE_SIZE") {
          return res.status(400).json({ message: "Image must be 5 MB or smaller." });
        }
        return res.status(400).json({ message: err.message });
      }
      next();
    });
  },
  async (req, res) => {
    try {
      // 1. Verify student session and retrieve trusted student profile
      const student = await User.findById(req.user.userId);
      if (!student) {
        return res.status(404).json({ message: "Student account not found" });
      }
      if (student.role !== "student") {
        return res.status(403).json({ message: "Only registered students can submit complaints" });
      }

      const {
        title,
        description,
        category,
        priority,
        hostelBlock,
        roomNumber,
        aiSummary
      } = req.body;

      let photoUrl = req.body.photoUrl;
      if (req.file) {
        const base64Image = req.file.buffer.toString("base64");
        photoUrl = `data:${req.file.mimetype};base64,${base64Image}`;
      }

      // 2. Validate required core fields
      if (!title || !title.trim()) {
        return res.status(400).json({ message: "Complaint title is required" });
      }
      if (!description || !description.trim()) {
        return res.status(400).json({ message: "Complaint description is required" });
      }
      if (!category || !category.trim()) {
        return res.status(400).json({ message: "Complaint category is required" });
      }

      // 3. Resolve location: use request or fall back to student's trusted profile
      const resolvedHostelName =
        (student.hostelName && student.hostelName.trim()) ||
        (req.body.hostelName && req.body.hostelName.trim()) ||
        "";
      const resolvedBlock =
        (hostelBlock && hostelBlock.trim()) ||
        (student.hostelBlock && student.hostelBlock.trim()) ||
        "";
      const resolvedRoom =
        (roomNumber && roomNumber.trim()) ||
        (student.roomNumber && student.roomNumber.trim()) ||
        "";

      if (!resolvedBlock) {
        return res.status(400).json({ message: "Hostel block is required" });
      }
      if (!resolvedRoom) {
        return res.status(400).json({ message: "Room number is required" });
      }

      const validCategories = ["plumbing", "electrical", "internet", "cleaning", "furniture", "other"];
      const normCategory = category.trim().toLowerCase();
      const finalCategory = validCategories.includes(normCategory) ? normCategory : "other";

      const validPriorities = ["low", "medium", "high", "emergency"];
      const normPriority = priority ? priority.trim().toLowerCase() : "medium";
      const finalPriority = validPriorities.includes(normPriority) ? normPriority : "medium";

      const complaint = await Complaint.create({
        student: student._id,
        title: title.trim(),
        description: description.trim(),
        category: finalCategory,
        priority: finalPriority,
        hostelName: resolvedHostelName,
        hostelBlock: resolvedBlock,
        roomNumber: resolvedRoom,
        photoUrl: photoUrl || "",
        aiSummary: (aiSummary && aiSummary.trim()) || "",
        status: "submitted",
        timeline: [
          {
            status: "submitted",
            note: "Complaint submitted by student",
            updatedBy: student._id
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
  }
);

router.get("/", auth, auth.requireStudent, async (req, res) => {
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
    if (error.kind === "ObjectId") {
      return res.status(404).json({ message: "Complaint not found" });
    }
    res.status(500).json({ message: "Server error" });
  }
});

module.exports = router;