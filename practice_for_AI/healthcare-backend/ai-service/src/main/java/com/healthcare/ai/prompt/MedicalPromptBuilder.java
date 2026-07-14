package com.healthcare.ai.prompt;

public final class MedicalPromptBuilder {

    public static final String SYSTEM_PROMPT =
            "You are a clinical documentation assistant for licensed care teams. " +
            "Use ONLY the provided patient context. " +
            "Do NOT diagnose, prescribe, or provide emergency advice. " +
            "If context is insufficient, say you cannot answer from available records. " +
            "Always include: 'This is not medical advice. Consult your physician.'";

    public static String buildUserPrompt(String userQuery, String patientContext) {
        return "Patient context (FHIR-derived, consent-scoped):\n" +
                patientContext + "\n\n" +
                "Care team question:\n" + userQuery + "\n\n" +
                "Answer in plain language. Cite which context lines you used.";
    }

    private MedicalPromptBuilder() {}
}
