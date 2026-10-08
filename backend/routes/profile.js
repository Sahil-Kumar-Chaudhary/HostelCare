const express = require("express");
const User = require("../models/User");
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


router.get("/", auth, async (req, res) => {
  try {
    const user = await User.findById(req.user.userId).select("-password");

    if (!user) {
      return res.status(404).json({
        message: "User not found"
      });
    }

    res.json({
      user: {
        id: user._id,
        name: user.name,
        studentId: user.studentId,
        email: user.email,
        hostelBlock: user.hostelBlock,
        roomNumber: user.roomNumber,
        phone: user.phone,
        profilePhoto: user.profilePhoto,
        role: user.role
      }
    });
  } catch (error) {
    console.error("Profile error:", error.message);

    res.status(500).json({
      message: "Server error"
    });
  }
});

router.put("/", auth, async (req, res) => {
  try {
    const { name, email, hostelBlock, roomNumber, phone } = req.body;

    if (!name || !email || !hostelBlock || !roomNumber) {
      return res.status(400).json({
        message: "Please provide all required fields (name, email, hostelBlock, roomNumber)"
      });
    }

    const trimmedEmail = email.trim().toLowerCase();

    const existingUser = await User.findOne({ email: trimmedEmail });
    if (existingUser && existingUser._id.toString() !== req.user.userId) {
      return res.status(409).json({
        message: "Email is already in use by another user"
      });
    }

    const user = await User.findById(req.user.userId);
    if (!user) {
      return res.status(404).json({
        message: "User not found"
      });
    }

    user.name = name.trim();
    user.email = trimmedEmail;
    user.hostelBlock = hostelBlock.trim();
    user.roomNumber = roomNumber.trim();
    if (phone !== undefined) {
      user.phone = phone.trim();
    }

    await user.save();

    res.status(200).json({
      message: "Profile updated successfully",
      user: {
        id: user._id,
        name: user.name,
        studentId: user.studentId,
        email: user.email,
        hostelBlock: user.hostelBlock,
        roomNumber: user.roomNumber,
        phone: user.phone,
        profilePhoto: user.profilePhoto,
        role: user.role
      }
    });

  } catch (error) {
    console.error("Update profile error:", error.message);
    res.status(500).json({
      message: "Server error"
    });
  }
});


router.post("/photo", auth, (req, res, next) => {
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
    if (!req.file) {
      return res.status(400).json({ message: "No photo provided" });
    }

    const user = await User.findById(req.user.userId);
    if (!user) {
      return res.status(404).json({ message: "User not found" });
    }

    const base64Image = req.file.buffer.toString('base64');
    user.profilePhoto = `data:${req.file.mimetype};base64,${base64Image}`;
    await user.save();

    res.status(200).json({
      message: "Profile photo updated successfully",
      photoUrl: user.profilePhoto
    });
  } catch (error) {
    console.error("Profile photo upload error:", error);
    res.status(500).json({ message: "Server error during photo upload" });
  }
});

router.delete("/photo", auth, async (req, res) => {
  try {
    const user = await User.findById(req.user.userId);
    if (!user) {
      return res.status(404).json({ message: "User not found" });
    }

    user.profilePhoto = "";
    await user.save();

    res.status(200).json({
      message: "Profile photo removed successfully"
    });
  } catch (error) {
    console.error("Profile photo remove error:", error);
    res.status(500).json({ message: "Server error during photo removal" });
  }
});

module.exports = router;