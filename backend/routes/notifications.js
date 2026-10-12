const express = require("express");
const Notification = require("../models/Notification");
const auth = require("../middleware/auth");

const router = express.Router();

router.get("/", auth, async (req, res) => {
  try {
    const notifications = await Notification.find({ recipient: req.user.userId })
      .sort({ createdAt: -1 })
      .limit(50);
    const unreadCount = await Notification.countDocuments({
      recipient: req.user.userId,
      isRead: false
    });

    res.json({
      success: true,
      unreadCount,
      notifications: notifications.map((n) => ({
        id: n._id.toString(),
        title: n.title,
        message: n.message,
        type: n.type,
        complaintId: n.complaintId ? n.complaintId.toString() : null,
        isRead: n.isRead,
        createdAt: n.createdAt
      }))
    });
  } catch (error) {
    console.error("Get notifications error:", error.message);
    res.status(500).json({ message: "Server error" });
  }
});

router.patch("/:id/read", auth, async (req, res) => {
  try {
    const notification = await Notification.findOne({
      _id: req.params.id,
      recipient: req.user.userId
    });

    if (!notification) {
      return res.status(404).json({ message: "Notification not found" });
    }

    notification.isRead = true;
    await notification.save();

    res.json({ success: true, message: "Marked as read" });
  } catch (error) {
    res.status(500).json({ message: "Server error" });
  }
});

router.patch("/mark-all-read", auth, async (req, res) => {
  try {
    await Notification.updateMany(
      { recipient: req.user.userId, isRead: false },
      { $set: { isRead: true } }
    );

    res.json({ success: true, message: "All notifications marked as read" });
  } catch (error) {
    res.status(500).json({ message: "Server error" });
  }
});

module.exports = router;
