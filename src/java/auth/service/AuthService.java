package com.islandtrails.auth.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class AuthService {

    // Password: min 8 chars, 1 uppercase, 1 number, 1 special char
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$"
    );

    public boolean isPasswordValid(String password) {
        if (password == null) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}
