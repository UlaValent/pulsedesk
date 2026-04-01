package com.pulsedesk.pulsedesk.service;

import com.pulsedesk.pulsedesk.config.HuggingFaceProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class HuggingFaceService {

    private final RestClient restClient;
    private final HuggingFaceProperties properties;

    public HuggingFaceService(RestClient.Builder restClientBuilder, HuggingFaceProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    public String sendCommentText(String commentText) {
        return sendPrompt(commentText);
    }

    public String sendPrompt(String prompt) {
        validateConfiguration();

        String requestBody = "{\"inputs\":\"" + escapeJson(prompt) + "\"}";

        try {
            return restClient.post()
                    .uri(properties.getModelUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Hugging Face inference request failed with status " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString(),
                    ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Hugging Face inference request failed", ex);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("Hugging Face API key is not configured. Set HUGGINGFACE_API_KEY.");
        }
        if (!StringUtils.hasText(properties.getModelUrl())) {
            throw new IllegalStateException("Hugging Face model URL is not configured.");
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
