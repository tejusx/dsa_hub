package dsa_hub_backend.service;

import dsa_hub_backend.dto.TokenResponse;
import dsa_hub_backend.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Duration tokenLifetime;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${app.jwt.expiration:PT1H}") Duration tokenLifetime) {
        this.jwtEncoder = jwtEncoder;
        this.tokenLifetime = tokenLifetime;
    }

    public TokenResponse createToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(tokenLifetime);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getUserId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("email", user.getEmail())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenResponse(token, "Bearer", expiresAt);
    }
}
