package com.autoboxex.dto;

import com.autoboxex.model.Role;
import com.autoboxex.model.Usuario;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UserDto(String name, String username, Long status, RoleDto role) {

    public record RoleDto(@JsonProperty("role_name") String roleName, String description) {
    }

    public static UserDto from(Usuario usuario) {
        Role role = usuario.getRole();
        RoleDto roleDto = role == null ? null : new RoleDto(role.getRoleName(), role.getDescription());
        return new UserDto(usuario.getName(), usuario.getUsername(), usuario.getStatus(), roleDto);
    }
}
