package com.ticketch.userservice.application.port.in;

import com.ticketch.userservice.domain.model.User;

public interface GetMyProfileUseCase {

    User getMyProfile(Long userId);
}
