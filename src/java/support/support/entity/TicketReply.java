package com.islandtrails.support.entity;

import com.islandtrails.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "ticket_replies")
public class TicketReply extends BaseEntity {

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "replied_by")
    private Long repliedBy; // null if customer, or user id

    @Column(name = "replied_by_name")
    private String repliedByName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_staff_reply", nullable = false)
    private Boolean isStaffReply = false;

    @Column(length = 255)
    private String attachments;

    public TicketReply() {
    }

    public TicketReply(Long ticketId, Long repliedBy, String repliedByName, String message, Boolean isStaffReply) {
        this.ticketId = ticketId;
        this.repliedBy = repliedBy;
        this.repliedByName = repliedByName;
        this.message = message;
        this.isStaffReply = isStaffReply != null ? isStaffReply : false;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getRepliedBy() {
        return repliedBy;
    }

    public void setRepliedBy(Long repliedBy) {
        this.repliedBy = repliedBy;
    }

    public String getRepliedByName() {
        return repliedByName;
    }

    public void setRepliedByName(String repliedByName) {
        this.repliedByName = repliedByName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getIsStaffReply() {
        return isStaffReply;
    }

    public void setIsStaffReply(Boolean isStaffReply) {
        this.isStaffReply = isStaffReply;
    }

    public String getAttachments() {
        return attachments;
    }

    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }
}
