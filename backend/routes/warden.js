const express = require("express");
const mongoose = require("mongoose");
const bcrypt = require("bcryptjs");
const User = require("../models/User");
const Complaint = require("../models/Complaint");
const Notification = require("../models/Notification");
const auth = require("../middleware/auth");
const {
  escapeRegex,
  normalizeHostelName,
  buildHostelRegex,
  isHostelMatch,
  buildBlockFilter,
  checkBlockAuthorized,
  isValidHostelForGender,
  getCanonicalHostelName
} = require("../utils/hostelUtils");
const {
  generateUniqueWardenId,
  generateUniqueStaffId,
  generateSecureTemporaryPassword
} = require("../utils/idGenerator");

const router = express.Router();

function isComplaintAuthorized(complaint, warden, studentIds) {
  if (!complaint) return false;

  const resolvedHostel = complaint.hostelName && complaint.hostelName.trim()
    ? complaint.hostelName.trim()
    : (complaint.student && complaint.student.hostelName ? complaint.student.hostelName.trim() : "");

  // 1. If a complaint has no hostel name, it must not be visible to any Warden until properly assigned.
  if (!resolvedHostel) {
    return false;
  }

  // 2. Direct match on complaint.hostelName with format flexibility (e.g., BH-1 vs BH1):
  // A Warden for "Hostel A" must never see complaints from "Hostel B".
  if (!isHostelMatch(resolvedHostel, warden.hostelName)) {
    return false;
  }

  // 3. Hostel name matches! Now check block authorization if warden has a specific block assigned.
  return checkBlockAuthorized(complaint, warden);
}

/**
 * Helper to resolve the authenticated Warden's authorized scope
 */
async function getWardenScope(wardenId) {
  const warden = await User.findById(wardenId).select("-password");
  if (!warden) {
    const error = new Error("Warden account not found");
    error.status = 404;
    throw error;
  }

  if (!warden.hostelName || !warden.hostelName.trim()) {
    const error = new Error(
      "Warden account does not have an authorized hostel assignment. Scoped access cannot be determined."
    );
    error.status = 400;
    throw error;
  }

  const cleanHostelName = warden.hostelName.trim();
  const hostelRegex = buildHostelRegex(cleanHostelName);

  const studentIds = await User.find({
    role: "student",
    hostelName: { $regex: hostelRegex }
  }).distinct("_id");

  const complaintQuery = {
    student: { $in: studentIds },
    hostelName: { $regex: hostelRegex }
  };

  if (warden.hostelBlock && warden.hostelBlock.trim() && warden.hostelBlock.trim().toLowerCase() !== "all") {
    const blockRegex = buildBlockFilter(warden.hostelBlock);
    if (blockRegex) {
      complaintQuery.hostelBlock = { $regex: blockRegex };
    }
  }

  return { warden, cleanHostelName, studentIds, complaintQuery, hostelRegex };
}

/**
 * 1. GET /api/warden/dashboard
 * Aggregated metrics and recent complaints scoped to Warden's hostel
 */
router.get("/dashboard", auth, auth.requireWarden, async (req, res) => {
  try {
    const { warden, complaintQuery } = await getWardenScope(req.user.userId);

    const [
      totalCount,
      submittedCount,
      underReviewCount,
      assignedCount,
      inProgressCount,
      resolvedCount,
      priorityIssuesCount,
      recentComplaintsList
    ] = await Promise.all([
      Complaint.countDocuments(complaintQuery),
      Complaint.countDocuments({ ...complaintQuery, status: "submitted" }),
      Complaint.countDocuments({ ...complaintQuery, status: "under_review" }),
      Complaint.countDocuments({ ...complaintQuery, status: "assigned" }),
      Complaint.countDocuments({ ...complaintQuery, status: "in_progress" }),
      Complaint.countDocuments({ ...complaintQuery, status: "resolved" }),
      Complaint.countDocuments({
        ...complaintQuery,
        priority: { $in: ["high", "emergency"] },
        status: { $ne: "resolved" }
      }),
      Complaint.find(complaintQuery)
        .sort({ createdAt: -1 })
        .limit(10)
        .populate({
          path: "student",
          select: "name studentId email phone hostelBlock roomNumber"
        })
        .populate({
          path: "assignedStaff",
          select: "name email phone staffId department"
        })
    ]);

    const pendingCount = submittedCount + underReviewCount;
    const activeInProgressCount = assignedCount + inProgressCount;

    const formattedRecentComplaints = recentComplaintsList.map((c) => ({
      id: c._id.toString(),
      ticketId: c.ticketId || "",
      title: c.title,
      description: c.description,
      category: c.category,
      priority: c.priority,
      status: c.status,
      hostelBlock: c.hostelBlock,
      roomNumber: c.roomNumber,
      photoUrl: c.photoUrl || "",
      aiSummary: c.aiSummary || "",
      createdAt: c.createdAt,
      student: c.student
        ? {
            id: c.student._id ? c.student._id.toString() : "",
            name: c.student.name || "Student",
            studentId: c.student.studentId || "",
            roomNumber: c.roomNumber,
            hostelBlock: c.hostelBlock
          }
        : null,
      assignedStaff: c.assignedStaff
        ? {
            id: c.assignedStaff._id ? c.assignedStaff._id.toString() : "",
            name: c.assignedStaff.name || "Staff",
            department: c.assignedStaff.department || ""
          }
        : null
    }));

    res.json({
      success: true,
      warden: {
        name: warden.name,
        email: warden.email,
        hostelName: warden.hostelName,
        hostelBlock: warden.hostelBlock || ""
      },
      stats: {
        total: totalCount,
        pending: pendingCount,
        inProgress: activeInProgressCount,
        resolved: resolvedCount,
        priorityIssues: priorityIssuesCount
      },
      recentComplaints: formattedRecentComplaints
    });
  } catch (error) {
    console.error("Warden dashboard error:", error.message);
    res.status(error.status || 500).json({ message: error.message || "Server error" });
  }
});

/**
 * 2. GET /api/warden/complaints
 * Search and filter complaints scoped to Warden's hostel
 */
router.get("/complaints", auth, auth.requireWarden, async (req, res) => {
  try {
    const { complaintQuery, cleanHostelName } = await getWardenScope(req.user.userId);
    const { search, status, category, priority } = req.query;

    const query = { ...complaintQuery };

    // Status filter
    if (status && status !== "all") {
      if (status === "pending") {
        query.status = { $in: ["submitted", "under_review"] };
      } else if (status === "in_progress") {
        query.status = { $in: ["assigned", "in_progress"] };
      } else {
        query.status = status.toLowerCase();
      }
    }

    // Category filter
    if (category && category !== "all") {
      query.category = category.toLowerCase();
    }

    // Priority filter
    if (priority && priority !== "all") {
      query.priority = priority.toLowerCase();
    }

    // Search filter across title, ticketId, roomNumber, hostelBlock
    if (search && search.trim()) {
      const cleanSearch = search.trim();
      const searchRegex = new RegExp(escapeRegex(cleanSearch), "i");

      // Also find students matching name
      const matchingStudentIds = await User.find({
        role: "student",
        hostelName: { $regex: buildHostelRegex(cleanHostelName) },
        name: searchRegex
      }).distinct("_id");

      query.$and = [
        {
          $or: [
            { title: searchRegex },
            { ticketId: searchRegex },
            { roomNumber: searchRegex },
            { hostelBlock: searchRegex },
            { category: searchRegex },
            { student: { $in: matchingStudentIds } }
          ]
        }
      ];
    }

    const complaintsList = await Complaint.find(query)
      .sort({ createdAt: -1 })
      .populate({
        path: "student",
        select: "name studentId email phone hostelBlock roomNumber"
      })
      .populate({
        path: "assignedStaff",
        select: "name email phone staffId department"
      });

    const formattedComplaints = complaintsList.map((c) => ({
      id: c._id.toString(),
      ticketId: c.ticketId || "",
      title: c.title,
      description: c.description,
      category: c.category,
      priority: c.priority,
      status: c.status,
      hostelBlock: c.hostelBlock,
      roomNumber: c.roomNumber,
      photoUrl: c.photoUrl || "",
      aiSummary: c.aiSummary || "",
      createdAt: c.createdAt,
      student: c.student
        ? {
            id: c.student._id ? c.student._id.toString() : "",
            name: c.student.name || "Student",
            studentId: c.student.studentId || "",
            roomNumber: c.roomNumber,
            hostelBlock: c.hostelBlock
          }
        : null,
      assignedStaff: c.assignedStaff
        ? {
            id: c.assignedStaff._id ? c.assignedStaff._id.toString() : "",
            name: c.assignedStaff.name || "Staff",
            department: c.assignedStaff.department || ""
          }
        : null
    }));

    res.json({
      success: true,
      count: formattedComplaints.length,
      complaints: formattedComplaints
    });
  } catch (error) {
    console.error("Warden complaints list error:", error.message);
    res.status(error.status || 500).json({ message: error.message || "Server error" });
  }
});

/**
 * 3. GET /api/warden/complaints/:id
 * Detailed complaint view with student, staff, and timeline
 */
router.get("/complaints/:id", auth, auth.requireWarden, async (req, res) => {
  try {
    const { warden, studentIds } = await getWardenScope(req.user.userId);

    const complaint = await Complaint.findById(req.params.id)
      .populate({
        path: "student",
        select: "name studentId email phone hostelName hostelBlock roomNumber"
      })
      .populate({
        path: "assignedStaff",
        select: "name email phone staffId department"
      })
      .populate({
        path: "timeline.updatedBy",
        select: "name role"
      });

    if (!complaint) {
      return res.status(404).json({ message: "Complaint not found" });
    }

    if (!isComplaintAuthorized(complaint, warden, studentIds)) {
      return res.status(403).json({ message: "Access denied. Complaint is outside authorized hostel scope." });
    }

    res.json({
      success: true,
      complaint: {
        id: complaint._id.toString(),
        ticketId: complaint.ticketId || "",
        title: complaint.title,
        description: complaint.description,
        category: complaint.category,
        priority: complaint.priority,
        status: complaint.status,
        hostelName: complaint.hostelName || (complaint.student ? complaint.student.hostelName : "") || "",
        hostelBlock: complaint.hostelBlock,
        roomNumber: complaint.roomNumber,
        photoUrl: complaint.photoUrl || "",
        completionPhotoUrl: complaint.completionPhotoUrl || "",
        aiSummary: complaint.aiSummary || "",
        assignmentInstructions: complaint.assignmentInstructions || "",
        workNotes: complaint.workNotes || "",
        resolutionNote: complaint.resolutionNote || "",
        createdAt: complaint.createdAt,
        student: complaint.student
          ? {
              id: complaint.student._id.toString(),
              name: complaint.student.name,
              studentId: complaint.student.studentId || "",
              email: complaint.student.email,
              phone: complaint.student.phone || "",
              hostelName: complaint.student.hostelName || complaint.hostelName || "",
              hostelBlock: complaint.hostelBlock || complaint.student.hostelBlock || "",
              roomNumber: complaint.roomNumber || complaint.student.roomNumber || ""
            }
          : null,
        assignedStaff: complaint.assignedStaff
          ? {
              id: complaint.assignedStaff._id.toString(),
              name: complaint.assignedStaff.name,
              staffId: complaint.assignedStaff.staffId || "",
              department: complaint.assignedStaff.department || "",
              phone: complaint.assignedStaff.phone || ""
            }
          : null,
        timeline: (complaint.timeline || []).map((t) => ({
          status: t.status,
          note: t.note || "",
          createdAt: t.createdAt,
          updatedBy: t.updatedBy ? { name: t.updatedBy.name, role: t.updatedBy.role } : null
        }))
      }
    });
  } catch (error) {
    console.error("Warden complaint details error:", error.message);
    res.status(error.status || 500).json({ message: error.message || "Server error" });
  }
});

/**
 * 4. PATCH /api/warden/complaints/:id/status
 * Update complaint status, resolution note, and timeline
 */
router.patch("/complaints/:id/status", auth, auth.requireWarden, async (req, res) => {
  try {
    const { warden, studentIds } = await getWardenScope(req.user.userId);
    const { status, note, resolutionNote } = req.body;

    const allowedStatuses = ["submitted", "under_review", "assigned", "in_progress", "resolved"];
    if (!status || !allowedStatuses.includes(status.toLowerCase())) {
      return res.status(400).json({
        message: `Invalid status. Must be one of: ${allowedStatuses.join(", ")}`
      });
    }

    const complaint = await Complaint.findById(req.params.id);
    if (!complaint) {
      return res.status(404).json({ message: "Complaint not found" });
    }

    if (!isComplaintAuthorized(complaint, warden, studentIds)) {
      return res.status(403).json({ message: "Access denied. Complaint is outside authorized hostel scope." });
    }

    const normalizedStatus = status.toLowerCase();
    complaint.status = normalizedStatus;

    if (normalizedStatus === "resolved") {
      complaint.resolutionNote = resolutionNote ? resolutionNote.trim() : (note ? note.trim() : "Resolved by Warden");
    }

    // Add entry to timeline
    complaint.timeline.push({
      status: normalizedStatus,
      note: note ? note.trim() : `Status changed to ${normalizedStatus} by Warden`,
      updatedBy: req.user.userId
    });

    await complaint.save();

    // Create notification for the student and assigned staff
    try {
      const studentNotifType = normalizedStatus === "resolved" ? "complaint_resolved" : (normalizedStatus === "in_progress" ? "complaint_reopened" : "status_updated");
      const studentTitle = normalizedStatus === "resolved" ? "Complaint Resolved" : (normalizedStatus === "in_progress" ? "Complaint In Progress" : "Complaint Status Updated");
      const studentMsg = normalizedStatus === "resolved"
        ? `Your complaint "${complaint.title}" has been resolved. Note: ${complaint.resolutionNote || "Work verified by Warden."}`
        : `Your complaint "${complaint.title}" is now marked as ${normalizedStatus.replace("_", " ")}.`;

      const existingStudentNotif = await Notification.findOne({
        recipient: complaint.student,
        type: studentNotifType,
        complaintId: complaint._id,
        createdAt: { $gte: new Date(Date.now() - 30000) }
      });
      if (!existingStudentNotif) {
        await Notification.create({
          recipient: complaint.student,
          title: studentTitle,
          message: studentMsg,
          type: studentNotifType,
          complaintId: complaint._id
        });
      }

      if (complaint.assignedStaff) {
        const staffNotifType = normalizedStatus === "resolved" ? "complaint_resolved" : (normalizedStatus === "in_progress" ? "complaint_reopened" : "status_updated");
        const staffTitle = normalizedStatus === "resolved" ? "Work Approved & Resolved" : (normalizedStatus === "in_progress" ? "Work Returned by Warden" : "Task Status Updated");
        const staffMsg = normalizedStatus === "resolved"
          ? `Work on ticket ${complaint.ticketId || "task"} has been verified and resolved by Warden.`
          : `Warden updated ticket ${complaint.ticketId || "task"} status to ${normalizedStatus.replace("_", " ")}: ${note || "Please check details."}`;

        const existingStaffNotif = await Notification.findOne({
          recipient: complaint.assignedStaff,
          type: staffNotifType,
          complaintId: complaint._id,
          createdAt: { $gte: new Date(Date.now() - 30000) }
        });
        if (!existingStaffNotif) {
          await Notification.create({
            recipient: complaint.assignedStaff,
            title: staffTitle,
            message: staffMsg,
            type: staffNotifType,
            complaintId: complaint._id
          });
        }
      }
    } catch (notifErr) {
      console.warn("Notification creation warning:", notifErr.message);
    }

    res.json({
      success: true,
      message: "Complaint status updated successfully",
      status: complaint.status,
      resolutionNote: complaint.resolutionNote
    });
  } catch (error) {
    console.error("Warden update status error:", error.message);
    res.status(error.status || 500).json({ message: error.message || "Server error" });
  }
});

/**
 * 5. POST /api/warden/complaints/:id/assign
 * Assign complaint to an active scoped staff member
 */
router.post("/complaints/:id/assign", auth, auth.requireWarden, async (req, res) => {
  try {
    const { studentIds, cleanHostelName, warden } = await getWardenScope(req.user.userId);
    const { staffId, instructions } = req.body;

    if (!staffId) {
      return res.status(400).json({ message: "Staff member selection is required" });
    }

    const complaint = await Complaint.findById(req.params.id);
    if (!complaint) {
      return res.status(404).json({ message: "Complaint not found" });
    }

    if (!isComplaintAuthorized(complaint, warden, studentIds)) {
      return res.status(403).json({ message: "Access denied. Complaint is outside authorized hostel scope." });
    }

    // Verify staff exists, belongs to warden's hostel, is staff role, and active
    const staff = await User.findById(staffId);
    if (!staff || staff.role !== "staff") {
      return res.status(400).json({ message: "Invalid staff member selected" });
    }

    if (staff.isActive === false) {
      return res.status(400).json({ message: "Selected staff member is inactive and cannot be assigned tasks" });
    }

    if (staff.hostelName && !isHostelMatch(staff.hostelName, cleanHostelName)) {
      return res.status(403).json({ message: "Staff member does not belong to your authorized hostel" });
    }

    complaint.assignedStaff = staff._id;
    complaint.assignmentInstructions = instructions ? instructions.trim() : "";
    complaint.status = "assigned";

    complaint.timeline.push({
      status: "assigned",
      note: instructions ? instructions.trim() : `Assigned to ${staff.name} (${staff.department || "Staff"})`,
      updatedBy: warden._id
    });

    await complaint.save();

    // Create notifications for staff and student with deduplication
    try {
      if (mongoose.Types.ObjectId.isValid(staff._id) && mongoose.Types.ObjectId.isValid(complaint._id)) {
        const existingStaffNotif = await Notification.findOne({
          recipient: staff._id,
          type: "complaint_assigned",
          complaintId: complaint._id
        });
        if (!existingStaffNotif) {
          await Notification.create({
            recipient: staff._id,
            title: "Task Assigned",
            message: `You have been assigned to ${complaint.ticketId || "complaint"} (${complaint.title}).`,
            type: "complaint_assigned",
            complaintId: complaint._id
          });
        }
      }

      if (mongoose.Types.ObjectId.isValid(complaint.student) && mongoose.Types.ObjectId.isValid(complaint._id)) {
        const existingStudentNotif = await Notification.findOne({
          recipient: complaint.student,
          type: "complaint_assigned",
          complaintId: complaint._id
        });
        if (!existingStudentNotif) {
          await Notification.create({
            recipient: complaint.student,
            title: "Staff Assigned to Your Complaint",
            message: `${staff.name} (${staff.department || "Staff"}) has been assigned to your complaint "${complaint.title}".`,
            type: "complaint_assigned",
            complaintId: complaint._id
          });
        }
      }
    } catch (notifErr) {
      console.warn("Notification creation warning:", notifErr.message);
    }

    res.json({
      success: true,
      message: `Complaint assigned to ${staff.name} successfully`,
      assignedStaff: {
        id: staff._id.toString(),
        name: staff.name,
        department: staff.department || ""
      },
      status: complaint.status
    });
  } catch (error) {
    console.error("Warden assign complaint error:", error.message);
    res.status(error.status || 500).json({ message: error.message || "Server error" });
  }
});

/**
 * 6. GET /api/warden/staff
 * List staff members scoped to Warden's hostel with assignment metrics
 */
router.get("/staff", auth, auth.requireWarden, async (req, res) => {
  try {
    const { cleanHostelName } = await getWardenScope(req.user.userId);
    const { filter, search } = req.query;

    const query = {
      role: "staff",
      hostelName: { $regex: buildHostelRegex(cleanHostelName) }
    };

    if (filter === "active") {
      query.isActive = { $ne: false };
    } else if (filter === "inactive") {
      query.isActive = false;
    }

    if (search && search.trim()) {
      const searchRegex = new RegExp(escapeRegex(search.trim()), "i");
      query.$or = [
        { name: searchRegex },
        { department: searchRegex },
        { staffId: searchRegex },
        { email: searchRegex }
      ];
    }

    const staffList = await User.find(query).select("-password").sort({ createdAt: -1 });

    // Calculate assignment statistics for each staff member
    const staffWithMetrics = await Promise.all(
      staffList.map(async (s) => {
        const [totalAssigned, completedCount, inProgressCount, pendingCount] = await Promise.all([
          Complaint.countDocuments({ assignedStaff: s._id }),
          Complaint.countDocuments({ assignedStaff: s._id, status: "resolved" }),
          Complaint.countDocuments({ assignedStaff: s._id, status: "in_progress" }),
          Complaint.countDocuments({ assignedStaff: s._id, status: "assigned" })
        ]);

        return {
          id: s._id.toString(),
          staffId: s.staffId || "",
          name: s.name,
          email: s.email,
          phone: s.phone || "",
          department: s.department || "General Maintenance",
          hostelName: s.hostelName || cleanHostelName,
          hostelBlock: s.hostelBlock || "",
          isActive: s.isActive !== false,
          totalAssigned,
          completedCount,
          inProgressCount,
          pendingCount
        };
      })
    );

    res.json({
      success: true,
      count: staffWithMetrics.length,
      staff: staffWithMetrics
    });
  } catch (error) {
    console.error("Warden staff list error:", error.message);
    res.status(error.status || 500).json({ message: error.message || "Server error" });
  }
});

/**
 * 7. POST /api/warden/staff
 * Warden creates a staff account scoped to their authorized hostel
 */
router.post("/staff", auth, auth.requireWarden, async (req, res) => {
  try {
    const { warden, cleanHostelName } = await getWardenScope(req.user.userId);
    const { name, email, phone, department, hostelBlock, password } = req.body;

    if (!name || !email || !password || !department) {
      return res.status(400).json({
        message: "Name, email, department, and temporary password are required"
      });
    }

    const cleanName = name.trim();
    const cleanEmail = email.trim().toLowerCase();
    const cleanDept = department.trim();
    const cleanPhone = phone ? phone.trim() : "";
    const cleanBlock = hostelBlock ? hostelBlock.trim() : warden.hostelBlock || "";

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(cleanEmail)) {
      return res.status(400).json({ message: "Invalid email format" });
    }

    if (password.length < 8) {
      return res.status(400).json({ message: "Temporary password must be at least 8 characters" });
    }

    // Email uniqueness check
    const existingUser = await User.findOne({ email: cleanEmail });
    if (existingUser) {
      return res.status(409).json({ message: "An account with this email already exists" });
    }

    // Generate collision-safe unique staff ID
    const generatedStaffId = await generateUniqueStaffId(User);

    let tempPassword = password ? password.trim() : "";
    if (!tempPassword) {
      tempPassword = generateSecureTemporaryPassword(12);
    } else if (tempPassword.length < 8) {
      return res.status(400).json({ message: "Temporary password must be at least 8 characters" });
    }

    const hashedPassword = await bcrypt.hash(tempPassword, 10);

    const staff = await User.create({
      name: cleanName,
      email: cleanEmail,
      phone: cleanPhone,
      department: cleanDept,
      staffId: generatedStaffId,
      hostelName: cleanHostelName,
      hostelBlock: cleanBlock,
      password: hashedPassword,
      role: "staff",
      isActive: true,
      mustChangePassword: true,
      acceptedTerms: true
    });

    res.status(201).json({
      success: true,
      message: "Staff member created successfully",
      staff: {
        id: staff._id.toString(),
        staffId: staff.staffId,
        name: staff.name,
        email: staff.email,
        phone: staff.phone,
        department: staff.department,
        hostelName: staff.hostelName,
        hostelBlock: staff.hostelBlock,
        isActive: staff.isActive
      },
      temporaryPassword: tempPassword
    });
  } catch (error) {
    console.error("Warden create staff error:", error.message);
    res.status(error.status || 500).json({ message: error.message || "Server error" });
  }
});

/**
 * 7B. GET /api/warden/wardens
 * List all wardens
 */
router.get("/wardens", auth, auth.requireWarden, async (req, res) => {
  try {
    const wardens = await User.find({ role: "warden" })
      .select("_id wardenId name email gender hostelName phone createdAt")
      .sort({ createdAt: -1 })
      .lean();

    const formatted = wardens.map(w => ({
      id: w._id.toString(),
      wardenId: w.wardenId || "",
      name: w.name,
      email: w.email,
      gender: w.gender || "",
      hostelName: w.hostelName || "",
      phone: w.phone || "",
      createdAt: w.createdAt
    }));

    res.json({
      success: true,
      wardens: formatted
    });
  } catch (error) {
    console.error("Get wardens error:", error.message);
    res.status(500).json({ message: "Server error" });
  }
});

/**
 * 7C. POST /api/warden/wardens
 * Create a new warden account (protected, public registration forbidden)
 */
router.post("/wardens", auth, auth.requireWarden, async (req, res) => {
  try {
    const { name, email, phone, gender, hostelName, password } = req.body;

    if (!name || !email || !gender || !hostelName) {
      return res.status(400).json({
        message: "Full Name, Email, Gender, and Assigned Hostel are required"
      });
    }

    const cleanName = name.trim();
    const cleanEmail = email.trim().toLowerCase();
    const cleanPhone = phone ? phone.trim() : "";
    const cleanGenderRaw = gender.trim().toLowerCase();

    if (!["male", "female"].includes(cleanGenderRaw)) {
      return res.status(400).json({
        message: "Gender must be either Male or Female"
      });
    }
    const cleanGender = cleanGenderRaw === "male" ? "Male" : "Female";

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(cleanEmail)) {
      return res.status(400).json({ message: "Invalid email format" });
    }

    // Validate hostel matches gender
    const hostelCheck = isValidHostelForGender(cleanGender, hostelName);
    if (!hostelCheck.valid) {
      return res.status(400).json({ message: hostelCheck.message });
    }
    const cleanHostelName = hostelCheck.canonicalHostel;

    // Email uniqueness check
    const existingUser = await User.findOne({ email: cleanEmail });
    if (existingUser) {
      return res.status(409).json({ message: "An account with this email already exists" });
    }

    // Generate collision-safe sequential Warden ID: WRD-0001, WRD-0002...
    const generatedWardenId = await generateUniqueWardenId(User);

    // Password handling: use provided or generate secure temporary password
    let tempPassword = password ? password.trim() : "";
    if (!tempPassword) {
      tempPassword = generateSecureTemporaryPassword(12);
    } else if (tempPassword.length < 8) {
      return res.status(400).json({ message: "Temporary password must be at least 8 characters" });
    }

    const hashedPassword = await bcrypt.hash(tempPassword, 10);

    const warden = await User.create({
      name: cleanName,
      email: cleanEmail,
      phone: cleanPhone,
      gender: cleanGender,
      wardenId: generatedWardenId,
      hostelName: cleanHostelName,
      hostelBlock: "", // No block restriction: assigned to whole hostel
      password: hashedPassword,
      role: "warden",
      isActive: true,
      mustChangePassword: true,
      acceptedTerms: true,
      acceptedTermsAt: new Date()
    });

    res.status(201).json({
      success: true,
      message: "Warden account created successfully",
      warden: {
        id: warden._id.toString(),
        wardenId: warden.wardenId,
        name: warden.name,
        email: warden.email,
        gender: warden.gender,
        hostelName: warden.hostelName,
        phone: warden.phone,
        createdAt: warden.createdAt
      },
      temporaryPassword: tempPassword
    });
  } catch (error) {
    console.error("Warden create warden error:", error.message);
    res.status(error.status || 500).json({ message: error.message || "Server error" });
  }
});

/**
 * 8. GET /api/warden/staff/:id
 * Staff profile with task counts and history
 */
router.get("/staff/:id", auth, auth.requireWarden, async (req, res) => {
  try {
    const { cleanHostelName } = await getWardenScope(req.user.userId);

    const staff = await User.findById(req.params.id).select("-password");
    if (!staff || staff.role !== "staff") {
      return res.status(404).json({ message: "Staff member not found" });
    }

    if (staff.hostelName && !isHostelMatch(staff.hostelName, cleanHostelName)) {
      return res.status(403).json({ message: "Access denied. Staff member belongs to another hostel." });
    }

    const [totalAssigned, completedCount, inProgressCount, pendingCount, recentTasks] = await Promise.all([
      Complaint.countDocuments({ assignedStaff: staff._id }),
      Complaint.countDocuments({ assignedStaff: staff._id, status: "resolved" }),
      Complaint.countDocuments({ assignedStaff: staff._id, status: "in_progress" }),
      Complaint.countDocuments({ assignedStaff: staff._id, status: "assigned" }),
      Complaint.find({ assignedStaff: staff._id })
        .sort({ updatedAt: -1 })
        .limit(5)
        .select("title ticketId category status priority roomNumber hostelBlock createdAt")
    ]);

    res.json({
      success: true,
      staff: {
        id: staff._id.toString(),
        staffId: staff.staffId || "",
        name: staff.name,
        email: staff.email,
        phone: staff.phone || "",
        department: staff.department || "",
        hostelName: staff.hostelName || cleanHostelName,
        hostelBlock: staff.hostelBlock || "",
        isActive: staff.isActive !== false,
        stats: {
          totalAssigned,
          completedCount,
          inProgressCount,
          pendingCount
        },
        recentTasks: recentTasks.map((t) => ({
          id: t._id.toString(),
          ticketId: t.ticketId,
          title: t.title,
          category: t.category,
          status: t.status,
          priority: t.priority,
          roomNumber: t.roomNumber,
          hostelBlock: t.hostelBlock,
          createdAt: t.createdAt
        }))
      }
    });
  } catch (error) {
    console.error("Warden get staff details error:", error.message);
    res.status(error.status || 500).json({ message: error.message || "Server error" });
  }
});

/**
 * 9. PUT /api/warden/staff/:id
 * Update staff details (phone, department, block)
 */
router.put("/staff/:id", auth, auth.requireWarden, async (req, res) => {
  try {
    const { cleanHostelName } = await getWardenScope(req.user.userId);
    const { name, phone, department, hostelBlock } = req.body;

    const staff = await User.findById(req.params.id);
    if (!staff || staff.role !== "staff") {
      return res.status(404).json({ message: "Staff member not found" });
    }

    if (staff.hostelName && !isHostelMatch(staff.hostelName, cleanHostelName)) {
      return res.status(403).json({ message: "Access denied. Staff member belongs to another hostel." });
    }

    if (name) staff.name = name.trim();
    if (phone !== undefined) staff.phone = phone.trim();
    if (department) staff.department = department.trim();
    if (hostelBlock !== undefined) staff.hostelBlock = hostelBlock.trim();

    await staff.save();

    res.json({
      success: true,
      message: "Staff member updated successfully",
      staff: {
        id: staff._id.toString(),
        name: staff.name,
        email: staff.email,
        phone: staff.phone,
        department: staff.department,
        hostelBlock: staff.hostelBlock,
        isActive: staff.isActive
      }
    });
  } catch (error) {
    console.error("Warden update staff error:", error.message);
    res.status(error.status || 500).json({ message: error.message || "Server error" });
  }
});

/**
 * 10. PATCH /api/warden/staff/:id/status
 * Activate or deactivate staff member
 */
router.patch("/staff/:id/status", auth, auth.requireWarden, async (req, res) => {
  try {
    const { cleanHostelName } = await getWardenScope(req.user.userId);
    const { isActive } = req.body;

    if (typeof isActive !== "boolean") {
      return res.status(400).json({ message: "isActive boolean is required" });
    }

    const staff = await User.findById(req.params.id);
    if (!staff || staff.role !== "staff") {
      return res.status(404).json({ message: "Staff member not found" });
    }

    if (staff.hostelName && !isHostelMatch(staff.hostelName, cleanHostelName)) {
      return res.status(403).json({ message: "Access denied. Staff member belongs to another hostel." });
    }

    staff.isActive = isActive;
    await staff.save();

    res.json({
      success: true,
      message: `Staff member account ${isActive ? "activated" : "deactivated"} successfully`,
      isActive: staff.isActive
    });
  } catch (error) {
    console.error("Warden toggle staff status error:", error.message);
    res.status(error.status || 500).json({ message: error.message || "Server error" });
  }
});

/**
 * 11. GET /api/warden/reports
 * Aggregated trends and category analytics for Warden's hostel
 */
router.get("/reports", auth, auth.requireWarden, async (req, res) => {
  try {
    const { complaintQuery } = await getWardenScope(req.user.userId);
    const { timeRange } = req.query; // 7days, 30days, 90days, all

    const query = { ...complaintQuery };

    if (timeRange && timeRange !== "all") {
      const days = timeRange === "7days" ? 7 : timeRange === "90days" ? 90 : 30;
      const sinceDate = new Date();
      sinceDate.setDate(sinceDate.getDate() - days);
      query.createdAt = { $gte: sinceDate };
    }

    const [total, pendingCount, inProgressCount, resolvedCount, allComplaints] = await Promise.all([
      Complaint.countDocuments(query),
      Complaint.countDocuments({ ...query, status: { $in: ["submitted", "under_review"] } }),
      Complaint.countDocuments({ ...query, status: { $in: ["assigned", "in_progress"] } }),
      Complaint.countDocuments({ ...query, status: "resolved" }),
      Complaint.find(query).select("category status createdAt updatedAt timeline")
    ]);

    // Category breakdown
    const categoryCounts = {};
    allComplaints.forEach((c) => {
      const cat = c.category ? c.category.toLowerCase() : "other";
      categoryCounts[cat] = (categoryCounts[cat] || 0) + 1;
    });

    const categoryBreakdown = Object.entries(categoryCounts).map(([category, count]) => ({
      category,
      count
    }));

    // Date trends (last 5 intervals or daily grouped)
    const trendMap = {};
    allComplaints.forEach((c) => {
      const dateKey = new Date(c.createdAt).toLocaleDateString("en-US", { month: "short", day: "numeric" });
      trendMap[dateKey] = (trendMap[dateKey] || 0) + 1;
    });

    const trends = Object.entries(trendMap).map(([date, count]) => ({
      date,
      count
    }));

    // Calculate average resolution time if reliable
    let totalResolutionHours = 0;
    let resolvedWithTimestampsCount = 0;
    allComplaints.forEach((c) => {
      if (c.status === "resolved" && c.createdAt && c.updatedAt) {
        const diffMs = new Date(c.updatedAt).getTime() - new Date(c.createdAt).getTime();
        const diffHours = diffMs / (1000 * 60 * 60);
        if (diffHours >= 0) {
          totalResolutionHours += diffHours;
          resolvedWithTimestampsCount++;
        }
      }
    });

    const avgResolutionHours =
      resolvedWithTimestampsCount > 0 ? Math.round(totalResolutionHours / resolvedWithTimestampsCount) : 0;

    res.json({
      success: true,
      timeRange: timeRange || "30days",
      stats: {
        total,
        pending: pendingCount,
        inProgress: inProgressCount,
        resolved: resolvedCount,
        avgResolutionHours
      },
      categoryBreakdown,
      trends
    });
  } catch (error) {
    console.error("Warden reports error:", error.message);
    res.status(error.status || 500).json({ message: error.message || "Server error" });
  }
});

/**
 * 12. GET & PUT /api/warden/profile
 */
router.get("/profile", auth, auth.requireWarden, async (req, res) => {
  try {
    const warden = await User.findById(req.user.userId).select("-password");
    if (!warden) return res.status(404).json({ message: "Warden not found" });
    res.json({
      success: true,
      warden: {
        id: warden._id.toString(),
        wardenId: warden.wardenId || "",
        name: warden.name,
        email: warden.email,
        gender: warden.gender || "",
        phone: warden.phone || "",
        hostelName: warden.hostelName,
        hostelBlock: warden.hostelBlock || "",
        profilePhoto: warden.profilePhoto || "",
        role: warden.role
      }
    });
  } catch (error) {
    res.status(500).json({ message: "Server error" });
  }
});

router.put("/profile", auth, auth.requireWarden, async (req, res) => {
  try {
    const { name, phone } = req.body;
    const warden = await User.findById(req.user.userId);
    if (!warden) return res.status(404).json({ message: "Warden not found" });

    if (name) warden.name = name.trim();
    if (phone !== undefined) warden.phone = phone.trim();
    await warden.save();

    res.json({
      success: true,
      message: "Profile updated successfully",
      warden: {
        id: warden._id.toString(),
        name: warden.name,
        email: warden.email,
        phone: warden.phone || "",
        hostelName: warden.hostelName,
        hostelBlock: warden.hostelBlock || ""
      }
    });
  } catch (error) {
    res.status(500).json({ message: "Server error" });
  }
});

module.exports = router;
