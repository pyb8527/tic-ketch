package com.ticketch.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long accessExpiry;   // ms (기본 15분)
    private long refreshExpiry;  // ms (기본 7일)
}
