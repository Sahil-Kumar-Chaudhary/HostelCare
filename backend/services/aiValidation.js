/**
 * HostelCare AI Response Validation Module
 * Enforces strict, consistent output schema across all AI providers (Gemini & OpenRouter).
 */

const ALLOWED_CATEGORIES = Object.freeze([
    "plumbing",
    "electrical",
    "internet",
    "cleaning",
    "furniture",
    "other"
]);

const ALLOWED_PRIORITIES = Object.freeze([
    "low",
    "medium",
    "high",
    "emergency"
]);

/**
 * Validates and normalizes raw AI output.
 * @param {any} raw - Parsed JSON object from AI provider response.
 * @returns {{ valid: boolean, data?: { category: string, priority: string, summary: string }, error?: string }}
 */
function validateAiOutput(raw) {
    if (!raw || typeof raw !== 'object') {
        return { valid: false, error: "AI output is not a valid JSON object" };
    }

    const { category, priority, summary } = raw;

    if (!category || typeof category !== 'string') {
        return { valid: false, error: "AI response missing or invalid 'category' field" };
    }
    const normCategory = category.trim().toLowerCase();
    if (!ALLOWED_CATEGORIES.includes(normCategory)) {
        return { valid: false, error: `Invalid category '${normCategory}'. Allowed: ${ALLOWED_CATEGORIES.join(', ')}` };
    }

    if (!priority || typeof priority !== 'string') {
        return { valid: false, error: "AI response missing or invalid 'priority' field" };
    }
    const normPriority = priority.trim().toLowerCase();
    if (!ALLOWED_PRIORITIES.includes(normPriority)) {
        return { valid: false, error: `Invalid priority '${normPriority}'. Allowed: ${ALLOWED_PRIORITIES.join(', ')}` };
    }

    if (!summary || typeof summary !== 'string' || summary.trim().length === 0) {
        return { valid: false, error: "AI response missing or empty 'summary' field" };
    }
    const cleanSummary = summary.trim();
    if (cleanSummary.length > 500) {
        return { valid: false, error: "AI response summary exceeds 500 character limit" };
    }

    return {
        valid: true,
        data: {
            category: normCategory,
            priority: normPriority,
            summary: cleanSummary
        }
    };
}

module.exports = {
    ALLOWED_CATEGORIES,
    ALLOWED_PRIORITIES,
    validateAiOutput
};
