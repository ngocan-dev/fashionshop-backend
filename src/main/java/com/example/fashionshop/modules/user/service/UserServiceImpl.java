package com.example.fashionshop.modules.user.service;

import com.example.fashionshop.common.enums.Role;
import com.example.fashionshop.common.exception.AccountDeletionException;
import com.example.fashionshop.common.exception.BadRequestException;
import com.example.fashionshop.common.exception.CustomerAccountRetrievalException;
import com.example.fashionshop.common.exception.InvalidAccountDeletionException;
import com.example.fashionshop.common.exception.ProfileUpdateException;
import com.example.fashionshop.common.exception.ResourceNotFoundException;
import com.example.fashionshop.common.exception.StaffAccountLoadException;
import com.example.fashionshop.common.mapper.UserMapper;
import com.example.fashionshop.common.util.SecurityUtil;
import com.example.fashionshop.modules.user.dto.CreateStaffRequest;
import com.example.fashionshop.modules.user.dto.CustomerAccountResponse;
import com.example.fashionshop.modules.user.dto.StaffAccountResponse;
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
        String normalizedEmail = normalizeRequired(request.getEmail());

        if (userRepository.existsByEmailAndIdNot(normalizedEmail, user.getId())) {
            throw new BadRequestException("Email already exists");
        }

        user.setFullName(normalizeRequired(request.getFullName()));
        user.setEmail(normalizedEmail);
        user.setPhoneNumber(normalizeOptional(request.getPhoneNumber()));
        user.setAddress(normalizeOptional(request.getAddress()));
        user.setAvatarUrl(normalizeOptional(request.getAvatarUrl()));
        user.setBio(normalizeOptional(request.getBio()));

        try {
            return UserMapper.toResponse(userRepository.save(user));
        } catch (DataAccessException ex) {
            throw new ProfileUpdateException("Profile update failed", ex);
        }
    }

    @Override
    public UserResponse createStaff(CreateStaffRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }
        if (request.getRole() == Role.CUSTOMER) {
            throw new BadRequestException("Invalid role for staff account");
        }
        User currentUser = getCurrentUser();
        User staff = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
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
    public List<StaffAccountResponse> getAllStaffAccounts() {
        try {
            return userRepository.findByRoleOrderByIdDesc(Role.STAFF).stream()
                    .map(this::toStaffAccountResponse)
                    .toList();
        } catch (Exception ex) {
            throw new StaffAccountLoadException("Unable to load staff accounts", ex);
        }
    }

    @Override
    public List<UserResponse> getCustomerAccounts() {
        return userRepository.findByRole(Role.CUSTOMER).stream().map(UserMapper::toResponse).toList();
    }

    @Override
    public List<CustomerAccountResponse> getAllCustomerAccounts() {
        try {
            return userRepository.findByRoleOrderByIdDesc(Role.CUSTOMER).stream()
                    .map(this::toCustomerAccountResponse)
                    .toList();
        } catch (Exception ex) {
            throw new CustomerAccountRetrievalException("Unable to load customer accounts", ex);
        }
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

    private CustomerAccountResponse toCustomerAccountResponse(User user) {
        return CustomerAccountResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(Boolean.TRUE.equals(user.getIsActive()) ? "ACTIVE" : "INACTIVE")
                .build();
    }

    private StaffAccountResponse toStaffAccountResponse(User user) {
        return StaffAccountResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .status(Boolean.TRUE.equals(user.getIsActive()) ? "ACTIVE" : "INACTIVE")
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
