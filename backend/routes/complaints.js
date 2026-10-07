const express = require("express");
const Complaint = require("../models/Complaint");
const auth = require("../middleware/auth");

const multer = require('multer');
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 }, // 5 MB
  fileFilter: (req, file, cb) => {
    if (['image/jpeg', 'image/png', 'image/webp'].includes(file.mimetype)) {
      cb(null, true);
    } else {
      cb(new Error('Only JPEG, PNG, and WebP images are allowed.'));
    }
  }
});


const router = express.Router();

router.post("/", auth, (req, res, next) => {
  upload.single('photo')(req, res, (err) => {
    if (err) {
      if (err.code === 'LIMIT_FILE_SIZE') {
        return res.status(400).json({ message: "Image must be 5 MB or smaller." });
      }
      return res.status(400).json({ message: err.message });
    }
    next();
  });
}, async (req, res) => {
  try {
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
        const base64Image = req.file.buffer.toString('base64');
        photoUrl = `data:${req.file.mimetype};base64,${base64Image}`;
      }

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