const mongoose = require("mongoose");

const userSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: true,
      trim: true
    },
    studentId: {
      type: String,
      required: function() {
        return this.role === "student";
      },
      sparse: true,
      trim: true
    },
    staffId: {
      type: String,
      sparse: true,
      trim: true
    },
    department: {
      type: String,
      default: "",
      trim: true
    },
    isActive: {
      type: Boolean,
      default: true
    },
    mustChangePassword: {
      type: Boolean,
      default: false
    },
    email: {
      type: String,
      required: true,
      unique: true,
      lowercase: true,
      trim: true
    },
    password: {
      type: String,
      required: true
    },
    collegeName: {
      type: String,
      default: "",
      trim: true
    },
    course: {
      type: String,
      default: "",
      trim: true
    },
    currentYear: {
      type: String,
      default: "",
      trim: true
    },
    semester: {
      type: String,
      default: "",
      trim: true
    },
    hostelName: {
      type: String,
      default: "",
      trim: true
    },
    hostelBlock: {
      type: String,
      required: function() {
        return this.role === "student";
      },
      default: "",
      trim: true
    },
    roomNumber: {
      type: String,
      required: function() {
        return this.role === "student";
      },
      default: "",
      trim: true
    },
    phone: {
      type: String,
      default: ""
    },
    acceptedTerms: {
      type: Boolean,
      default: true
    },
    acceptedTermsAt: {
      type: Date,
      default: Date.now
    },
    profilePhoto: {
      type: String,
      default: ""
    },
    role: {
      type: String,
      enum: ["student", "warden", "staff"],
      default: "student"
    }
  },
  {
    timestamps: true
  }
);

module.exports = mongoose.model("User", userSchema);