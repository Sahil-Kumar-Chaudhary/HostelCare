const express = require("express");
const mongoose = require("mongoose");
const cors = require("cors");
require("dotenv").config();

const auth = require("./middleware/auth");
const authRoutes = require("./routes/auth");
const profileRoutes = require("./routes/profile");
const complaintRoutes = require("./routes/complaints");

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(express.json());

app.use("/api/auth", authRoutes);
app.use("/api/profile", profileRoutes);
app.use("/api/complaints", complaintRoutes);

app.get("/", (req, res) => {
  res.json({
    message: "HostelCare backend is running"
  });
});

app.get("/api/protected", auth, (req, res) => {
  res.json({
    message: "You have access to the protected route",
    user: req.user
  });
});

mongoose
  .connect(process.env.MONGO_URI)
  .then(() => {
    console.log("MongoDB connected successfully");

    app.listen(PORT, "0.0.0.0", () => {
      console.log(`Server running on port ${PORT}`);
    });
  })
  .catch((error) => {
    console.error("MongoDB connection failed:", error.message);
    process.exit(1);
  });