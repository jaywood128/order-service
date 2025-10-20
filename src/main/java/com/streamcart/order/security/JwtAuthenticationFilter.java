package com.streamcart.order.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter
 * 
 * This filter intercepts EVERY HTTP request and checks for a valid JWT token.
 * It extends OncePerRequestFilter to ensure it runs exactly once per request.
 * 
 * Flow:
 * 1. Extract JWT token from Authorization header
 * 2. Validate the token
 * 3. Load user details from database
 * 4. Set authentication in Spring Security context
 * 5. Pass request to next filter in chain
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Step 1: Extract the Authorization header
        final String authHeader = request.getHeader("Authorization");
        
        // If no Authorization header or doesn't start with "Bearer ", skip this filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No JWT token found in request headers");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Step 2: Extract the JWT token (remove "Bearer " prefix)
            final String jwt = authHeader.substring(7);
            final String username = jwtUtil.extractUsername(jwt);
            
            log.debug("JWT token found for user: {}", username);

            // Step 3: If we have a username and user is not already authenticated
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Step 4: Load user details from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // Step 5: Validate the token
                if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                    
                    // Step 6: Create authentication object
                    // This is what Spring Security uses to know "who is logged in"
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,  // credentials (we don't need password here since JWT is validated)
                            userDetails.getAuthorities()  // user roles/permissions
                    );
                    
                    // Set additional details (IP address, session ID, etc.)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Step 7: Set the authentication in the SecurityContext
                    // This tells Spring Security "this user is authenticated!"
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("User {} successfully authenticated via JWT", username);
                } else {
                    log.warn("Invalid JWT token for user: {}", username);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        // Step 8: Continue with the next filter in the chain
        // If authentication was set, the request will be allowed
        // If not, Spring Security will reject it with 401/403
        filterChain.doFilter(request, response);
    }
}

