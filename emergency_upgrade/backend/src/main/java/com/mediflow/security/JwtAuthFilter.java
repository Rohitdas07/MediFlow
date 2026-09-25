package com.mediflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthFilter extends org.springframework.web.filter.OncePerRequestFilter {

    private final JwtService jwtService;
    private final PatientUserDetailsService patientUserDetailsService;
    private final StaffUserDetailsService staffUserDetailsService;
    private final AdminUserDetailsService adminUserDetailsService;

    public JwtAuthFilter(JwtService jwtService,
                         PatientUserDetailsService patientUserDetailsService,
                         StaffUserDetailsService staffUserDetailsService,
                         AdminUserDetailsService adminUserDetailsService) {
        this.jwtService = jwtService;
        this.patientUserDetailsService = patientUserDetailsService;
        this.staffUserDetailsService = staffUserDetailsService;
        this.adminUserDetailsService = adminUserDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7).trim();
        try {
            final String username = jwtService.extractUsername(jwt);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = null;
                try {
                    userDetails = patientUserDetailsService.loadUserByUsername(username);
                } catch (Exception ignored) {
                    try {
                        userDetails = staffUserDetailsService.loadUserByUsername(username);
                    } catch (Exception ignoredAgain) {
                        try {
                            userDetails = adminUserDetailsService.loadUserByUsername(username);
                        } catch (Exception ignoredAdmin) { }
                    }
                }

                if (userDetails != null && jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ignored) {
            // Invalid/expired JWT is simply treated as unauthenticated.
        }
        filterChain.doFilter(request, response);
    }
}
