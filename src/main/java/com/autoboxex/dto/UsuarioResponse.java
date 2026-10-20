package com.autoboxex.dto;

public record UsuarioResponse(
        Long id,
        String name,
        String username,
        Long roleId,
        Long status) {
}