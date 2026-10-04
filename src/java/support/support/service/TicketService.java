package com.islandtrails.support.service;

import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.support.entity.*;
import com.islandtrails.support.repository.TicketReplyRepository;
import com.islandtrails.support.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketReplyRepository ticketReplyRepository;
    private final TicketRoutingService ticketRoutingService;

    public TicketService(TicketRepository ticketRepository,
                         TicketReplyRepository ticketReplyRepository,
                         TicketRoutingService ticketRoutingService) {
        this.ticketRepository = ticketRepository;
        this.ticketReplyRepository = ticketReplyRepository;
        this.ticketRoutingService = ticketRoutingService;
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Ticket> getTicketsByCustomer(Long customerId) {
        return ticketRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public List<Ticket> getTicketsByDepartment(TicketDepartment department) {
        return ticketRepository.findByDepartmentOrderByCreatedAtDesc(department);
    }

    public List<Ticket> getTicketsByStatus(TicketStatus status) {
        return ticketRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
    }

    public List<TicketReply> getReplies(Long ticketId) {
        return ticketReplyRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    @Transactional
    public Ticket createTicket(Long customerId, String customerName, String customerEmail, String subject, String description, TicketCategory category, InquiryType inquiryType) {
        if (subject == null || subject.isBlank()) {
            throw new ValidationException("Ticket subject cannot be empty.");
        }
        if (description == null || description.isBlank()) {
            throw new ValidationException("Ticket description cannot be empty.");
        }
        if (category == null) {
            category = TicketCategory.GENERAL;
        }
        if (inquiryType == null) {
            inquiryType = InquiryType.QUESTION;
        }

        Ticket ticket = new Ticket(customerId, customerName, customerEmail, subject.trim(), description.trim(), category, inquiryType);

        // Apply auto-routing and priority
        ticketRoutingService.routeTicket(ticket);

        return ticketRepository.save(ticket);
    }

    @Transactional
    public TicketReply addReply(Long ticketId, Long repliedBy, String repliedByName, String message, Boolean isStaffReply, String attachments) {
        Ticket ticket = getTicketById(ticketId);

        if (message == null || message.isBlank()) {
            throw new ValidationException("Reply message cannot be empty.");
        }

        TicketReply reply = new TicketReply(ticketId, repliedBy, repliedByName, message.trim(), isStaffReply);
        reply.setAttachments(attachments);
        TicketReply savedReply = ticketReplyRepository.save(reply);

        if (Boolean.TRUE.equals(isStaffReply)) {
            if (ticket.getStatus() == TicketStatus.OPEN) {
                ticket.setStatus(TicketStatus.IN_PROGRESS);
                ticketRepository.save(ticket);
            }
        }

        return savedReply;
    }

    @Transactional
    public Ticket assignTicket(Long ticketId, Long staffUserId, String staffUserName) {
        Ticket ticket = getTicketById(ticketId);
        ticket.setAssignedTo(staffUserId);
        ticket.setAssignedToName(staffUserName);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket updateStatus(Long ticketId, TicketStatus newStatus) {
        Ticket ticket = getTicketById(ticketId);
        ticket.setStatus(newStatus);
        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else if (newStatus == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        }
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket updatePriority(Long ticketId, TicketPriority newPriority) {
        Ticket ticket = getTicketById(ticketId);
        ticket.setPriority(newPriority);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public void markAsSpam(Long ticketId) {
        Ticket ticket = getTicketById(ticketId);
        ticket.setStatus(TicketStatus.SPAM);
        ticketRepository.save(ticket);
    }
}
