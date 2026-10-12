const jwt = require("jsonwebtoken");

const auth = (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      return res.status(401).json({
        message: "Authentication required"
      });
    }

    const token = authHeader.split(" ")[1];

    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    req.user = decoded;

    next();
  } catch (error) {
    return res.status(401).json({
      message: "Invalid or expired token"
    });
  }
};

/**
 * Reusable role authorization middleware generator
 * @param  {...string} allowedRoles Roles permitted to access the route
 */
const requireRole = (...allowedRoles) => {
  const normalized = allowedRoles.map((r) => r.toLowerCase());
  return (req, res, next) => {
    if (!req.user || !req.user.role) {
      return res.status(401).json({
        message: "Authentication required"
      });
    }

    const userRole = req.user.role.toLowerCase();
    if (!normalized.includes(userRole)) {
      return res.status(403).json({
        message: `Forbidden: requires one of [${allowedRoles.join(", ")}] role`
      });
    }

    next();
  };
};

// Reusable helpers attached directly to auth function
auth.requireRole = requireRole;
auth.requireWarden = requireRole("warden");
auth.requireStaff = requireRole("staff", "warden");
auth.requireStudent = requireRole("student");

module.exports = auth;
module.exports.auth = auth;
module.exports.requireRole = requireRole;
module.exports.requireWarden = auth.requireWarden;
module.exports.requireStaff = auth.requireStaff;
module.exports.requireStudent = auth.requireStudent;