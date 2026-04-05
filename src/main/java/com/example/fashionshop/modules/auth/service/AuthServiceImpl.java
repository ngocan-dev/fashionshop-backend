package com.example.fashionshop.modules.auth.service;

import com.example.fashionshop.common.enums.Role;
import com.example.fashionshop.common.exception.AuthenticationSystemException;
import com.example.fashionshop.common.exception.BadRequestException;
import com.example.fashionshop.modules.auth.dto.AuthResponse;
import com.example.fashionshop.modules.auth.dto.LoginRequest;
import com.example.fashionshop.modules.auth.dto.RegisterRequest;
import com.example.fashionshop.modules.user.entity.User;
import com.example.fashionshop.modules.user.repository.UserRepository;
import com.example.fashionshop.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());

        return AuthResponse.builder()
                .token(jwtService.generateToken(userDetails))
                .userId(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadRequestException("Invalid email or password"));

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            return AuthResponse.builder()
                    .token(jwtService.generateToken(userDetails))
                    .userId(user.getId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .build();
        } catch (AuthenticationException ex) {
            throw new BadRequestException("Invalid email or password");
        } catch (Exception ex) {
            throw new AuthenticationSystemException("Login failed, please try again later");
        }
    }
}
