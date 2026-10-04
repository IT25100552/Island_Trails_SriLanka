package com.islandtrails.support.service;

import com.islandtrails.support.entity.*;
import org.springframework.stereotype.Service;

@Service
public class TicketRoutingService {

    /**
     * Hard-coded Auto-Routing rules:
     * PAYMENT -> FINANCE
     * GUIDE_QUALITY -> TOUR_OPS
     * ACCOMMODATION -> TOUR_CONSULTANT
     * else -> GENERAL
     *
     * Priority rules:
     * COMPLAINT -> HIGH
     * REVIEW -> LOW
     * QUESTION -> MEDIUM
     */
    public void routeTicket(Ticket ticket) {
        if (ticket == null) {
            return;
        }

        // Auto-routing department assignment
        TicketDepartment department;
        if (ticket.getCategory() != null) {
            switch (ticket.getCategory()) {
                case PAYMENT -> department = TicketDepartment.FINANCE;
                case GUIDE_QUALITY -> department = TicketDepartment.TOUR_OPS;
                case ACCOMMODATION -> department = TicketDepartment.TOUR_CONSULTANT;
                default -> department = TicketDepartment.GENERAL;
            }
        } else {
            department = TicketDepartment.GENERAL;
        }
        ticket.setDepartment(department);

        // Priority assignment
        TicketPriority priority;
        if (ticket.getInquiryType() != null) {
            switch (ticket.getInquiryType()) {
                case COMPLAINT -> priority = TicketPriority.HIGH;
                case REVIEW -> priority = TicketPriority.LOW;
                case QUESTION -> priority = TicketPriority.MEDIUM;
                default -> priority = TicketPriority.MEDIUM;
            }
        } else {
            priority = TicketPriority.MEDIUM;
        }
        ticket.setPriority(priority);
    }
}
