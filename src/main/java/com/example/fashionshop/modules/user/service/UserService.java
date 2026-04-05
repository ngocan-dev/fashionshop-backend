package com.example.fashionshop.modules.user.service;

import com.example.fashionshop.modules.user.dto.CreateStaffRequest;
import com.example.fashionshop.modules.user.dto.UpdateProfileRequest;
import com.example.fashionshop.modules.user.dto.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse getMyProfile();

    UserResponse updateMyProfile(UpdateProfileRequest request);

    UserResponse createStaff(CreateStaffRequest request);

    List<UserResponse> getStaffAccounts();

    List<UserResponse> getCustomerAccounts();

    void deactivateUser(Integer userId);

    void deleteAccountById(Long id, Boolean confirm);

    void deleteAccountByEmail(String email, Boolean confirm);
}
