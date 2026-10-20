package com.autoboxex.service;

import com.autoboxex.config.JwtProperties;
import com.autoboxex.dto.LoginRequest;
import com.autoboxex.dto.LoginResponse;
import com.autoboxex.dto.UserDto;
import com.autoboxex.dto.UsuarioResponse;
import com.autoboxex.exception.AuthException;
import com.autoboxex.model.Usuario;
import com.autoboxex.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AuthService {
    private static final long INACTIVE_STATUS = 0L;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ZoneId zone;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.zone = ZoneId.of(jwtProperties.timezone());
    }

    public List<UsuarioResponse> users() {
        return userRepository.findAllProjectedBy();
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new AuthException("username", "Usuario no encontrado"));

        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new AuthException("password", "Contraseña incorrecta");
        }

        if (usuario.getStatus() == null || usuario.getStatus() == INACTIVE_STATUS) {
            throw new AuthException("Usuario inactivo, contacte al administrador");
        }

        JwtService.IssuedToken token = jwtService.issue(usuario);

        return new LoginResponse(
                UserDto.from(usuario),
                token.value(),
                "Bearer",
                token.expiresAt().atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
}
