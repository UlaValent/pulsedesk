package com.pulsedesk.pulsedesk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequest {

    @NotBlank(message = "Comment text must not be blank")
    private String text;

    private String source;
}
