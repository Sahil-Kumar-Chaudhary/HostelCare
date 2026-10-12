const express = require("express");
const mongoose = require("mongoose");
const Complaint = require("../models/Complaint");
const User = require("../models/User");
const Notification = require("../models/Notification");
const auth = require("../middleware/auth");

const router = express.Router();

/**
 * 1. GET /api/staff/tasks
 * Tasks strictly assigned to the authenticated staff member
 */
router.get("/tasks", auth, auth.requireStaff, async (req, res) => {
  try {
    const { status } = req.query;

    const query = { assignedStaff: req.user.userId };
    if (status && status !== "all") {
      query.status = status.toLowerCase();
    }

    const [tasks, totalAssigned, inProgressCount, completedCount, pendingCount] = await Promise.all([
      Complaint.find(query)
        .sort({ updatedAt: -1 })
        .populate({
          path: "student",
          select: "name studentId phone roomNumber hostelBlock"
        }),
      Complaint.countDocuments({ assignedStaff: req.user.userId }),
      Complaint.countDocuments({ assignedStaff: req.user.userId, status: "in_progress" }),
      Complaint.countDocuments({ assignedStaff: req.user.userId, status: "resolved" }),
      Complaint.countDocuments({ assignedStaff: req.user.userId, status: "assigned" })
    ]);

    const formattedTasks = tasks.map((t) => ({
      id: t._id.toString(),
      ticketId: t.ticketId || "",
      title: t.title,
      description: t.description,
      category: t.category,
      priority: t.priority,
      status: t.status,
      hostelBlock: t.hostelBlock,
      roomNumber: t.roomNumber,
      photoUrl: t.photoUrl || "",
      aiSummary: t.aiSummary || "",
      assignmentInstructions: t.assignmentInstructions || "",
      workNotes: t.workNotes || "",
      createdAt: t.createdAt,
      student: t.student
        ? {
            id: t.student._id ? t.student._id.toString() : "",
            name: t.student.name || "Student",
            phone: t.student.phone || "",
            roomNumber: t.roomNumber,
            hostelBlock: t.hostelBlock
          }
        : null
    }));

    res.json({
      success: true,
      stats: {
        totalAssigned,
        inProgressCount,
        completedCount,
        pendingCount
      },
      tasks: formattedTasks
    });
  } catch (error) {
    console.error("Staff tasks error:", error.message);
    res.status(500).json({ message: "Server error" });
  }
});

/**
 * 2. GET /api/staff/tasks/:id
 * Task details for staff member
 */
router.get("/tasks/:id", auth, auth.requireStaff, async (req, res) => {
  try {
    const task = await Complaint.findById(req.params.id)
      .populate({
        path: "student",
        select: "name studentId phone email roomNumber hostelBlock"
      })
      .populate({
        path: "timeline.updatedBy",
        select: "name role"
      });

    if (!task) {
      return res.status(404).json({ message: "Task not found" });
    }

    if (!task.assignedStaff || task.assignedStaff.toString() !== req.user.userId) {
      return res.status(403).json({ message: "Access denied. Task is not assigned to you." });
    }

    res.json({
      success: true,
      task: {
        id: task._id.toString(),
        ticketId: task.ticketId,
        title: task.title,
        description: task.description,
        category: task.category,
        priority: task.priority,
        status: task.status,
        hostelBlock: task.hostelBlock,
        roomNumber: task.roomNumber,
        photoUrl: task.photoUrl || "",
        completionPhotoUrl: task.completionPhotoUrl || "",
        assignmentInstructions: task.assignmentInstructions || "",
        workNotes: task.workNotes || "",
        aiSummary: task.aiSummary || "",
        createdAt: task.createdAt,
        student: task.student
          ? {
              name: task.student.name,
              phone: task.student.phone || "",
              roomNumber: task.roomNumber,
              hostelBlock: task.hostelBlock
            }
          : null,
        timeline: task.timeline || []
      }
    });
  } catch (error) {
    console.error("Staff task details error:", error.message);
    res.status(500).json({ message: "Server error" });
  }
});

/**
 * 3. PATCH /api/staff/tasks/:id/status
 * Staff updates work progress (e.g. starts work or adds work note)
 */
router.patch("/tasks/:id/status", auth, auth.requireStaff, async (req, res) => {
  try {
    const { status, workNotes } = req.body;

    const task = await Complaint.findById(req.params.id);
    if (!task) return res.status(404).json({ message: "Task not found" });

    if (!task.assignedStaff || task.assignedStaff.toString() !== req.user.userId) {
      return res.status(403).json({ message: "Access denied. Task is not assigned to you." });
    }

    if (status && ["in_progress"].includes(status.toLowerCase())) {
      task.status = status.toLowerCase();
    }

    if (workNotes) {
      task.workNotes = workNotes.trim();
    }

    task.timeline.push({
      status: task.status,
      note: workNotes ? workNotes.trim() : `Staff updated task status to ${task.status}`,
      updatedBy: req.user.userId
    });

    await task.save();

    res.json({
      success: true,
      message: "Task progress updated",
      status: task.status,
      workNotes: task.workNotes
    });
  } catch (error) {
    console.error("Staff update task error:", error.message);
    res.status(500).json({ message: "Server error" });
  }
});

/**
 * 4. POST /api/staff/tasks/:id/complete
 * Staff submits completion request to Warden with optional photo and note
 */
router.post("/tasks/:id/complete", auth, auth.requireStaff, async (req, res) => {
  try {
    const { completionNotes, completionPhotoUrl } = req.body;

    const task = await Complaint.findById(req.params.id);
    if (!task) return res.status(404).json({ message: "Task not found" });

    if (!task.assignedStaff || task.assignedStaff.toString() !== req.user.userId) {
      return res.status(403).json({ message: "Access denied. Task is not assigned to you." });
    }

    if (completionPhotoUrl) {
      task.completionPhotoUrl = completionPhotoUrl;
    }

    if (completionNotes) {
      task.workNotes = completionNotes.trim();
    }

    task.status = "under_review";
    task.timeline.push({
      status: "under_review",
      note: completionNotes ? `Work completed by staff: ${completionNotes.trim()}` : "Work completed by staff. Awaiting warden review.",
      updatedBy: req.user.userId
    });

    await task.save();

    // Create notification for Warden(s) of this hostel
    try {
      const student = await User.findById(task.student);
      const hostelName = task.hostelName || (student && student.hostelName);
      if (hostelName) {
        const wardens = await User.find({ role: "warden", hostelName: hostelName });
        for (const w of wardens) {
          await Notification.create({
            recipient: w._id,
            title: `Completion Review: ${task.title}`,
            message: `Staff member completed work on ${task.ticketId} in Room ${task.roomNumber}. Review and resolve.`,
            type: "completion_requested",
            complaintId: task._id
          });
        }
      }
    } catch (notifErr) {
      console.warn("Notification error:", notifErr.message);
    }

    res.json({
      success: true,
      message: "Work submitted for completion review",
      status: task.status
    });
  } catch (error) {
    console.error("Staff complete task error:", error.message);
    res.status(500).json({ message: "Server error" });
  }
});

module.exports = router;
