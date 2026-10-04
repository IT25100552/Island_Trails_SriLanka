package com.islandtrails.support.repository;

import com.islandtrails.support.entity.Ticket;
import com.islandtrails.support.entity.TicketDepartment;
import com.islandtrails.support.entity.TicketPriority;
import com.islandtrails.support.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Ticket> findByDepartmentOrderByCreatedAtDesc(TicketDepartment department);
    List<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status);
    List<Ticket> findByAssignedToOrderByCreatedAtDesc(Long assignedTo);
    List<Ticket> findAllByOrderByCreatedAtDesc();
    long countByStatus(TicketStatus status);
    long countByDepartment(TicketDepartment department);
}
