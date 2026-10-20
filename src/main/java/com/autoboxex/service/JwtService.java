package com.autoboxex.service;

import com.autoboxex.config.JwtConfig;
import com.autoboxex.config.JwtProperties;
import com.autoboxex.model.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = Clock.systemUTC();
    }

    public IssuedToken issue(Usuario usuario) {
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(properties.expirationMinutes(), ChronoUnit.MINUTES);

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(usuario.getUsername())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("uid", usuario.getId())
                .claim("name", usuario.getName());
        if (usuario.getRole() != null) {
            claims.claim(JwtConfig.ROLE_CLAIM, usuario.getRole().getRoleName());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
