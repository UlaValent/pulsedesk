package com.pulsedesk.pulsedesk.service;

import com.pulsedesk.pulsedesk.dto.TicketResponse;
import com.pulsedesk.pulsedesk.model.Comment;
import com.pulsedesk.pulsedesk.model.Ticket;
import com.pulsedesk.pulsedesk.model.enums.Category;
import com.pulsedesk.pulsedesk.model.enums.Priority;
import com.pulsedesk.pulsedesk.repository.TicketRepository;
import org.springframework.stereotype.Service;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public Ticket createTicketForComment(Comment comment) {
        Ticket ticket = new Ticket();
        ticket.setCommentId(comment.getId());
        ticket.setTitle(buildTitle(comment.getText()));
        ticket.setCategory(Category.OTHER);
        ticket.setPriority(Priority.MEDIUM);
        ticket.setSummary(buildSummary(comment.getText()));
        return ticketRepository.save(ticket);
    }

    public TicketResponse toResponse(Ticket ticket) {
        TicketResponse response = new TicketResponse();
        response.setId(ticket.getId());
        response.setCommentId(ticket.getCommentId());
        response.setTitle(ticket.getTitle());
        response.setCategory(ticket.getCategory());
        response.setPriority(ticket.getPriority());
        response.setSummary(ticket.getSummary());
        response.setCreatedAt(ticket.getCreatedAt());
        return response;
    }

    private String buildTitle(String commentText) {
        if (commentText == null || commentText.isBlank()) {
            return "Customer comment";
        }
        String trimmed = commentText.trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }

    private String buildSummary(String commentText) {
        if (commentText == null || commentText.isBlank()) {
            return "No summary available.";
        }
        return commentText.length() > 2000 ? commentText.substring(0, 2000) : commentText;
    }
}