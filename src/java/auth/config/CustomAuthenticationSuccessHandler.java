package com.islandtrails.auth.config;

import com.islandtrails.auth.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;

    public CustomAuthenticationSuccessHandler(@org.springframework.context.annotation.Lazy UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String email = authentication.getName();
        String ipAddress = request.getRemoteAddr();

        userService.recordSuccessfulLogin(email, ipAddress);

        String redirectUrl = "/dashboard";
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            switch (role) {
                case "ROLE_IT_SYSTEMS_OFFICER" -> {
                    redirectUrl = "/admin/dashboard";
                }
                case "ROLE_TOUR_OPS_MANAGER" -> {
                    redirectUrl = "/staff/tour-ops/calendar";
                }
                case "ROLE_TRAVEL_CONSULTANT" -> {
                    redirectUrl = "/staff/consultant/trip-requests";
                }
                case "ROLE_FINANCE_OFFICER" -> {
                    redirectUrl = "/staff/finance/payments";
                }
                case "ROLE_CUSTOMER_RELATIONS_OFFICER" -> {
                    redirectUrl = "/staff/support/tickets";
                }
                case "ROLE_CUSTOMER" -> {
                    redirectUrl = "/customer/dashboard";
                }
            }
        }

        response.sendRedirect(redirectUrl);
    }
}
