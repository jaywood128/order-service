package com.streamcart.order.security;

import com.streamcart.order.entity.User;
import com.streamcart.order.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Custom UserDetailsService Implementation
 * 
 * Spring Security uses UserDetailsService to load user information during authentication.
 * 
 * UserDetails is Spring Security's representation of a user with:
 * - username
 * - password (encrypted)
 * - authorities (roles/permissions like "ROLE_USER", "ROLE_ADMIN")
 * - account status (enabled, locked, expired, etc.)
 * 
 * This service bridges YOUR User entity with Spring Security's UserDetails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by username - called by Spring Security during authentication
     * 
     * @param username the username to look up
     * @return UserDetails object that Spring Security understands
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        
        // Find user in database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        log.debug("User found: {}", user.getUsername());
        
        // Convert YOUR User entity to Spring Security's UserDetails
        // We're using Spring's built-in User class (org.springframework.security.core.userdetails.User)
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())  // Already BCrypt encrypted
                .authorities(Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_USER")  // Give all users the "USER" role
                ))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}

