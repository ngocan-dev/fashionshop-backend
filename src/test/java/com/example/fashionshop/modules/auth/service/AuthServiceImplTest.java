package com.example.fashionshop.modules.auth.service;

import com.example.fashionshop.common.enums.Role;
import com.example.fashionshop.common.exception.AuthenticationSystemException;
import com.example.fashionshop.common.exception.BadRequestException;
import com.example.fashionshop.modules.auth.dto.AuthResponse;
import com.example.fashionshop.modules.auth.dto.LoginRequest;
import com.example.fashionshop.modules.user.entity.User;
import com.example.fashionshop.modules.user.repository.UserRepository;
import com.example.fashionshop.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void loginShouldReturnAuthResponseWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");

        User user = User.builder()
                .id(1L)
                .fullName("Test User")
                .email("user@test.com")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("Test User", response.getFullName());
        assertEquals("user@test.com", response.getEmail());
        assertEquals(Role.CUSTOMER, response.getRole());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void loginShouldThrowBadRequestWhenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@test.com");
        request.setPassword("wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.login(request));

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void loginShouldThrowAuthenticationSystemExceptionWhenUnexpectedErrorOccurs() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        AuthenticationSystemException exception = assertThrows(AuthenticationSystemException.class,
                () -> authService.login(request));

        assertEquals("Login failed, please try again later", exception.getMessage());
    }
}
