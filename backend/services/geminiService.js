const { GoogleGenAI, Type } = require('@google/genai');

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

async function analyzeComplaint(title, description, existingCategory, imageUrl) {
    let attempts = 0;
    while (attempts < 10) {
        try {
            attempts++;
            const modelName = process.env.GEMINI_MODEL || "gemini-3.8-flash";
            
            const prompt = "Analyze the following hostel maintenance complaint. \nYou must output a structured JSON response containing:\n1. 'category' (must be exactly one of: plumbing, electrical, internet, cleaning, furniture, other). Use the existing category if it seems correct, otherwise correct it.\n2. 'priority' (must be exactly one of: low, medium, high, emergency). Assess based on urgency and potential damage.\n3. 'summary' (a concise 1-sentence summary of the issue).\n\nTitle: " + title + "\nDescription: " + description + "\nExisting Category: " + (existingCategory || "none");

            const response = await ai.models.generateContent({
                model: modelName,
                contents: prompt,
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
            });

            // Parse and return the JSON
            const rawText = response.text;
            const result = JSON.parse(rawText);
            
            return {
                success: true,
                analysis: {
                    category: result.category,
                    priority: result.priority,
                    summary: result.summary
                }
            };
        } catch (error) {
            console.error('Gemini API error (Attempt ' + attempts + '):');
            console.error('status=' + (error.status || error.error?.code || 'unknown'));
            console.error('message=' + (error.message || error.error?.message || 'unknown error'));
            
            if (error.status === 503 && attempts < 10) {
                // Wait 2 seconds before retrying
                await new Promise(resolve => setTimeout(resolve, 2000));
                continue;
            }
            
            return {
                success: false,
                error: error.message || "Failed to analyze complaint"
            };
        }
    }
}

module.exports = { analyzeComplaint };