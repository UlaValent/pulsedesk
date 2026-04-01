package com.pulsedesk.pulsedesk.controller;

import com.pulsedesk.pulsedesk.dto.TicketResponse;
import com.pulsedesk.pulsedesk.model.Ticket;
import com.pulsedesk.pulsedesk.repository.TicketRepository;
import com.pulsedesk.pulsedesk.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketRepository ticketRepository;
    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getAllTickets() {
        List<TicketResponse> tickets = ticketRepository.findAll().stream()
                .map(ticketService::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long ticketId) {
        return ticketRepository.findById(ticketId)
                .map(ticketService::toResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }
}
