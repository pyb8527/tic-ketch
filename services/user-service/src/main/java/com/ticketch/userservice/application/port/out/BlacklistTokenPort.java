package com.ticketch.userservice.application.port.out;

public interface BlacklistTokenPort {

    void blacklist(String jti, long ttlMs);

    boolean isBlacklisted(String jti);
}
