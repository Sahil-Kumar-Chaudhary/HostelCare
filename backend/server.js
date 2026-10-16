const express = require("express");
const mongoose = require("mongoose");
const cors = require("cors");
const path = require("path");
require("dotenv").config({ path: path.resolve(__dirname, ".env") });

const auth = require("./middleware/auth");
const authRoutes = require("./routes/auth");
const profileRoutes = require("./routes/profile");
const complaintRoutes = require('./routes/complaints');
const aiRoutes = require('./routes/ai');
const wardenRoutes = require('./routes/warden');
const staffRoutes = require('./routes/staff');
const notificationRoutes = require('./routes/notifications');

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(express.json());

app.use("/api/auth", authRoutes);
app.use("/api/profile", profileRoutes);
app.use('/api/complaints', complaintRoutes);
app.use('/api/ai', aiRoutes);
app.use('/api/warden', wardenRoutes);
app.use('/api/staff', staffRoutes);
app.use('/api/notifications', notificationRoutes);

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
  .connect(process.env.MONGO_URI, {
    dbName: process.env.MONGODB_DB_NAME || process.env.DB_NAME || "HostelCareDB"
  })
  .then(() => {
    console.log(`MongoDB connected successfully to database: ${mongoose.connection.name}`);

    app.listen(PORT, "0.0.0.0", () => {
      console.log(`Server running on port ${PORT}`);
    });
  })
  .catch((error) => {
    console.error("MongoDB connection failed:", error.message);
    process.exit(1);
  });