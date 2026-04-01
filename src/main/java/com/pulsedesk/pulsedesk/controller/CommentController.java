package com.pulsedesk.pulsedesk.controller;

import com.pulsedesk.pulsedesk.dto.CommentRequest;
import com.pulsedesk.pulsedesk.model.Comment;
import com.pulsedesk.pulsedesk.repository.CommentRepository;
import com.pulsedesk.pulsedesk.service.TriageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentRepository commentRepository;
    private final TriageService triageService;

    @PostMapping
    public ResponseEntity<Comment> createComment(@Valid @RequestBody CommentRequest request) {
        Comment comment = new Comment();
        comment.setText(request.getText());
        comment.setSource(request.getSource());

        Comment savedComment = commentRepository.save(comment);
        triageService.shouldCreateTicket(savedComment.getText());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedComment);
    }

    @GetMapping
    public ResponseEntity<List<Comment>> getAllComments() {
        List<Comment> comments = commentRepository.findAll();
        return ResponseEntity.ok(comments);
    }
}