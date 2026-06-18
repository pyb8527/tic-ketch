package com.ticketch.userservice.application.port.out;

import com.ticketch.userservice.domain.model.User;

public interface SaveUserPort {

    Long save(User user);
}
