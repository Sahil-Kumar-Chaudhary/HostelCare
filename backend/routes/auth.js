const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const User = require("../models/User");

const router = express.Router();

function validatePassword(pwd) {
  if (!pwd || typeof pwd !== "string") return false;
  if (pwd.length < 8 || pwd.length > 128) return false;
  if (!/[A-Z]/.test(pwd)) return false;
  if (!/[a-z]/.test(pwd)) return false;
  if (!/[0-9]/.test(pwd)) return false;
  if (!/[!@#$%^&*(),.?":{}|<>_\-+=\[\]\\\/~`]/.test(pwd)) return false;
  return true;
}

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const phoneRegex = /^\+?[0-9\s\-()]{7,20}$/;

router.post("/register", async (req, res) => {
  try {
    const {
      name,
      studentId,
      email,
      phone,
      collegeName,
      course,
      currentYear,
      admissionYear,
      semester,
      hostelName,
      hostelBlock,
      roomNumber,
      password,
      confirmPassword,
      acceptedTerms
    } = req.body;

    const yearValue = admissionYear || currentYear;

    if (
      !name ||
      !studentId ||
      !email ||
      !phone ||
      !collegeName ||
      !course ||
      !yearValue ||
      !hostelName ||
      !hostelBlock ||
      !roomNumber ||
      !password ||
      confirmPassword === undefined
    ) {
      return res.status(400).json({
        message: "Please fill all required fields"
      });
    }

    if (acceptedTerms !== true && acceptedTerms !== "true") {
      return res.status(400).json({
        message: "You must accept the Terms of Service and Code of Conduct"
      });
    }

    const cleanName = name.trim();
    const cleanStudentId = studentId.trim();
    const cleanEmail = email.trim().toLowerCase();
    const cleanPhone = phone.trim();
    const cleanCollegeName = collegeName.trim();
    const cleanCourse = course.trim();
    const cleanCurrentYear = yearValue.trim();
    const cleanSemester = semester ? semester.trim() : "";
    const cleanHostelName = hostelName.trim();
    const cleanHostelBlock = hostelBlock.trim();
    const cleanRoomNumber = roomNumber.trim();

    if (
      !cleanName ||
      !cleanStudentId ||
      !cleanEmail ||
      !cleanPhone ||
      !cleanCollegeName ||
      !cleanCourse ||
      !cleanCurrentYear ||
      !cleanHostelName ||
      !cleanHostelBlock ||
      !cleanRoomNumber
    ) {
      return res.status(400).json({
        message: "Fields cannot be empty or blank"
      });
    }

    if (!emailRegex.test(cleanEmail)) {
      return res.status(400).json({
        message: "Please enter a valid email address"
      });
    }

    if (!cleanEmail.endsWith("@gmail.com")) {
      return res.status(400).json({
        message: "Only @gmail.com email addresses are allowed"
      });
    }

    if (!phoneRegex.test(cleanPhone)) {
      return res.status(400).json({
        message: "Please enter a valid phone number"
      });
    }

    if (!/^[a-zA-Z0-9\-_]+$/.test(cleanStudentId)) {
      return res.status(400).json({
        message: "Student ID must be alphanumeric"
      });
    }

    if (password !== confirmPassword) {
      return res.status(400).json({
        message: "Passwords do not match"
      });
    }

    if (!validatePassword(password)) {
      return res.status(400).json({
        message: "Password must be at least 8 characters, and include uppercase, lowercase, number, and special character"
      });
    }

    const existingUser = await User.findOne({
      $or: [
        { email: cleanEmail },
        { studentId: cleanStudentId }
      ]
    });

    if (existingUser) {
      return res.status(409).json({
        message: "Email or Student ID already exists"
      });
    }

    const hashedPassword = await bcrypt.hash(password, 10);

    const user = await User.create({
      name: cleanName,
      studentId: cleanStudentId,
      email: cleanEmail,
      phone: cleanPhone,
      collegeName: cleanCollegeName,
      course: cleanCourse,
      currentYear: cleanCurrentYear,
      semester: cleanSemester,
      hostelName: cleanHostelName,
      hostelBlock: cleanHostelBlock,
      roomNumber: cleanRoomNumber,
      password: hashedPassword,
      acceptedTerms: true,
      acceptedTermsAt: new Date(),
      role: "student"
    });

    res.status(201).json({
      message: "Account created successfully",
      user: {
        id: user._id,
        name: user.name,
        studentId: user.studentId,
        email: user.email,
        phone: user.phone || "",
        collegeName: user.collegeName || "",
        course: user.course || "",
        currentYear: user.currentYear || "",
        semester: user.semester || "",
        hostelName: user.hostelName || "",
        hostelBlock: user.hostelBlock,
        roomNumber: user.roomNumber,
        profilePhoto: user.profilePhoto || "",
        role: user.role
      }
    });
  } catch (error) {
    console.error("Register error:", error.message);

    res.status(500).json({
      message: "Server error"
    });
  }
});

router.post("/login", async (req, res) => {
  try {
    const { email, studentId, password } = req.body;

    if ((!email && !studentId) || !password) {
      return res.status(400).json({
        message: "Please enter your email or Student ID and password"
      });
    }

    const query = email
      ? { email: email.trim().toLowerCase() }
      : { studentId: studentId.trim() };

    const user = await User.findOne(query);

    if (!user) {
      return res.status(401).json({
        message: "Invalid credentials"
      });
    }

    const passwordMatch = await bcrypt.compare(
      password,
      user.password
    );

    if (!passwordMatch) {
      return res.status(401).json({
        message: "Invalid credentials"
      });
    }

    if (!process.env.JWT_SECRET) {
      console.error("JWT_SECRET is missing");

      return res.status(500).json({
        message: "Server configuration error"
      });
    }

    const token = jwt.sign(
      {
        userId: user._id.toString(),
        role: user.role
      },
      process.env.JWT_SECRET,
      {
        expiresIn: "7d"
      }
    );

    res.status(200).json({
      message: "Login successful",
      token,
      user: {
        id: user._id,
        name: user.name,
        studentId: user.studentId,
        email: user.email,
        phone: user.phone || "",
        collegeName: user.collegeName || "",
        course: user.course || "",
        currentYear: user.currentYear || "",
        semester: user.semester || "",
        hostelName: user.hostelName || "",
        hostelBlock: user.hostelBlock,
        roomNumber: user.roomNumber,
        profilePhoto: user.profilePhoto || "",
        role: user.role
      }
    });
  } catch (error) {
    console.error("Login error:", error.message);

    res.status(500).json({
      message: "Server error"
    });
  }
});

module.exports = router;