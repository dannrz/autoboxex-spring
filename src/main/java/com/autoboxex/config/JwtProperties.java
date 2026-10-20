package com.autoboxex.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(String secret, String issuer, long expirationMinutes, String timezone) {
}
