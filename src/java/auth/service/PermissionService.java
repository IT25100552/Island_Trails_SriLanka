package com.islandtrails.auth.service;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.entity.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PermissionService {

    private final UserService userService;

    public PermissionService(UserService userService) {
        this.userService = userService;
    }

    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return userService.findByEmail(auth.getName());
    }

    public boolean hasRole(UserRole role) {
        return getCurrentUser()
                .map(user -> user.getRole() == role)
                .orElse(false);
    }
}
