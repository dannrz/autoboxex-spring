package com.autoboxex.controller;

import com.autoboxex.dto.MessageResponse;
import com.autoboxex.dto.LoginRequest;
import com.autoboxex.dto.LoginResponse;
import com.autoboxex.dto.UsuarioResponse;
import com.autoboxex.service.AuthService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(
            AuthService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> users() {
        return new ResponseEntity<>(service.users(), HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.login(request));
    }

    // JWT is stateless: the client discards the token; it stays valid until it expires.
    @GetMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        return ResponseEntity.ok(MessageResponse.of("Sesion finalizada"));
    }
}
