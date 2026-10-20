package com.autoboxex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(
        UserDto user,
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_at") String expiresAt) {
}
