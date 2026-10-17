/**
 * hostelUtils.js
 *
 * Centralized utility for normalizing, matching, and building regexes
 * for hostel names and blocks across student, warden, and staff workflows.
 *
 * Handles variations such as:
 * - "BH1", "BH-1", "BH 1", "B.H. 1", "B-H-1"
 * - "Hostel A", "Hostel-A", "HostelA"
 * - Case-insensitive matching and flexible block scoping
 */

function escapeRegex(text) {
  if (typeof text !== "string") return "";
  return text.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&");
}

function normalizeHostelName(name) {
  if (!name || typeof name !== "string") return "";
  return name.trim().toLowerCase().replace(/[\s\-_.]+/g, "");
}

function buildHostelRegex(hostelName) {
  if (!hostelName || typeof hostelName !== "string") {
    return /^$/;
  }
  const clean = hostelName.trim();
  if (!clean) return /^$/;

  const alnumChars = clean.replace(/[^a-zA-Z0-9]/g, "").split("");
  if (alnumChars.length === 0) {
    return new RegExp(`^\\s*${escapeRegex(clean)}\\s*$`, "i");
  }

  const separatorPattern = "[\\s\\-_.]*";
  const regexString = "^\\s*" + alnumChars.map(c => escapeRegex(c)).join(separatorPattern) + "\\s*$";
  return new RegExp(regexString, "i");
}

function isHostelMatch(hostelA, hostelB) {
  if (!hostelA || !hostelB) return false;
  if (typeof hostelA !== "string" || typeof hostelB !== "string") return false;
  const normA = normalizeHostelName(hostelA);
  const normB = normalizeHostelName(hostelB);
  if (normA && normB && normA === normB) {
    return true;
  }
  return buildHostelRegex(hostelA).test(hostelB.trim()) || buildHostelRegex(hostelB).test(hostelA.trim());
}

function buildBlockFilter(wardenBlock) {
  if (!wardenBlock || !wardenBlock.trim() || wardenBlock.trim().toLowerCase() === "all") {
    return null;
  }
  const clean = wardenBlock.trim();
  const singleLetterMatch = clean.match(/\b([A-Za-z0-9])\b/);
  if (singleLetterMatch) {
    const letter = singleLetterMatch[1];
    return new RegExp(`(^|\\b|Block[- ]?)${escapeRegex(letter)}(\\b|$)`, "i");
  }
  return new RegExp(escapeRegex(clean), "i");
}

function checkBlockAuthorized(complaint, warden) {
  if (!warden || !warden.hostelBlock || !warden.hostelBlock.trim() || warden.hostelBlock.trim().toLowerCase() === "all") {
    return true;
  }
  const blockRegex = buildBlockFilter(warden.hostelBlock);
  if (!blockRegex) return true;
  return blockRegex.test(complaint.hostelBlock || "");
}

const MALE_HOSTELS = [
  "BH-1", "BH-2", "BH-3", "BH-4", "BH-5",
  "BH-6", "BH-7", "BH-8", "BH-9", "BH-10"
];

const FEMALE_HOSTELS = [
  "GH-1", "GH-2", "GH-3", "GH-4", "GH-5",
  "GH-6", "GH-7", "GH-8", "GH-9", "GH-10"
];

function getCanonicalHostelName(name) {
  if (!name || typeof name !== "string") return "";
  const clean = name.trim();
  if (!clean) return "";

  // Check direct matches first (case-insensitive)
  const maleMatch = MALE_HOSTELS.find(h => isHostelMatch(h, clean));
  if (maleMatch) return maleMatch;

  const femaleMatch = FEMALE_HOSTELS.find(h => isHostelMatch(h, clean));
  if (femaleMatch) return femaleMatch;

  // Pattern matching for Boys Hostel variations: e.g. "BH 1", "Boys Hostel 1"
  const bhMatch = clean.match(/^(?:B\.?H\.?|Boys?(?:\s+Hostel)?)[-\s.]*([1-9]|10)$/i);
  if (bhMatch) {
    return `BH-${bhMatch[1]}`;
  }

  // Pattern matching for Girls Hostel variations: e.g. "GH 1", "Girls Hostel 1"
  const ghMatch = clean.match(/^(?:G\.?H\.?|Girls?(?:\s+Hostel)?)[-\s.]*([1-9]|10)$/i);
  if (ghMatch) {
    return `GH-${ghMatch[1]}`;
  }

  return clean;
}

function isValidHostelForGender(gender, hostelName) {
  if (!gender || typeof gender !== "string") {
    return { valid: false, message: "Gender is required" };
  }
  if (!hostelName || typeof hostelName !== "string") {
    return { valid: false, message: "Hostel name is required" };
  }

  const cleanGender = gender.trim().toLowerCase();
  const canonical = getCanonicalHostelName(hostelName);

  if (cleanGender === "male") {
    const isMaleHostel = MALE_HOSTELS.includes(canonical);
    if (!isMaleHostel) {
      return {
        valid: false,
        message: `Invalid hostel for Male student. Allowed hostels are: ${MALE_HOSTELS.join(", ")}`
      };
    }
    return { valid: true, canonicalHostel: canonical };
  } else if (cleanGender === "female") {
    const isFemaleHostel = FEMALE_HOSTELS.includes(canonical);
    if (!isFemaleHostel) {
      return {
        valid: false,
        message: `Invalid hostel for Female student. Allowed hostels are: ${FEMALE_HOSTELS.join(", ")}`
      };
    }
    return { valid: true, canonicalHostel: canonical };
  }

  return { valid: false, message: "Gender must be either Male or Female" };
}

const ALL_CAMPUS_HOSTELS = [...MALE_HOSTELS, ...FEMALE_HOSTELS];

const CHIEF_WARDEN_EMAIL = (process.env.CHIEF_WARDEN_EMAIL || "warden@hostelcare.com").trim().toLowerCase();

/**
 * Validates whether an authenticated user is the designated Chief Warden with campus-wide authority.
 * Relies strictly on server-side model/environment properties, never trusting client parameters.
 */
function isChiefWardenUser(user) {
  if (!user || user.role !== "warden") return false;
  if (user.isChiefWarden === true) return true;
  if (user.email && user.email.trim().toLowerCase() === CHIEF_WARDEN_EMAIL) return true;
  return false;
}

module.exports = {
  escapeRegex,
  normalizeHostelName,
  buildHostelRegex,
  isHostelMatch,
  buildBlockFilter,
  checkBlockAuthorized,
  MALE_HOSTELS,
  FEMALE_HOSTELS,
  ALL_CAMPUS_HOSTELS,
  CHIEF_WARDEN_EMAIL,
  isChiefWardenUser,
  getCanonicalHostelName,
  isValidHostelForGender
};
