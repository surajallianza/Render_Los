package com.los.bank.login.service;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7).trim();
            Claims claims = jwtTokenProvider.parseAndValidateToken(jwt);

            if (claims != null) {
                String jti = claims.getId();
                if (jwtTokenProvider.isSessionActive(jti)) {
                    String userId = claims.getSubject();
                    String role = claims.get("role", String.class);
                    @SuppressWarnings("unchecked")
                    List<String> permissions = claims.get("permissions", List.class);

                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    if (role != null && !role.trim().isEmpty()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase().replace(" ", "_")));
                    }
                    if (permissions != null) {
                        for (String perm : permissions) {
                            authorities.add(new SimpleGrantedAuthority(perm));
                        }
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("[JwtAuthenticationFilter] Authenticated userId: {} with role: {}", userId, role);
                } else {
                    logger.warn("[JwtAuthenticationFilter] Token JTI session {} is invalidated", jti);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
