package com.islandtrails.auth.config;

import com.islandtrails.auth.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final UserService userService;

    public CustomAuthenticationFailureHandler(@org.springframework.context.annotation.Lazy UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("username");
        String errorMessage;

        if (exception instanceof DisabledException) {
            errorMessage = "This account has been deactivated. Please contact an IT Systems Officer.";
        } else if (exception instanceof LockedException) {
            errorMessage = "Account locked for 30 minutes due to 5 consecutive failed login attempts.";
        } else {
            if (email != null && !email.isBlank()) {
                userService.recordFailedLogin(email);
            }
            errorMessage = "Invalid email or password.";
        }

        response.sendRedirect("/auth/login?error=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8));
    }
}
