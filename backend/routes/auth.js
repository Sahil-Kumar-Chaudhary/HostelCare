const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const User = require("../models/User");

const router = express.Router();

router.post("/register", async (req, res) => {
  try {
    const {
      name,
      studentId,
      email,
      password,
      hostelBlock,
      roomNumber,
      phone
    } = req.body;

    if (
      !name ||
      !studentId ||
      !email ||
      !password ||
      !hostelBlock ||
      !roomNumber
    ) {
      return res.status(400).json({
        message: "Please fill all required fields"
      });
    }

    const cleanEmail = email.trim().toLowerCase();
    const cleanStudentId = studentId.trim();

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
      name: name.trim(),
      studentId: cleanStudentId,
      email: cleanEmail,
      password: hashedPassword,
      hostelBlock: hostelBlock.trim(),
      roomNumber: roomNumber.trim(),
      phone: phone ? phone.trim() : ""
    });

    res.status(201).json({
      message: "Account created successfully",
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
        hostelBlock: user.hostelBlock,
        roomNumber: user.roomNumber,
        phone: user.phone,
        profilePhoto: user.profilePhoto,
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