package com.example.fashionshop.modules.user.controller;

import com.example.fashionshop.common.response.ApiResponse;
import com.example.fashionshop.modules.user.dto.UpdateProfileRequest;
import com.example.fashionshop.modules.user.dto.UserResponse;
import com.example.fashionshop.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<UserResponse> getMyProfile() {
        return ApiResponse.success("Profile fetched successfully", userService.getMyProfile());
    }

    @PutMapping
    public ApiResponse<UserResponse> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("Profile updated successfully", userService.updateMyProfile(request));
    }
}
