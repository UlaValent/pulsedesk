package com.pulsedesk.pulsedesk.service;

import com.pulsedesk.pulsedesk.config.HuggingFaceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class HuggingFaceServiceTests {

    private HuggingFaceProperties properties;
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private HuggingFaceService service;

    @BeforeEach
    void setUp() {
        properties = new HuggingFaceProperties();
        properties.setApiKey("test-api-key");
        properties.setModelUrl("https://api-inference.huggingface.co/models/test-model");

        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        service = new HuggingFaceService(restClientBuilder, properties);
    }

    @Test
    void sendsCommentTextAndReturnsRawResponse() {
        mockServer.expect(requestTo(properties.getModelUrl()))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andRespond(withSuccess("[{\"generated_text\":\"raw model payload\"}]", MediaType.APPLICATION_JSON));

        String response = service.sendCommentText("The app keeps crashing on login.");

        assertThat(response).isEqualTo("[{\"generated_text\":\"raw model payload\"}]");
        mockServer.verify();
    }

    @Test
    void throwsClearErrorWhenApiKeyMissing() {
        properties.setApiKey(" ");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.sendCommentText("Hello"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Hugging Face API key is not configured");
    }
}
