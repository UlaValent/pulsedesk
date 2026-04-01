package com.pulsedesk.pulsedesk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TriageService {

    private static final Logger logger = LoggerFactory.getLogger(TriageService.class);
    private static final Pattern YES_OR_NO_PATTERN = Pattern.compile("\\b(yes|no)\\b");

    private final HuggingFaceService huggingFaceService;

    public TriageService(HuggingFaceService huggingFaceService) {
        this.huggingFaceService = huggingFaceService;
    }

    public boolean shouldCreateTicket(String commentText) {
        String prompt = buildPrompt(commentText);
        String rawResponse = huggingFaceService.sendPrompt(prompt);
        try {
            return shouldCreateTicketFromResponse(rawResponse);
        } catch (IllegalStateException exception) {
            logger.warn("Triage response was unclear, defaulting to no ticket creation. Response: {}", rawResponse);
            return false;
        }
    }

    public boolean shouldCreateTicketFromResponse(String huggingFaceResponse) {
        String answer = normalizeAnswer(extractDecisionText(huggingFaceResponse));
        Matcher matcher = YES_OR_NO_PATTERN.matcher(answer);
        Boolean decision = null;
        while (matcher.find()) {
            boolean currentDecision = "yes".equals(matcher.group(1));
            if (decision == null) {
                decision = currentDecision;
                continue;
            }
            if (decision != currentDecision) {
                throw new IllegalStateException("Unable to determine a yes/no triage decision from Hugging Face response.");
            }
        }

        if (decision != null) {
            return decision;
        }

        throw new IllegalStateException("Unable to determine a yes/no triage decision from Hugging Face response.");
    }

    private String buildPrompt(String commentText) {
        return "Does this comment report a problem or request a feature? Answer yes or no. Comment: \""
                + escapePrompt(commentText)
                + "\"";
    }

    private String extractDecisionText(String rawResponse) {
        if (rawResponse == null) {
            return "";
        }

        String generatedText = extractFieldValue(rawResponse, "generated_text");
        if (!generatedText.isBlank()) {
            return generatedText;
        }

        String label = extractFieldValue(rawResponse, "label");
        if (!label.isBlank()) {
            return label;
        }

        return rawResponse;
    }

    private String extractFieldValue(String rawResponse, String fieldName) {
        int fieldIndex = rawResponse.indexOf("\"" + fieldName + "\"");
        if (fieldIndex < 0) {
            return "";
        }

        int colonIndex = rawResponse.indexOf(':', fieldIndex);
        if (colonIndex < 0) {
            return "";
        }

        int firstQuote = rawResponse.indexOf('"', colonIndex);
        if (firstQuote < 0) {
            return "";
        }

        int textStart = firstQuote + 1;
        StringBuilder text = new StringBuilder();
        boolean escaped = false;
        for (int index = textStart; index < rawResponse.length(); index++) {
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

        return "";
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