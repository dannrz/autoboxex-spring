package com.autoboxex.controller;

import com.autoboxex.config.JsonSecurityErrorHandler;
import com.autoboxex.config.JwtConfig;
import com.autoboxex.config.SecurityConfig;
import com.autoboxex.model.Role;
import com.autoboxex.model.Usuario;
import com.autoboxex.repository.UserRepository;
import com.autoboxex.service.AuthService;
import com.autoboxex.service.JwtService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtConfig.class, JsonSecurityErrorHandler.class, AuthService.class, JwtService.class})
@TestPropertySource(properties = "auth.jwt.secret=test-secret-key-with-at-least-32-bytes!!")
class AuthControllerTest {
    private static final String PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private UserRepository userRepository;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setId(1L);
        role.setRoleName("admin");
        role.setDescription("Administrador");

        usuario = new Usuario();
        usuario.setId(10L);
        usuario.setName("Daniel");
        usuario.setUsername("daniel");
        // Laravel stores bcrypt hashes with the $2y$ prefix
        usuario.setPassword(new BCryptPasswordEncoder().encode(PASSWORD).replaceFirst("^\\$2a\\$", "\\$2y\\$"));
        usuario.setRoleId(1L);
        usuario.setRole(role);
        usuario.setStatus(1L);

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername("daniel")).thenReturn(Optional.of(usuario));
    }

    private String loginBody(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void loginReturnsJwtThatGrantsAccessToProtectedEndpoints() throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("daniel", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.expires_at").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("daniel"))
                .andExpect(jsonPath("$.user.role.role_name").value("admin"))
                .andExpect(jsonPath("$.user.role.description").value("Administrador"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.id").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(response, "$.access_token");

        mockMvc.perform(get("/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sesion finalizada"));
    }

    @Test
    void loginWithUnknownUsernameReturnsUsernameMismatch() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nobody", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mismatch").value("username"))
                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
    }

    @Test
    void loginWithWrongPasswordReturnsPasswordMismatch() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("daniel", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mismatch").value("password"))
                .andExpect(jsonPath("$.message").value("Contraseña incorrecta"));
    }

    @Test
    void loginWithInactiveUserIsRejected() throws Exception {
        usuario.setStatus(0L);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("daniel", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mismatch").doesNotExist())
                .andExpect(jsonPath("$.message").value("Usuario inactivo, contacte al administrador"));
    }

    @Test
    void loginWithBlankFieldsReturnsValidationErrors() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.username").isArray())
                .andExpect(jsonPath("$.errors.password").isArray());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Sesión no válida o expirada"));
    }

    @Test
    void protectedEndpointWithTamperedTokenReturns401() throws Exception {
        String token = issueToken(Instant.now().plus(1, ChronoUnit.HOURS)) + "x";

        mockMvc.perform(get("/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithExpiredTokenReturns401() throws Exception {
        String token = issueToken(Instant.now().minus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String issueToken(Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("autoboxex-spring")
                .subject("daniel")
                .issuedAt(expiresAt.minus(2, ChronoUnit.HOURS))
                .expiresAt(expiresAt)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
