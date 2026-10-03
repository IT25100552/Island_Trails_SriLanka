package com.islandtrails.auth.service;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.entity.UserRole;
import com.islandtrails.auth.entity.UserStatus;
import com.islandtrails.auth.repository.UserRepository;
import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.common.exception.ValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthService authService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.auditService = auditService;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User registerCustomer(String email, String name, String rawPassword, String phoneNumber) {
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException("Email address is already registered.");
        }
        if (!authService.isPasswordValid(rawPassword)) {
            throw new ValidationException("Password must be at least 8 characters long and contain at least 1 uppercase letter, 1 number, and 1 special character.");
        }

        User user = new User(
                email.trim().toLowerCase(),
                name.trim(),
                passwordEncoder.encode(rawPassword),
                UserRole.CUSTOMER,
                UserStatus.ACTIVE // Or PENDING_EMAIL_VERIFICATION; activated for immediate usability
        );
        user.setPhoneNumber(phoneNumber);

        User saved = userRepository.save(user);
        auditService.logAction(saved.getId(), saved.getEmail(), "REGISTER", "Customer registered successfully", null);
        return saved;
    }

    @Transactional
    public User createStaffUser(String email, String name, String rawPassword, UserRole role, String phoneNumber, Long adminId, String adminEmail, String ipAddress) {
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException("Email address is already in use.");
        }
        if (!authService.isPasswordValid(rawPassword)) {
            throw new ValidationException("Password must be at least 8 characters long and contain at least 1 uppercase letter, 1 number, and 1 special character.");
        }
        if (role == UserRole.CUSTOMER) {
            throw new ValidationException("Staff accounts cannot be assigned the CUSTOMER role via staff creation.");
        }

        User user = new User(
                email.trim().toLowerCase(),
                name.trim(),
                passwordEncoder.encode(rawPassword),
                role,
                UserStatus.ACTIVE
        );
        user.setPhoneNumber(phoneNumber);

        User saved = userRepository.save(user);
        auditService.logAction(
                adminId,
                adminEmail,
                "STAFF_CREATED",
                String.format("Created staff user '%s' with role %s", saved.getEmail(), role.name()),
                ipAddress
        );
        return saved;
    }

    @Transactional
    public User updateStaffRole(Long targetUserId, UserRole newRole, Long adminId, String adminEmail, String ipAddress) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));

        UserRole oldRole = user.getRole();
        user.setRole(newRole);
        User saved = userRepository.save(user);

        auditService.logAction(
                adminId,
                adminEmail,
                "ROLE_CHANGE",
                String.format("Changed role of user '%s' (ID: %d) from %s to %s", user.getEmail(), user.getId(), oldRole.name(), newRole.name()),
                ipAddress
        );

        return saved;
    }

    @Transactional
    public User deactivateUser(Long targetUserId, Long adminId, String adminEmail, String ipAddress) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));

        user.setStatus(UserStatus.DEACTIVATED);
        user.setDeactivatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        auditService.logAction(
                adminId,
                adminEmail,
                "ACCOUNT_DEACTIVATED",
                String.format("Account '%s' (ID: %d) deactivated (soft-delete)", user.getEmail(), user.getId()),
                ipAddress
        );

        return saved;
    }

    @Transactional
    public User reactivateUser(Long targetUserId, Long adminId, String adminEmail, String ipAddress) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));

        user.setStatus(UserStatus.ACTIVE);
        user.setDeactivatedAt(null);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        User saved = userRepository.save(user);

        auditService.logAction(
                adminId,
                adminEmail,
                "ACCOUNT_REACTIVATED",
                String.format("Account '%s' (ID: %d) reactivated", user.getEmail(), user.getId()),
                ipAddress
        );

        return saved;
    }

    @Transactional
    public void updateProfile(Long userId, String name, String phoneNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setName(name);
        user.setPhoneNumber(phoneNumber);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new ValidationException("Current password does not match.");
        }
        if (!authService.isPasswordValid(newPassword)) {
            throw new ValidationException("New password must be at least 8 characters long and contain at least 1 uppercase letter, 1 number, and 1 special character.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditService.logAction(user.getId(), user.getEmail(), "PASSWORD_RESET", "Password changed successfully", null);
    }

    @Transactional
    public void recordFailedLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
                auditService.logAction(
                        user.getId(),
                        user.getEmail(),
                        "ACCOUNT_LOCKED",
                        String.format("Account locked for %d minutes after %d failed attempts", LOCK_DURATION_MINUTES, attempts),
                        null
                );
            }
            userRepository.save(user);
        });
    }

    @Transactional
    public void recordSuccessfulLogin(String email, String ipAddress) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            auditService.logAction(user.getId(), user.getEmail(), "LOGIN", "Successful login", ipAddress);
        });
    }
}
