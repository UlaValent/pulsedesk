package com.pulsedesk.pulsedesk.service;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class TriageService {

    private final HuggingFaceService huggingFaceService;

    public TriageService(HuggingFaceService huggingFaceService) {
        this.huggingFaceService = huggingFaceService;
    }

    public boolean shouldCreateTicket(String commentText) {
        String prompt = buildPrompt(commentText);
        String rawResponse = huggingFaceService.sendPrompt(prompt);
        return shouldCreateTicketFromResponse(rawResponse);
    }

    public boolean shouldCreateTicketFromResponse(String huggingFaceResponse) {
        String answer = normalizeAnswer(extractGeneratedText(huggingFaceResponse));

        if (answer.startsWith("yes")) {
            return true;
        }
        if (answer.startsWith("no")) {
            return false;
        }
        if (answer.contains(" yes ") || answer.endsWith(" yes") || answer.startsWith("yes ")) {
            return true;
        }
        if (answer.contains(" no ") || answer.endsWith(" no") || answer.startsWith("no ")) {
            return false;
        }

        throw new IllegalStateException("Unable to determine a yes/no triage decision from Hugging Face response.");
    }

    private String buildPrompt(String commentText) {
        return "Does this comment report a problem or request a feature? Answer yes or no. Comment: \""
                + escapePrompt(commentText)
                + "\"";
    }

    private String extractGeneratedText(String rawResponse) {
        if (rawResponse == null) {
            return "";
        }

        int generatedTextIndex = rawResponse.indexOf("generated_text");
        if (generatedTextIndex < 0) {
            return rawResponse;
        }

        int firstQuote = rawResponse.indexOf('"', generatedTextIndex + "generated_text".length());
        if (firstQuote < 0) {
            return rawResponse;
        }

        firstQuote = rawResponse.indexOf('"', firstQuote + 1);
        if (firstQuote < 0) {
            return rawResponse;
        }

        int secondQuote = firstQuote + 1;
        StringBuilder text = new StringBuilder();
        boolean escaped = false;
        for (int index = secondQuote; index < rawResponse.length(); index++) {
            char current = rawResponse.charAt(index);
            if (escaped) {
                text.append(current);
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                return text.toString();
            }
            text.append(current);
        }

        return rawResponse;
    }

    private String normalizeAnswer(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace(".", "")
                .replace(",", "")
                .replace("!", "")
                .replace("?", "");
    }

    private String escapePrompt(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}