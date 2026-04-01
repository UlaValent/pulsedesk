package com.pulsedesk.pulsedesk.service;

import com.pulsedesk.pulsedesk.config.HuggingFaceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TriageServiceTests {

    @Test
    void parsesYesAnswerFromRawModelResponse() {
        TriageService service = new TriageService(new StubHuggingFaceService("[{\"generated_text\":\"yes\"}]"));

        boolean shouldCreateTicket = service.shouldCreateTicketFromResponse("[{\"generated_text\":\"yes\"}]");

        assertThat(shouldCreateTicket).isTrue();
    }

    @Test
    void parsesNoAnswerFromRawModelResponse() {
        TriageService service = new TriageService(new StubHuggingFaceService("[{\"generated_text\":\"no\"}]"));

        boolean shouldCreateTicket = service.shouldCreateTicketFromResponse("[{\"generated_text\":\"no\"}]");

        assertThat(shouldCreateTicket).isFalse();
    }

    @Test
    void callsHuggingFaceClientWhenTriagingCommentText() {
        StubHuggingFaceService stub = new StubHuggingFaceService("[{\"generated_text\":\"yes\"}]");
        TriageService service = new TriageService(stub);

        boolean shouldCreateTicket = service.shouldCreateTicket("The app crashes when I log in.");

        assertThat(shouldCreateTicket).isTrue();
        assertThat(stub.lastPrompt).contains("Does this comment report a problem or request a feature? Answer yes or no.");
        assertThat(stub.lastPrompt).contains("The app crashes when I log in.");
    }

    @Test
    void rejectsUnclearAnswers() {
        TriageService service = new TriageService(new StubHuggingFaceService("[{\"generated_text\":\"maybe\"}]"));

        assertThatThrownBy(() -> service.shouldCreateTicketFromResponse("[{\"generated_text\":\"maybe\"}]"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to determine a yes/no triage decision");
    }

    private static final class StubHuggingFaceService extends HuggingFaceService {

        private final String response;
        private String lastPrompt;

        private StubHuggingFaceService(String response) {
            super(RestClient.builder(), new HuggingFaceProperties());
            this.response = response;
        }

        @Override
        public String sendPrompt(String prompt) {
            this.lastPrompt = prompt;
            return response;
        }
    }
}