package com.streamcart.order.service;

import com.streamcart.order.dto.AuthResponse;
import com.streamcart.order.dto.LoginRequest;
import com.streamcart.order.dto.RegisterRequest;
import com.streamcart.order.entity.User;
import com.streamcart.order.exception.DuplicateEmailException;
import com.streamcart.order.exception.DuplicateUsernameException;
import com.streamcart.order.exception.InvalidCredentialsException;
import com.streamcart.order.repository.UserRepository;
import com.streamcart.order.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.username());
        
        // Check if username already exists
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUsernameException(request.username());
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        
        // Create new user with encrypted password
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .build();
        
        userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUsername());
        
        return new AuthResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                "User registered successfully"
        );
    }
    
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.username());
        
        // Find user by username
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(InvalidCredentialsException::new);
        
        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        
        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUsername());
        
        log.info("User logged in successfully: {}", user.getUsername());
        
        return new AuthResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                "Login successful"
        );
    }
}

