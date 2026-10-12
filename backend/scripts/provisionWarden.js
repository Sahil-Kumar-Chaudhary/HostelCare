const mongoose = require("mongoose");
const bcrypt = require("bcryptjs");
const path = require("path");
require("dotenv").config({ path: path.join(__dirname, "../.env") });

const User = require("../models/User");

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

/**
 * Secure one-time Warden provisioning utility.
 * Reads operator parameters from options object, environment variables, or CLI arguments.
 * NEVER prints passwords or password hashes.
 */
async function provisionWarden(options = {}) {
  const name = options.name || process.env.WARDEN_NAME;
  const email = options.email || process.env.WARDEN_EMAIL;
  const password = options.password || process.env.WARDEN_PASSWORD;
  const hostelName = options.hostelName || process.env.WARDEN_HOSTEL_NAME;
  const hostelBlock = options.hostelBlock || process.env.WARDEN_HOSTEL_BLOCK || "";
  const phone = options.phone || process.env.WARDEN_PHONE || "";
  const mongoUri = options.mongoUri || process.env.MONGO_URI;

  if (!name || !email || !password || !hostelName) {
    throw new Error(
      "Missing required fields. Please provide WARDEN_NAME, WARDEN_EMAIL, WARDEN_PASSWORD, and WARDEN_HOSTEL_NAME."
    );
  }

  const cleanName = name.trim();
  const cleanEmail = email.trim().toLowerCase();
  const cleanHostelName = hostelName.trim();
  const cleanHostelBlock = hostelBlock.trim();
  const cleanPhone = phone.trim();

  if (!cleanName || !cleanEmail || !cleanHostelName) {
    throw new Error("Name, email, and hostel name cannot be blank.");
  }

  if (!emailRegex.test(cleanEmail)) {
    throw new Error("Invalid email address format.");
  }

  if (!validatePassword(password)) {
    throw new Error(
      "Password must be 8-128 characters and include uppercase, lowercase, number, and special character."
    );
  }

  let shouldCloseConnection = false;
  if (mongoose.connection.readyState === 0) {
    if (!mongoUri) {
      throw new Error("MONGO_URI is required to connect to the database.");
    }
    const targetDbName = options.dbName || process.env.DB_NAME || "HostelCareLocalTest";
    await mongoose.connect(mongoUri, { dbName: targetDbName });
    shouldCloseConnection = true;
  }

  try {
    // Check if user already exists
    const existingUser = await User.findOne({ email: cleanEmail });
    if (existingUser) {
      console.log(
        `[Provisioning] An account with email "${cleanEmail}" already exists (Role: ${existingUser.role}). Refusing to overwrite.`
      );
      return {
        success: false,
        created: false,
        message: "Account already exists; refused to overwrite.",
        user: {
          id: existingUser._id.toString(),
          name: existingUser.name,
          email: existingUser.email,
          role: existingUser.role,
          hostelName: existingUser.hostelName
        }
      };
    }

    // Hash password securely with bcrypt
    const hashedPassword = await bcrypt.hash(password, 10);

    // Create warden document - omit studentId entirely to maintain clean schema and sparse index
    const warden = await User.create({
      name: cleanName,
      email: cleanEmail,
      password: hashedPassword,
      hostelName: cleanHostelName,
      hostelBlock: cleanHostelBlock,
      phone: cleanPhone,
      role: "warden",
      acceptedTerms: true,
      acceptedTermsAt: new Date()
    });

    console.log(
      `[Provisioning] Warden account successfully provisioned for "${cleanName}" (${cleanEmail}) - Assigned to "${cleanHostelName}".`
    );

    return {
      success: true,
      created: true,
      message: "Warden account provisioned successfully.",
      user: {
        id: warden._id.toString(),
        name: warden.name,
        email: warden.email,
        role: warden.role,
        hostelName: warden.hostelName,
        hostelBlock: warden.hostelBlock
      }
    };
  } finally {
    if (shouldCloseConnection && options.closeConnection !== false) {
      await mongoose.connection.close();
    }
  }
}

// Support CLI execution
if (require.main === module) {
  // Parse CLI args: e.g. --name="Dr. Sharma" --email="warden@hostelcare.com" --password="Password123!" --hostel="Tagore Hostel"
  const args = process.argv.slice(2);
  const parsedArgs = {};
  for (const arg of args) {
    if (arg.startsWith("--")) {
      const eqIdx = arg.indexOf("=");
      if (eqIdx !== -1) {
        const key = arg.slice(2, eqIdx);
        const val = arg.slice(eqIdx + 1);
        parsedArgs[key] = val;
      }
    }
  }

  const options = {
    name: parsedArgs.name,
    email: parsedArgs.email,
    password: parsedArgs.password,
    hostelName: parsedArgs.hostel || parsedArgs.hostelName,
    hostelBlock: parsedArgs.block || parsedArgs.hostelBlock,
    phone: parsedArgs.phone
  };

  provisionWarden(options)
    .then((result) => {
      process.exit(result.success ? 0 : 0);
    })
    .catch((err) => {
      console.error("[Provisioning Error]:", err.message);
      process.exit(1);
    });
}

module.exports = {
  provisionWarden,
  validatePassword
};
