const mongoose = require("mongoose");

const timelineSchema = new mongoose.Schema(
  {
    status: {
      type: String,
      enum: [
        "submitted",
        "under_review",
        "assigned",
        "in_progress",
        "resolved"
      ],
      required: true
    },
    note: {
      type: String,
      default: ""
    },
    updatedBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      default: null
    }
  },
  {
    timestamps: true
  }
);

const complaintSchema = new mongoose.Schema(
  {
    ticketId: {
      type: String,
      unique: true,
      default: () =>
        `HC-${Date.now().toString().slice(-6)}`
    },

    student: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true
    },

    title: {
      type: String,
      required: true,
      trim: true,
      maxlength: 150
    },

    description: {
      type: String,
      required: true,
      trim: true,
      maxlength: 1000
    },

    category: {
      type: String,
      enum: [
        "plumbing",
        "electrical",
        "internet",
        "cleaning",
        "furniture",
        "other"
      ],
      required: true
    },

    priority: {
      type: String,
      enum: [
        "low",
        "medium",
        "high",
        "emergency"
      ],
      default: "medium"
    },

    hostelBlock: {
      type: String,
      required: true,
      trim: true
    },

    roomNumber: {
      type: String,
      required: true,
      trim: true
    },

    photoUrl: {
      type: String,
      default: ""
    },

    aiSummary: {
      type: String,
      default: "",
      trim: true
    },

    status: {
      type: String,
      enum: [
        "submitted",
        "under_review",
        "assigned",
        "in_progress",
        "resolved"
      ],
      default: "submitted"
    },

    assignedStaff: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      default: null
    },

    resolutionNote: {
      type: String,
      default: "",
      trim: true
    },

    timeline: {
      type: [timelineSchema],
      default: []
    }
  },
  {
    timestamps: true
  }
);

module.exports = mongoose.model("Complaint", complaintSchema);