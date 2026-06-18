package com.ticketch.userservice.application.port.out;

public interface EncodePasswordPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
