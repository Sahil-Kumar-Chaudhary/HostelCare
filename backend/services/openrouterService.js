/**
 * HostelCare OpenRouter AI Service
 * Modular client for OpenRouter Chat Completions API with multimodal vision support.
 */

const { validateAiOutput } = require('./aiValidation');

const OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";
const DEFAULT_MODEL = "openai/gpt-4o-mini";
const REQUEST_TIMEOUT_MS = 15000;

/**
 * Checks whether OpenRouter is configured in environment.
 * @returns {boolean}
 */
function isConfigured() {
    return Boolean(process.env.OPENROUTER_API_KEY && process.env.OPENROUTER_API_KEY.trim());
}

/**
 * Gets configured OpenRouter model name.
 * @returns {string}
 */
function getModelName() {
    return (process.env.OPENROUTER_MODEL && process.env.OPENROUTER_MODEL.trim()) || DEFAULT_MODEL;
}

/**
 * Strips markdown code fences if model wrapped JSON in ```json ... ```
 * @param {string} text
 * @returns {string}
 */
function stripMarkdownFences(text) {
    if (!text || typeof text !== 'string') return "";
    let cleaned = text.trim();
    if (cleaned.startsWith("```")) {
        cleaned = cleaned.replace(/^```(?:json)?\s*/i, "").replace(/\s*```$/, "");
    }
    return cleaned.trim();
}

/**
 * Analyzes a complaint using OpenRouter.
 * @param {string} title
 * @param {string} description
 * @param {string} [existingCategory]
 * @param {string} [imageUrl] - Optional data URL or HTTP image URL
 * @returns {Promise<{ success: boolean, analysis?: { category: string, priority: string, summary: string }, provider?: string, error?: string, errorCategory?: string, status?: number, retryAfterMs?: number }>}
 */
async function analyzeComplaint(title, description, existingCategory, imageUrl) {
    if (!isConfigured()) {
        return {
            success: false,
            error: "OpenRouter API key is not configured",
            errorCategory: "unconfigured",
            status: 500
        };
    }

    const apiKey = process.env.OPENROUTER_API_KEY.trim();
    const model = getModelName();

    const promptText = `Analyze the following hostel maintenance complaint.
You must output a structured JSON response containing exactly these fields:
1. "category": must be exactly one of: plumbing, electrical, internet, cleaning, furniture, other. If the existing category seems appropriate, keep it; otherwise select the best fit.
2. "priority": must be exactly one of: low, medium, high, emergency. Assess based on urgency, health/safety, and potential structural damage.
3. "summary": a concise 1-sentence summary of the issue.

Title: ${title}
Description: ${description}
Existing Category: ${existingCategory || "none"}

Respond ONLY with a valid JSON object matching:
{"category": "...", "priority": "...", "summary": "..."}`;

    // Construct message content (text or multimodal)
    let userContent;
    if (imageUrl && typeof imageUrl === 'string' && (imageUrl.startsWith("data:image/") || imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
        userContent = [
            { type: "text", text: promptText },
            {
                type: "image_url",
                image_url: {
                    url: imageUrl
                }
            }
        ];
    } else {
        userContent = promptText;
    }

    const payload = {
        model: model,
        messages: [
            {
                role: "system",
                content: "You are an expert hostel facility maintenance coordinator. Output strictly valid JSON."
            },
            {
                role: "user",
                content: userContent
            }
        ],
        response_format: { type: "json_object" },
        temperature: 0.2,
        max_tokens: 300
    };

    const startTime = Date.now();
    try {
        const response = await fetch(OPENROUTER_ENDPOINT, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${apiKey}`,
                "HTTP-Referer": "https://github.com/Sahil-Kumar-Chaudhary/HostelCare",
                "X-Title": "HostelCare"
            },
            body: JSON.stringify(payload),
            signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS)
        });

        const elapsedMs = Date.now() - startTime;
        const statusCode = response.status;

        // Parse retry-after header if present
        let retryAfterMs = null;
        const retryAfterHeader = response.headers.get("retry-after");
        if (retryAfterHeader) {
            const parsedSeconds = parseInt(retryAfterHeader, 10);
            if (!isNaN(parsedSeconds) && parsedSeconds > 0) {
                retryAfterMs = parsedSeconds * 1000;
            }
        }

        if (!response.ok) {
            const errorBody = await response.text().catch(() => "");
            let errorCategory = "transient_error";

            if (statusCode === 429) {
                errorCategory = "quota_exceeded";
            } else if (statusCode === 401 || statusCode === 403) {
                errorCategory = "auth_error";
            } else if (statusCode === 400 || statusCode === 404) {
                errorCategory = "config_error";
            }

            console.warn(`[OpenRouter] Call failed: HTTP ${statusCode} (${errorCategory}) in ${elapsedMs}ms`);

            return {
                success: false,
                error: `OpenRouter HTTP ${statusCode}: ${errorBody.slice(0, 200)}`,
                errorCategory,
                status: statusCode,
                retryAfterMs
            };
        }

        const data = await response.json();
        const contentText = data.choices?.[0]?.message?.content;

        if (!contentText) {
            return {
                success: false,
                error: "Empty completion response from OpenRouter",
                errorCategory: "validation_error",
                status: 502
            };
        }

        let parsedJson;
        try {
            parsedJson = JSON.parse(stripMarkdownFences(contentText));
        } catch (parseErr) {
            return {
                success: false,
                error: `Malformed JSON from OpenRouter: ${parseErr.message}`,
                errorCategory: "validation_error",
                status: 502
            };
        }

        const validation = validateAiOutput(parsedJson);
        if (!validation.valid) {
            return {
                success: false,
                error: `OpenRouter output failed schema validation: ${validation.error}`,
                errorCategory: "validation_error",
                status: 502
            };
        }

        console.info(`[OpenRouter] Completed successfully in ${elapsedMs}ms using model: ${model}`);

        return {
            success: true,
            analysis: validation.data,
            provider: "openrouter"
        };

    } catch (err) {
        const elapsedMs = Date.now() - startTime;
        let errorCategory = "transient_error";

        if (err.name === "TimeoutError" || err.name === "AbortError") {
            errorCategory = "timeout";
            console.warn(`[OpenRouter] Request timed out after ${REQUEST_TIMEOUT_MS}ms`);
        } else {
            errorCategory = "network_error";
            console.warn(`[OpenRouter] Network failure in ${elapsedMs}ms: ${err.message}`);
        }

        return {
            success: false,
            error: err.message || "OpenRouter connection failure",
            errorCategory,
            status: errorCategory === "timeout" ? 504 : 503
        };
    }
}

module.exports = {
    isConfigured,
    getModelName,
    analyzeComplaint
};
