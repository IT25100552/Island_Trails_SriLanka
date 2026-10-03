package com.islandtrails.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_actions")
public class UserAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(nullable = false, length = 50)
    private String action; // LOGIN, LOGOUT, ROLE_CHANGE, ACCOUNT_DEACTIVATED, PASSWORD_RESET, SETTING_CHANGED

    @Column(columnDefinition = "TEXT")
    private String delta; // what changed

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    public UserAction() {
        this.timestamp = LocalDateTime.now();
    }

    public UserAction(Long userId, String userEmail, String action, String delta, String ipAddress) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.action = action;
        this.delta = delta;
        this.timestamp = LocalDateTime.now();
        this.ipAddress = ipAddress;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDelta() {
        return delta;
    }

    public void setDelta(String delta) {
        this.delta = delta;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
