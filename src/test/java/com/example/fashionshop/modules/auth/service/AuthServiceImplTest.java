package com.example.fashionshop.modules.auth.service;

import com.example.fashionshop.common.enums.Role;
import com.example.fashionshop.common.exception.AccountCreationException;
import com.example.fashionshop.modules.auth.dto.AuthResponse;
import com.example.fashionshop.modules.auth.dto.RegisterRequest;
import com.example.fashionshop.modules.user.repository.UserRepository;
import com.example.fashionshop.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerShouldDefaultFullNameFromEmailWhenFullNameMissing() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new.customer@example.com");
        request.setPassword("password123");
        request.setVerifiedPassword("password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(com.example.fashionshop.modules.user.entity.User.class))).thenAnswer(invocation -> {
            com.example.fashionshop.modules.user.entity.User user = invocation.getArgument(0);
            user.setId(99);
            return user;
        });
        UserDetails userDetails = User.withUsername(request.getEmail()).password("encoded-password").authorities("CUSTOMER").build();
        when(userDetailsService.loadUserByUsername(request.getEmail())).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertEquals("new.customer", response.getFullName());
        assertEquals("jwt-token", response.getToken());
        assertEquals(Role.CUSTOMER, response.getRole());
    }

    @Test
    void registerShouldThrowAccountCreationExceptionWhenSaveFails() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane");
        request.setEmail("jane@example.com");
        request.setPassword("password123");
        request.setVerifiedPassword("password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(com.example.fashionshop.modules.user.entity.User.class)))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(AccountCreationException.class, () -> authService.register(request));
    }
}
