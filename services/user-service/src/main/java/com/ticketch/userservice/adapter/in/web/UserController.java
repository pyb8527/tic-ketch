package com.ticketch.userservice.adapter.in.web;

import com.ticketch.common.response.ApiResponse;
import com.ticketch.security.UserPrincipal;
import com.ticketch.userservice.adapter.in.web.dto.UserResponse;
import com.ticketch.userservice.application.port.in.GetMyProfileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final GetMyProfileUseCase getMyProfileUseCase;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(UserResponse.from(getMyProfileUseCase.getMyProfile(principal.getUserId())))
        );
    }
}
