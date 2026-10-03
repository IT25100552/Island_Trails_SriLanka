package com.islandtrails.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;

    public SecurityConfig(CustomAuthenticationSuccessHandler successHandler,
                          CustomAuthenticationFailureHandler failureHandler) {
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Static resources & public pages
                .requestMatchers(
                    "/",
                    "/auth/**",
                    "/catalog/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/error/**",
                    "/favicon.ico"
                ).permitAll()
                // Role-based access control
                .requestMatchers("/customer/**").hasRole("CUSTOMER")
                .requestMatchers("/staff/tour-ops/**").hasRole("TOUR_OPS_MANAGER")
                .requestMatchers("/staff/consultant/**").hasRole("TRAVEL_CONSULTANT")
                .requestMatchers("/staff/finance/**").hasRole("FINANCE_OFFICER")
                .requestMatchers("/staff/support/**").hasRole("CUSTOMER_RELATIONS_OFFICER")
                .requestMatchers("/admin/**").hasRole("IT_SYSTEMS_OFFICER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
                .invalidSessionUrl("/auth/login?session=expired")
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403")
            );

        return http.build();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
