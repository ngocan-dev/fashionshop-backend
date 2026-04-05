package com.example.fashionshop.modules.user.service;

import com.example.fashionshop.common.enums.Role;
import com.example.fashionshop.common.exception.BadRequestException;
import com.example.fashionshop.common.exception.CustomerAccountRetrievalException;
import com.example.fashionshop.common.exception.ResourceNotFoundException;
import com.example.fashionshop.common.mapper.UserMapper;
import com.example.fashionshop.common.util.SecurityUtil;
import com.example.fashionshop.modules.user.dto.CreateStaffRequest;
import com.example.fashionshop.modules.user.dto.CustomerAccountResponse;
import com.example.fashionshop.modules.user.dto.UpdateProfileRequest;
import com.example.fashionshop.modules.user.dto.UserResponse;
import com.example.fashionshop.modules.user.entity.User;
import com.example.fashionshop.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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


    private CustomerAccountResponse toCustomerAccountResponse(User user) {
        return CustomerAccountResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(Boolean.TRUE.equals(user.getIsActive()) ? "ACTIVE" : "INACTIVE")
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
}
