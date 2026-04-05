package com.example.fashionshop.modules.user.service;

import com.example.fashionshop.common.enums.Role;
import com.example.fashionshop.common.exception.AccountDeletionException;
import com.example.fashionshop.common.exception.BadRequestException;
import com.example.fashionshop.common.exception.InvalidAccountDeletionException;
import com.example.fashionshop.common.exception.ResourceNotFoundException;
import com.example.fashionshop.common.mapper.UserMapper;
import com.example.fashionshop.common.util.SecurityUtil;
import com.example.fashionshop.modules.user.dto.CreateStaffRequest;
import com.example.fashionshop.modules.user.dto.UpdateProfileRequest;
import com.example.fashionshop.modules.user.dto.UserResponse;
import com.example.fashionshop.modules.user.entity.User;
import com.example.fashionshop.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse getMyProfile() {
        User user = getCurrentUser();
        return UserMapper.toResponse(user);
    }

    @Override
    public UserResponse updateMyProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();
        user.setFullName(request.getFullName());
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse createStaff(CreateStaffRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }
        User currentUser = getCurrentUser();
        User staff = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.STAFF)
                .isActive(true)
                .managedBy(currentUser)
                .build();
        return UserMapper.toResponse(userRepository.save(staff));
    }

    @Override
    public List<UserResponse> getStaffAccounts() {
        return userRepository.findByRole(Role.STAFF).stream().map(UserMapper::toResponse).toList();
    }

    @Override
    public List<UserResponse> getCustomerAccounts() {
        return userRepository.findByRole(Role.CUSTOMER).stream().map(UserMapper::toResponse).toList();
    }

    @Override
    public void deactivateUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot deactivate admin account");
        }
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteAccountById(Long id, Boolean confirm) {
        requireConfirmation(confirm);
        Integer userId;
        try {
            userId = Math.toIntExact(id);
        } catch (ArithmeticException ex) {
            throw new ResourceNotFoundException("Account not found");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        deleteAccount(user);
    }

    @Override
    @Transactional
    public void deleteAccountByEmail(String email, Boolean confirm) {
        requireConfirmation(confirm);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        deleteAccount(user);
    }

    private void deleteAccount(User user) {
        validateDeletableAccount(user);

        try {
            // Project currently uses soft delete for user account lifecycle.
            user.setIsActive(false);
            userRepository.save(user);
        } catch (DataAccessException ex) {
            throw new AccountDeletionException("Account deletion failed");
        }
    }

    private void validateDeletableAccount(User user) {
        if (user.getRole() == Role.ADMIN) {
            throw new InvalidAccountDeletionException("Admin account cannot be deleted");
        }

        if (user.getRole() != Role.STAFF && user.getRole() != Role.CUSTOMER) {
            throw new InvalidAccountDeletionException("Only staff or customer accounts can be deleted");
        }
    }

    private void requireConfirmation(Boolean confirm) {
        if (!Boolean.TRUE.equals(confirm)) {
            throw new BadRequestException("Deletion confirmation is required");
        }
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
}
