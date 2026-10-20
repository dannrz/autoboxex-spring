package com.autoboxex.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageResponse(String message, String mismatch, Map<String, List<String>> errors) {

    public static MessageResponse of(String message) {
        return new MessageResponse(message, null, null);
    }
}
