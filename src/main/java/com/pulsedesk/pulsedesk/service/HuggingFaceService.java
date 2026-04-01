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

    private static final String LEGACY_HF_HOST = "https://api-inference.huggingface.co";
    private static final String MODEL_PAGE_HF_HOST = "https://huggingface.co";
    private static final String ROUTER_HF_HOST = "https://router.huggingface.co";

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
        String modelUrl = resolveModelUrl(properties.getModelUrl());

        String requestBody = "{\"inputs\":\"" + escapeJson(prompt) + "\"}";

        try {
            return restClient.post()
                .uri(modelUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 410 && ex.getResponseBodyAsString().contains("router.huggingface.co")) {
            throw new IllegalStateException(
                "Hugging Face endpoint is deprecated. Set HUGGINGFACE_MODEL_URL to a router endpoint, " +
                    "for example https://router.huggingface.co/hf-inference/models/google/flan-t5-large.",
                ex);
            }
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

    private String resolveModelUrl(String configuredUrl) {
        String trimmed = configuredUrl.trim();
        if (trimmed.startsWith(ROUTER_HF_HOST)) {
            return trimmed;
        }

        if (trimmed.startsWith(LEGACY_HF_HOST)) {
            String path = trimmed.substring(LEGACY_HF_HOST.length());
            if (path.startsWith("/models/")) {
                return ROUTER_HF_HOST + "/hf-inference" + path;
            }
            return ROUTER_HF_HOST + path;
        }

        if (trimmed.startsWith(MODEL_PAGE_HF_HOST)) {
            String path = trimmed.substring(MODEL_PAGE_HF_HOST.length());
            if (path.startsWith("/models/")) {
                path = path.substring("/models".length());
            }
            if (path.startsWith("/")) {
                return ROUTER_HF_HOST + "/hf-inference/models" + path;
            }
        }

        return ROUTER_HF_HOST + "/hf-inference/models/" + trimmed;
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
