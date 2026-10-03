package com.islandtrails.auth.service;

import com.islandtrails.auth.entity.UserAction;
import com.islandtrails.auth.repository.UserActionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final UserActionRepository userActionRepository;

    public AuditService(UserActionRepository userActionRepository) {
        this.userActionRepository = userActionRepository;
    }

    @Transactional
    public void logAction(Long userId, String userEmail, String action, String delta, String ipAddress) {
        UserAction userAction = new UserAction(userId, userEmail, action, delta, ipAddress);
        userActionRepository.save(userAction);
    }

    public List<UserAction> getAllAuditLogs() {
        return userActionRepository.findAllByOrderByTimestampDesc();
    }

    public List<UserAction> getUserAuditLogs(Long userId) {
        return userActionRepository.findByUserIdOrderByTimestampDesc(userId);
    }
}
