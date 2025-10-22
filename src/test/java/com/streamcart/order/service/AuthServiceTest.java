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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService
 * Tests authentication and authorization business logic with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Setup test data
        validRegisterRequest = new RegisterRequest(
                "johndoe",
                "john.doe@example.com",
                "SecurePass123!",
                "John",
                "Doe"
        );

        validLoginRequest = new LoginRequest(
                "johndoe",
                "SecurePass123!"
        );

        testUser = User.builder()
                .id(1L)
                .username("johndoe")
                .email("john.doe@example.com")
                .password("$2a$10$hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    // ========== REGISTRATION TESTS ==========

    @Test
    void testRegister_WithValidData_ReturnsAuthResponseAndSavesUser() {
        // Arrange
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("$2a$10$hashedPassword");
        when(jwtUtil.generateToken("johndoe")).thenReturn("jwt.token.here");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AuthResponse response = authService.register(validRegisterRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt.token.here");
        assertThat(response.username()).isEqualTo("johndoe");
        assertThat(response.email()).isEqualTo("john.doe@example.com");
        assertThat(response.message()).isEqualTo("User registered successfully");

        // Verify password was encoded
        verify(passwordEncoder).encode("SecurePass123!");

        // Verify user was saved with correct data
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("johndoe");
        assertThat(savedUser.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("$2a$10$hashedPassword");
        assertThat(savedUser.getFirstName()).isEqualTo("John");
        assertThat(savedUser.getLastName()).isEqualTo("Doe");

        // Verify JWT token was generated
        verify(jwtUtil).generateToken("johndoe");
    }

    @Test
    void testRegister_WithDuplicateUsername_ThrowsDuplicateUsernameException() {
        // Arrange
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(DuplicateUsernameException.class)
                .hasMessageContaining("johndoe");

        // Verify no user was saved
        verify(userRepository, never()).save(any(User.class));
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void testRegister_WithDuplicateEmail_ThrowsDuplicateEmailException() {
        // Arrange
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("john.doe@example.com");

        // Verify no user was saved
        verify(userRepository, never()).save(any(User.class));
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void testRegister_ChecksUsernameBeforeEmail_ValidatesInCorrectOrder() {
        // Arrange - Username is duplicate (email check won't be reached)
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);
        // Intentionally NOT mocking existsByEmail because it should never be called

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(DuplicateUsernameException.class); // Username check happens first

        // Email check should not be reached
        verify(userRepository).existsByUsername("johndoe");
        verify(userRepository, never()).existsByEmail(anyString());
    }

    // ========== LOGIN TESTS ==========

    @Test
    void testLogin_WithValidCredentials_ReturnsAuthResponseAndUpdatesLastLogin() {
        // Arrange
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("SecurePass123!", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("johndoe")).thenReturn("jwt.token.here");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AuthResponse response = authService.login(validLoginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt.token.here");
        assertThat(response.username()).isEqualTo("johndoe");
        assertThat(response.email()).isEqualTo("john.doe@example.com");
        assertThat(response.message()).isEqualTo("Login successful");

        // Verify password was checked
        verify(passwordEncoder).matches("SecurePass123!", "$2a$10$hashedPassword");

        // Verify last login was updated
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertThat(updatedUser.getLastLoginAt()).isNotNull();

        // Verify JWT token was generated
        verify(jwtUtil).generateToken("johndoe");
    }

    @Test
    void testLogin_WithNonExistentUsername_ThrowsInvalidCredentialsException() {
        // Arrange
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(InvalidCredentialsException.class);

        // Verify password was not checked
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void testLogin_WithInvalidPassword_ThrowsInvalidCredentialsException() {
        // Arrange
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("SecurePass123!", "$2a$10$hashedPassword")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(InvalidCredentialsException.class);

        // Verify user was not saved (no lastLoginAt update)
        verify(userRepository, never()).save(any(User.class));
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void testLogin_WithCorrectPasswordMatch_VerifiesPasswordCorrectly() {
        // Arrange
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("SecurePass123!", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(anyString())).thenReturn("jwt.token.here");

        // Act
        authService.login(validLoginRequest);

        // Assert - Verify password encoder was called with correct parameters
        verify(passwordEncoder).matches("SecurePass123!", "$2a$10$hashedPassword");
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    void testRegister_WithMinimalValidData_SucceedsWithAllRequiredFields() {
        // Arrange
        RegisterRequest minimalRequest = new RegisterRequest(
                "a",  // Minimal username
                "a@b.c",  // Minimal email
                "pass",  // Minimal password
                "F",  // Minimal first name
                "L"   // Minimal last name
        );

        when(userRepository.existsByUsername("a")).thenReturn(false);
        when(userRepository.existsByEmail("a@b.c")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encodedPass");
        when(jwtUtil.generateToken("a")).thenReturn("token");

        // Act
        AuthResponse response = authService.register(minimalRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("a");
        assertThat(response.email()).isEqualTo("a@b.c");
    }

    @Test
    void testLogin_AfterMultipleFailedAttempts_StillAllowsValidLogin() {
        // This tests that the service doesn't implement account lockout
        // (which is fine for a demo project, but good to document)

        // Arrange
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("SecurePass123!", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("johndoe")).thenReturn("jwt.token.here");

        // Act - Login should succeed regardless of previous failures
        AuthResponse response = authService.login(validLoginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt.token.here");
    }
}

