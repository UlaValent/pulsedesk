package com.pulsedesk.pulsedesk.controller;

import com.pulsedesk.pulsedesk.dto.CommentRequest;
import com.pulsedesk.pulsedesk.model.Comment;
import com.pulsedesk.pulsedesk.model.Ticket;
import com.pulsedesk.pulsedesk.model.enums.Category;
import com.pulsedesk.pulsedesk.model.enums.Priority;
import com.pulsedesk.pulsedesk.repository.CommentRepository;
import com.pulsedesk.pulsedesk.repository.TicketRepository;
import com.pulsedesk.pulsedesk.service.TicketService;
import com.pulsedesk.pulsedesk.service.TriageService;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommentAndTicketControllerTest {

    @Test
    void createCommentLinksTicketIdWhenTriageReturnsYes() {
        CommentRepository commentRepository = mock(CommentRepository.class);
        TriageService triageService = mock(TriageService.class);
        TicketService ticketService = mock(TicketService.class);

        Comment savedComment = new Comment();
        savedComment.setId(1L);
        savedComment.setText("The app crashes when I open settings.");
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(triageService.shouldCreateTicket(any(String.class))).thenReturn(true);

        Ticket ticket = new Ticket();
        ticket.setId(44L);
        when(ticketService.createTicketForComment(savedComment)).thenReturn(ticket);

        CommentController controller = new CommentController(commentRepository, triageService, ticketService);
        CommentRequest request = new CommentRequest();
        request.setText("The app crashes when I open settings.");
        request.setSource("web");

        Comment response = controller.createComment(request).getBody();

        assertEquals(44L, response.getTicketId());
    }

    @Test
    void ticketControllerReturnsDtoAndThrowsForMissingTicket() {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        TicketService ticketService = mock(TicketService.class);

        Ticket ticket = new Ticket();
        ticket.setId(7L);
        ticket.setCommentId(3L);
        ticket.setTitle("Login failure");
        ticket.setCategory(Category.BUG);
        ticket.setPriority(Priority.HIGH);
        ticket.setSummary("Users cannot log in");
        ticket.setCreatedAt(LocalDateTime.of(2026, 4, 1, 19, 0));

        when(ticketRepository.findAll()).thenReturn(List.of(ticket));
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        com.pulsedesk.pulsedesk.dto.TicketResponse response = new com.pulsedesk.pulsedesk.dto.TicketResponse();
        response.setId(7L);
        response.setCommentId(3L);
        response.setTitle("Login failure");
        response.setCategory(Category.BUG);
        response.setPriority(Priority.HIGH);
        response.setSummary("Users cannot log in");
        response.setCreatedAt(ticket.getCreatedAt());
        when(ticketService.toResponse(ticket)).thenReturn(response);

        TicketController controller = new TicketController(ticketRepository, ticketService);

        assertEquals(7L, controller.getAllTickets().getBody().get(0).getId());
        assertEquals(3L, controller.getTicketById(7L).getBody().getCommentId());
        assertThrows(TicketNotFoundException.class, () -> controller.getTicketById(99L));
    }
}