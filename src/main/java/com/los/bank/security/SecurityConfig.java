package com.los.bank.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.los.bank.login.service.JwtAuthenticationFilter;

/**
 * Security configuration permitting unauthenticated access to setup endpoints
 * and exposing standard BCrypt password encoder for secure user password
 * hashing. Global Security Configuration. - Stateless JWT authentication -
 * Public access to authentication, setup, and lookup endpoints - Role-Based &
 * Permission-Based Access Control via JwtAuthenticationFilter Security
 * configuration permitting unauthenticated access to setup endpoints and
 * exposing standard BCrypt password encoder for secure user password hashing.
 *
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/setup/**",
                                "/api/users/**",
                                "/api/rbac/**",
                                "/api/lookups/**",
                                "/api/sub-lookups/**",
                                "/api/leads/**",
                                "/api/setup/**",
                                "/api/bank-profile/**",
                                "/api/organizations/**",
                                "/bank/**",
                                "/api/bank/**",
                                "/bank-accounts/**",
                                "/api/bank-accounts/**",
                                "/approval-limits/**",
                                "/api/approval-limits/**",
                                "/api/loan-products/**",
                                "/api/gold-rates/**",
                                "/api/rules/**",
                                "/api/rule-engine/**",
                                "/api/retail-loan-applications/**",
                                "/api/customers/**",
                                "/api/interest-rate-masters/**",
                                "/error")
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
