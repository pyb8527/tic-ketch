package com.ticketch.userservice.adapter.in.web;

import com.ticketch.common.response.ApiResponse;
import com.ticketch.userservice.adapter.in.web.dto.LoginRequest;
import com.ticketch.userservice.adapter.in.web.dto.RegisterRequest;
import com.ticketch.userservice.adapter.in.web.dto.TokenResponse;
import com.ticketch.userservice.application.port.in.LoginUseCase;
import com.ticketch.userservice.application.port.in.LogoutUseCase;
import com.ticketch.userservice.application.port.in.RegisterUserUseCase;
import com.ticketch.userservice.application.port.in.ReissueTokenUseCase;
import com.ticketch.userservice.application.port.in.TokenPair;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final ReissueTokenUseCase reissueTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Long>> register(@RequestBody @Valid RegisterRequest request) {
        Long userId = registerUserUseCase.register(
                new RegisterUserUseCase.RegisterCommand(request.email(), request.password(), request.name())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(userId));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody @Valid LoginRequest request) {
        TokenPair pair = loginUseCase.login(new LoginUseCase.LoginCommand(request.email(), request.password()));
        return ResponseEntity.ok(ApiResponse.ok(new TokenResponse(pair.accessToken(), pair.refreshToken())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @RequestHeader("Refresh-Token") String refreshToken) {
        TokenPair pair = reissueTokenUseCase.reissue(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok(new TokenResponse(pair.accessToken(), pair.refreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("Refresh-Token") String refreshToken) {
        String accessToken = authHeader.replace("Bearer ", "");
        logoutUseCase.logout(accessToken, refreshToken);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
