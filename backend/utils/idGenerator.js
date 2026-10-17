/**
 * idGenerator.js
 *
 * Collision-safe ID and temporary credential generation utilities for
 * Warden and Staff account provisioning.
 */
const crypto = require("crypto");

/**
 * Generate a collision-safe sequential Warden ID in the format WRD-0001, WRD-0002...
 * @param {import("mongoose").Model} UserModel - Mongoose User model
 * @returns {Promise<string>} Unique Warden ID
 */
async function generateUniqueWardenId(UserModel) {
  if (!UserModel) {
    throw new Error("UserModel is required to generate a unique Warden ID");
  }

  // Retrieve all existing warden IDs
  let wardens = [];
  try {
    const query = UserModel.find(
      { role: "warden", wardenId: { $exists: true, $ne: null } },
      "wardenId"
    );
    wardens = (query && typeof query.lean === "function") ? await query.lean() : await query;
  } catch (err) {
    wardens = [];
  }

  let maxNum = 0;
  if (Array.isArray(wardens)) {
    for (const w of wardens) {
      if (w && w.wardenId) {
        const match = w.wardenId.match(/^WRD-(\d+)$/i);
        if (match) {
          const num = parseInt(match[1], 10);
          if (!isNaN(num) && num > maxNum) {
            maxNum = num;
          }
        }
      }
    }
  }

  let candidateNum = maxNum + 1;
  let candidateId = `WRD-${String(candidateNum).padStart(4, "0")}`;

  // Collision safety check
  const checkExists = async (id) => {
    try {
      const res = UserModel.findOne({ wardenId: id });
      if (res && typeof res.select === "function") {
        return await res.select("_id").lean();
      }
      return await res;
    } catch (err) {
      return null;
    }
  };

  while (await checkExists(candidateId)) {
    candidateNum++;
    candidateId = `WRD-${String(candidateNum).padStart(4, "0")}`;
  }

  return candidateId;
}

/**
 * Generate a collision-safe sequential Staff ID in the format STF-1001, STF-1002...
 * @param {import("mongoose").Model} UserModel - Mongoose User model
 * @returns {Promise<string>} Unique Staff ID
 */
async function generateUniqueStaffId(UserModel) {
  if (!UserModel) {
    throw new Error("UserModel is required to generate a unique Staff ID");
  }

  let staffList = [];
  try {
    const query = UserModel.find(
      { role: "staff", staffId: { $exists: true, $ne: null } },
      "staffId"
    );
    staffList = (query && typeof query.lean === "function") ? await query.lean() : await query;
  } catch (err) {
    staffList = [];
  }

  let maxNum = 1000;
  if (Array.isArray(staffList)) {
    for (const s of staffList) {
      if (s && s.staffId) {
        const match = s.staffId.match(/^STF-(\d+)$/i);
        if (match) {
          const num = parseInt(match[1], 10);
          if (!isNaN(num) && num > maxNum) {
            maxNum = num;
          }
        }
      }
    }
  }

  let candidateNum = maxNum + 1;
  let candidateId = `STF-${candidateNum}`;

  const checkStaffExists = async (id) => {
    try {
      const res = UserModel.findOne({ staffId: id });
      if (res && typeof res.select === "function") {
        return await res.select("_id").lean();
      }
      return await res;
    } catch (err) {
      return null;
    }
  };

  while (await checkStaffExists(candidateId)) {
    candidateNum++;
    candidateId = `STF-${candidateNum}`;
  }

  return candidateId;
}

/**
 * Generate a secure temporary password satisfying password policy:
 * - 12+ characters
 * - Uppercase, lowercase, number, and special character
 * @param {number} length - Desired length (default 12)
 * @returns {string} Temporary password
 */
function generateSecureTemporaryPassword(length = 12) {
  const uppers = "ABCDEFGHJKLMNPQRSTUVWXYZ";
  const lowers = "abcdefghijkmnopqrstuvwxyz";
  const numbers = "23456789";
  const specials = "!@#$%^&*";

  // Ensure at least one of each category
  const passChars = [
    uppers[crypto.randomInt(0, uppers.length)],
    lowers[crypto.randomInt(0, lowers.length)],
    numbers[crypto.randomInt(0, numbers.length)],
    specials[crypto.randomInt(0, specials.length)],
  ];

  const allChars = uppers + lowers + numbers + specials;
  for (let i = passChars.length; i < length; i++) {
    passChars.push(allChars[crypto.randomInt(0, allChars.length)]);
  }

  // Shuffle the characters
  for (let i = passChars.length - 1; i > 0; i--) {
    const j = crypto.randomInt(0, i + 1);
    [passChars[i], passChars[j]] = [passChars[j], passChars[i]];
  }

  return passChars.join("");
}

module.exports = {
  generateUniqueWardenId,
  generateUniqueStaffId,
  generateSecureTemporaryPassword
};
