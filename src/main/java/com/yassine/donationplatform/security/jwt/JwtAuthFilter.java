// src/main/java/com/yassine/donationplatform/security/jwt/JwtAuthFilter.java
package com.yassine.donationplatform.security.jwt;

import com.yassine.donationplatform.security.admin.AdminProps;
import com.yassine.donationplatform.security.auth.AuthUser;
import com.yassine.donationplatform.user.User;
import com.yassine.donationplatform.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AdminProps adminProps;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository, AdminProps adminProps) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.adminProps = adminProps;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // On protège seulement l'admin
        if (!path.startsWith("/api/admin/")) return true;

        // On laisse passer les endpoints publics
        return path.equals("/api/admin/auth/login")
                || path.equals("/api/admin/auth/refresh")
                || path.equals("/api/admin/auth/logout");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = header.substring(7);

        final UUID userId;
        try {
            userId = jwtService.parseUserId(token);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // ✅ admin unique : refuse si ce n’est pas l’email admin
        if (!user.getEmail().equalsIgnoreCase(adminProps.email())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            AuthUser principal = new AuthUser(user.getId(), user.getEmail());
            var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }
}
