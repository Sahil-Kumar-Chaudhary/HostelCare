/**
 * HostelCare AI Provider Manager
 * Automatic Server-Side Failover between Google Gemini (primary) and OpenRouter (fallback).
 * Manages in-memory bounded cooldowns, health status, and uniform response delivery.
 */

const geminiService = require('./geminiService');
const openrouterService = require('./openrouterService');

// Default Cooldown Durations in milliseconds
const COOLDOWNS = {
    QUOTA_EXCEEDED: 60 * 1000,    // 60 seconds for HTTP 429
    TRANSIENT_ERROR: 30 * 1000,   // 30 seconds for HTTP 503 / 500
    TIMEOUT: 30 * 1000,           // 30 seconds for timeouts
    AUTH_OR_CONFIG: 5 * 60 * 1000,// 5 minutes for invalid keys/models
    VALIDATION_ERROR: 15 * 1000   // 15 seconds for malformed JSON
};

// In-memory provider state
const providerState = {
    gemini: {
        consecutiveFailures: 0,
        cooldownUntil: 0,
        lastFailureReason: null,
        lastFailureCategory: null,
        lastSuccessTime: null,
        totalRequests: 0,
        totalSuccesses: 0,
        totalFailures: 0
    },
    openrouter: {
        consecutiveFailures: 0,
        cooldownUntil: 0,
        lastFailureReason: null,
        lastFailureCategory: null,
        lastSuccessTime: null,
        totalRequests: 0,
        totalSuccesses: 0,
        totalFailures: 0
    }
};

/**
 * Checks if a provider is currently cooling down.
 * @param {'gemini'|'openrouter'} provider
 * @returns {boolean}
 */
function isCoolingDown(provider) {
    const state = providerState[provider];
    if (!state) return false;
    return Date.now() < state.cooldownUntil;
}

/**
 * Applies a cooldown to a provider based on error category and optional retryAfter.
 * @param {'gemini'|'openrouter'} provider
 * @param {string} errorCategory
 * @param {string} errorMessage
 * @param {number} [retryAfterMs]
 */
function recordFailure(provider, errorCategory, errorMessage, retryAfterMs) {
    const state = providerState[provider];
    if (!state) return;

    state.consecutiveFailures++;
    state.totalFailures++;
    state.totalRequests++;
    state.lastFailureReason = errorMessage;
    state.lastFailureCategory = errorCategory;

    let durationMs = COOLDOWNS.TRANSIENT_ERROR;

    if (retryAfterMs && retryAfterMs > 0) {
        durationMs = Math.min(retryAfterMs, 5 * 60 * 1000); // Bound at 5 minutes
    } else {
        switch (errorCategory) {
            case 'quota_exceeded':
                // Progressive backoff: 60s, 120s, max 300s
                durationMs = Math.min(COOLDOWNS.QUOTA_EXCEEDED * Math.max(1, state.consecutiveFailures), 300 * 1000);
                break;
            case 'auth_error':
            case 'config_error':
                durationMs = COOLDOWNS.AUTH_OR_CONFIG;
                break;
            case 'timeout':
                durationMs = COOLDOWNS.TIMEOUT;
                break;
            case 'validation_error':
                durationMs = COOLDOWNS.VALIDATION_ERROR;
                break;
            case 'transient_error':
            default:
                durationMs = Math.min(COOLDOWNS.TRANSIENT_ERROR * Math.max(1, state.consecutiveFailures), 120 * 1000);
                break;
        }
    }

    state.cooldownUntil = Date.now() + durationMs;
    console.warn(`[AI Provider Manager] ${provider} entered cooldown for ${Math.round(durationMs / 1000)}s due to [${errorCategory}]: ${errorMessage}`);
}

/**
 * Records a successful invocation for a provider, clearing cooldown.
 * @param {'gemini'|'openrouter'} provider
 */
function recordSuccess(provider) {
    const state = providerState[provider];
    if (!state) return;

    state.consecutiveFailures = 0;
    state.cooldownUntil = 0;
    state.lastFailureReason = null;
    state.lastFailureCategory = null;
    state.lastSuccessTime = Date.now();
    state.totalRequests++;
    state.totalSuccesses++;
}

/**
 * Resets all provider cooldowns and counters (used in testing and diagnostics).
 */
function resetCooldowns() {
    for (const key of ['gemini', 'openrouter']) {
        providerState[key].consecutiveFailures = 0;
        providerState[key].cooldownUntil = 0;
        providerState[key].lastFailureReason = null;
        providerState[key].lastFailureCategory = null;
    }
}

/**
 * Force a provider into cooldown for testing purposes.
 * @param {'gemini'|'openrouter'} provider
 * @param {number} durationMs
 * @param {string} reason
 */
function forceCooldown(provider, durationMs, reason = "Forced cooldown") {
    if (providerState[provider]) {
        providerState[provider].cooldownUntil = Date.now() + durationMs;
        providerState[provider].lastFailureReason = reason;
        providerState[provider].lastFailureCategory = "forced_cooldown";
    }
}

/**
 * Gets the current status of all AI providers.
 * @returns {object}
 */
function getProviderStatus() {
    const now = Date.now();
    return {
        gemini: {
            configured: geminiService.isConfigured(),
            model: geminiService.getModelName(),
            coolingDown: isCoolingDown('gemini'),
            cooldownRemainingSec: Math.max(0, Math.round((providerState.gemini.cooldownUntil - now) / 1000)),
            consecutiveFailures: providerState.gemini.consecutiveFailures,
            lastFailureReason: providerState.gemini.lastFailureReason,
            totalRequests: providerState.gemini.totalRequests,
            totalSuccesses: providerState.gemini.totalSuccesses,
            totalFailures: providerState.gemini.totalFailures
        },
        openrouter: {
            configured: openrouterService.isConfigured(),
            model: openrouterService.getModelName(),
            coolingDown: isCoolingDown('openrouter'),
            cooldownRemainingSec: Math.max(0, Math.round((providerState.openrouter.cooldownUntil - now) / 1000)),
            consecutiveFailures: providerState.openrouter.consecutiveFailures,
            lastFailureReason: providerState.openrouter.lastFailureReason,
            totalRequests: providerState.openrouter.totalRequests,
            totalSuccesses: providerState.openrouter.totalSuccesses,
            totalFailures: providerState.openrouter.totalFailures
        }
    };
}

/**
 * Executes AI complaint analysis with automatic transparent failover.
 * Primary: Google Gemini -> Secondary: OpenRouter.
 *
 * @param {string} title
 * @param {string} description
 * @param {string} [existingCategory]
 * @param {string} [imageUrl]
 * @returns {Promise<{ success: boolean, analysis?: { category: string, priority: string, summary: string }, provider?: string, failedOver?: boolean, error?: string, status?: number }>}
 */
async function analyzeComplaint(title, description, existingCategory, imageUrl) {
    const geminiConfigured = geminiService.isConfigured();
    const openrouterConfigured = openrouterService.isConfigured();

    if (!geminiConfigured && !openrouterConfigured) {
        return {
            success: false,
            error: "No AI provider is configured on the server",
            status: 503
        };
    }

    let geminiErrorResult = null;

    // 1. Check if Gemini can be attempted
    if (geminiConfigured && !isCoolingDown('gemini')) {
        console.info("[AI Provider Manager] Invoking primary provider: Gemini...");
        const result = await geminiService.analyzeComplaint(title, description, existingCategory, imageUrl);

        if (result.success) {
            recordSuccess('gemini');
            return {
                success: true,
                analysis: result.analysis,
                provider: "gemini",
                failedOver: false
            };
        }

        // Gemini failed
        geminiErrorResult = result;
        recordFailure('gemini', result.errorCategory, result.error, result.retryAfterMs);
        console.warn(`[AI Provider Manager] Gemini failed with ${result.errorCategory} (status ${result.status}).`);
    } else if (geminiConfigured && isCoolingDown('gemini')) {
        const remaining = Math.round((providerState.gemini.cooldownUntil - Date.now()) / 1000);
        console.info(`[AI Provider Manager] Gemini is in cooldown (${remaining}s remaining). Skipping to secondary provider.`);
    }

    // 2. Failover to OpenRouter
    if (openrouterConfigured && !isCoolingDown('openrouter')) {
        console.info("[AI Provider Manager] Invoking secondary provider: OpenRouter (failover)...");
        const result = await openrouterService.analyzeComplaint(title, description, existingCategory, imageUrl);

        if (result.success) {
            recordSuccess('openrouter');
            return {
                success: true,
                analysis: result.analysis,
                provider: "openrouter",
                failedOver: true
            };
        }

        // OpenRouter also failed
        recordFailure('openrouter', result.errorCategory, result.error, result.retryAfterMs);
        console.warn(`[AI Provider Manager] OpenRouter also failed with ${result.errorCategory} (status ${result.status}).`);

        return {
            success: false,
            error: "AI analysis is currently unavailable. All AI providers failed.",
            details: {
                gemini: geminiErrorResult ? geminiErrorResult.error : (isCoolingDown('gemini') ? "In cooldown" : "Not attempted"),
                openrouter: result.error
            },
            status: 503
        };
    }

    // 3. If both providers are in cooldown or unconfigured
    if (geminiConfigured && isCoolingDown('gemini') && openrouterConfigured && isCoolingDown('openrouter')) {
        console.warn("[AI Provider Manager] All configured providers are currently in cooldown.");
        return {
            success: false,
            error: "AI analysis is temporarily unavailable due to rate limits across all providers. Please try again shortly.",
            status: 503
        };
    }

    // If Gemini was unconfigured and OpenRouter is in cooldown or failed
    return {
        success: false,
        error: geminiErrorResult ? geminiErrorResult.error : "AI analysis is currently unavailable.",
        status: 503
    };
}

module.exports = {
    analyzeComplaint,
    getProviderStatus,
    resetCooldowns,
    forceCooldown,
    isCoolingDown,
    COOLDOWNS
};
