package com.ticketch.userservice.application.port.in;

public interface ReissueTokenUseCase {

    TokenPair reissue(String refreshToken);
}
