/**
 * HostelCare Gemini AI Service
 * Primary AI analysis service using Google GenAI SDK with structured schema validation.
 */

const { GoogleGenAI, Type } = require('@google/genai');
const { validateAiOutput } = require('./aiValidation');

const DEFAULT_MODEL = "gemini-3.8-flash";
const REQUEST_TIMEOUT_MS = 15000;
const MAX_ATTEMPTS = 2; // Maximum 2 attempts (1 quick retry on 503/transient) to prevent blocking

function isConfigured() {
    return Boolean(process.env.GEMINI_API_KEY && process.env.GEMINI_API_KEY.trim());
}

function getModelName() {
    return (process.env.GEMINI_MODEL && process.env.GEMINI_MODEL.trim()) || DEFAULT_MODEL;
}

function stripMarkdownFences(text) {
    if (!text || typeof text !== 'string') return "";
    let cleaned = text.trim();
    if (cleaned.startsWith("```")) {
        cleaned = cleaned.replace(/^```(?:json)?\s*/i, "").replace(/\s*```$/, "");
    }
    return cleaned.trim();
}

/**
 * Analyzes a complaint using Google Gemini.
 * @param {string} title
 * @param {string} description
 * @param {string} [existingCategory]
 * @param {string} [imageUrl] - Optional base64 data URL
 * @returns {Promise<{ success: boolean, analysis?: { category: string, priority: string, summary: string }, provider?: string, error?: string, errorCategory?: string, status?: number, retryAfterMs?: number }>}
 */
async function analyzeComplaint(title, description, existingCategory, imageUrl) {
    if (!isConfigured()) {
        return {
            success: false,
            error: "Gemini API key is not configured",
            errorCategory: "unconfigured",
            status: 500
        };
    }

    const apiKey = process.env.GEMINI_API_KEY.trim();
    const modelName = getModelName();
    const ai = new GoogleGenAI({ apiKey });

    const promptText = `Analyze the following hostel maintenance complaint.
You must output a structured JSON response containing:
1. 'category' (must be exactly one of: plumbing, electrical, internet, cleaning, furniture, other). Use the existing category if it seems correct, otherwise correct it.
2. 'priority' (must be exactly one of: low, medium, high, emergency). Assess based on urgency and potential damage.
3. 'summary' (a concise 1-sentence summary of the issue).

Title: ${title}
Description: ${description}
Existing Category: ${existingCategory || "none"}

Respond ONLY with a valid JSON object matching:
{"category": "...", "priority": "...", "summary": "..."}`;

    // Multimodal parts if image is base64
    let contents = promptText;
    if (imageUrl && typeof imageUrl === 'string' && imageUrl.startsWith("data:")) {
        const match = imageUrl.match(/^data:([^;]+);base64,(.+)$/);
        if (match) {
            contents = [
                promptText,
                {
                    inlineData: {
                        mimeType: match[1],
                        data: match[2]
                    }
                }
            ];
        }
    }

    let attempts = 0;
    while (attempts < MAX_ATTEMPTS) {
        attempts++;
        const startTime = Date.now();
        try {
            const response = await Promise.race([
                ai.models.generateContent({
                    model: modelName,
                    contents: contents,
                    config: {
                        responseMimeType: "application/json",
                        responseSchema: {
                            type: Type.OBJECT,
                            properties: {
                                category: {
                                    type: Type.STRING,
                                    enum: ["plumbing", "electrical", "internet", "cleaning", "furniture", "other"]
                                },
                                priority: {
                                    type: Type.STRING,
                                    enum: ["low", "medium", "high", "emergency"]
                                },
                                summary: {
                                    type: Type.STRING,
                                    description: "A short 1-sentence summary of the complaint"
                                }
                            },
                            required: ["category", "priority", "summary"]
                        }
                    }
                }),
                new Promise((_, reject) =>
                    setTimeout(() => reject(new Error("Gemini request timed out")), REQUEST_TIMEOUT_MS)
                )
            ]);

            const elapsedMs = Date.now() - startTime;
            const rawText = response.text;

            if (!rawText) {
                return {
                    success: false,
                    error: "Empty completion response from Gemini",
                    errorCategory: "validation_error",
                    status: 502
                };
            }

            let parsedJson;
            try {
                parsedJson = JSON.parse(stripMarkdownFences(rawText));
            } catch (parseErr) {
                return {
                    success: false,
                    error: `Malformed JSON from Gemini: ${parseErr.message}`,
                    errorCategory: "validation_error",
                    status: 502
                };
            }

            const validation = validateAiOutput(parsedJson);
            if (!validation.valid) {
                return {
                    success: false,
                    error: `Gemini output failed schema validation: ${validation.error}`,
                    errorCategory: "validation_error",
                    status: 502
                };
            }

            console.info(`[Gemini] Completed successfully in ${elapsedMs}ms using model: ${modelName}`);

            return {
                success: true,
                analysis: validation.data,
                provider: "gemini"
            };

        } catch (error) {
            const elapsedMs = Date.now() - startTime;
            const statusCode = error.status || error.error?.code || (error.message?.includes("timed out") ? 504 : 500);
            const errMsg = error.message || error.error?.message || "Gemini service error";

            console.warn(`[Gemini] Call failed (Attempt ${attempts}/${MAX_ATTEMPTS}): status=${statusCode} in ${elapsedMs}ms: ${errMsg}`);

            // Quick retry once on 503 if attempts < MAX_ATTEMPTS
            if (statusCode === 503 && attempts < MAX_ATTEMPTS) {
                await new Promise(resolve => setTimeout(resolve, 1000));
                continue;
            }

            let errorCategory = "transient_error";
            if (statusCode === 429) {
                errorCategory = "quota_exceeded";
            } else if (statusCode === 401 || statusCode === 403 || /api key/i.test(errMsg)) {
                errorCategory = "auth_error";
            } else if (statusCode === 400 || statusCode === 404 || /not found/i.test(errMsg)) {
                errorCategory = "config_error";
            } else if (statusCode === 504 || errMsg.includes("timed out")) {
                errorCategory = "timeout";
            }

            return {
                success: false,
                error: errMsg,
                errorCategory,
                status: statusCode
            };
        }
    }
}

module.exports = {
    isConfigured,
    getModelName,
    analyzeComplaint
};