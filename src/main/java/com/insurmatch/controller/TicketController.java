package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.entity.Ticket;
import com.insurmatch.entity.TicketComment;
import com.insurmatch.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TicketController — Khớp với FE:
 *   GET    /api/tickets?pipeline=&status=&priority=&contactId=&dealId=
 *   GET    /api/tickets/:id
 *   POST   /api/tickets
 *   PUT    /api/tickets/:id
 *   POST   /api/tickets/:id/comments
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Ticket>>> getTickets(
            @RequestParam(required = false) String pipeline,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long contactId,
            @RequestParam(required = false) Long dealId) {
        List<Ticket> tickets = ticketService.getAllTickets(pipeline, status, priority, contactId, dealId);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Ticket>> getTicket(@PathVariable Long id) {
        Ticket ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Ticket>> createTicket(@RequestBody Ticket ticket) {
        Ticket created = ticketService.createTicket(ticket);
        return ResponseEntity.ok(ApiResponse.success("Ticket created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Ticket>> updateTicket(@PathVariable Long id, @RequestBody Ticket ticketData) {
        Ticket updated = ticketService.updateTicket(id, ticketData);
        return ResponseEntity.ok(ApiResponse.success("Ticket updated", updated));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<TicketComment>> addComment(@PathVariable Long id, @RequestBody TicketComment comment) {
        TicketComment saved = ticketService.addComment(id, comment);
        return ResponseEntity.ok(ApiResponse.success("Comment added", saved));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<TicketComment>>> getComments(@PathVariable Long id) {
        List<TicketComment> comments = ticketService.getComments(id);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }
}
